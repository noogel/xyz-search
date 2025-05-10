package noogel.xyz.search.infrastructure.repo.impl.elastic;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FuzzyQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.HighlighterFragmenter;
import co.elastic.clients.elasticsearch.core.search.HighlighterOrder;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ForcemergeRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.client.ElasticClient;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.dto.LLMSearchResultDto;
import noogel.xyz.search.infrastructure.dto.ResourceHighlightHitsDto;
import noogel.xyz.search.infrastructure.dto.SearchResultDto;
import noogel.xyz.search.infrastructure.dto.repo.CommonSearchDto;
import noogel.xyz.search.infrastructure.dto.repo.CommonSearchDto.CompareEnum;
import noogel.xyz.search.infrastructure.dto.repo.CommonSearchDto.Field;
import noogel.xyz.search.infrastructure.dto.repo.LLMSearchDto;
import noogel.xyz.search.infrastructure.dto.repo.RandomSearchDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.model.FullTextSearchModel;
import noogel.xyz.search.infrastructure.repo.FullTextSearchRepo;

@Repository
@Slf4j
public class ElasticSearchRepoImpl implements FullTextSearchRepo {

    @Resource
    private ElasticClient elasticClient;
    @Resource
    private volatile ConfigProperties searchConfig;

    /**
     * 创建 mapping
     */
    public boolean createIndex(boolean deleteIfExist) {
        int maxRetries = 3;
        int retryInterval = 5000; // 5秒

        for (int i = 0; i < maxRetries; i++) {
            try {
                log.info("开始检查索引 {} 是否存在，第{}次尝试", getIndexName(), i + 1);
                boolean indexExist = elasticClient.getClient().indices()
                        .exists(b -> b.index(getIndexName())).value();

                if (indexExist && deleteIfExist) {
                    log.info("删除已存在的索引 {}", getIndexName());
                    DeleteIndexResponse delete = elasticClient.getClient().indices()
                            .delete(c -> c.index(getIndexName()));
                    log.info("索引删除结果: {}", delete.acknowledged());
                }

                if (!indexExist || deleteIfExist) {
                    log.info("创建新索引 {}, mapping配置: {}", getIndexName(), ElasticMapping.generate());
                    CreateIndexResponse response = elasticClient.getClient().indices()
                            .create(c -> c
                                    .index(getIndexName())
                                    .mappings(d -> d.properties(ElasticMapping.generate()))
                                    .settings(s -> s.analysis(k ->
                                            k.analyzer("path_tokenizer",
                                                    a -> a.custom(l -> l.tokenizer("path_hierarchy"))))));

                    log.info("索引创建结果: {}", response.acknowledged());

                    if (!response.acknowledged()) {
                        log.error("索引创建失败");
                        if (i < maxRetries - 1) {
                            Thread.sleep(retryInterval);
                            continue;
                        }
                        return false;
                    }
                    elasticClient.updateSettings();
                    searchConfig.getRuntime().setFtsInitIndex(true);
                    searchConfig.overrideToFile();
                }
                return true;
            } catch (Exception ex) {
                log.error("创建索引发生异常，第{}次尝试", i + 1, ex);
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 获取索引名称
     *
     * @return
     */
    private String getIndexName() {
        return searchConfig.getRuntime().getFtsIndexName();
    }

    @Override
    public FullTextSearchModel findByResId(String resId) {
        try {
            GetResponse<FullTextSearchModel> response = elasticClient.getClient().get(
                    t -> t.index(getIndexName()).id(resId),
                    FullTextSearchModel.class);
            return response.source();
        } catch (Exception ex) {
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(ex);
        }
    }

    @Override
    @Nullable
    public ResourceHighlightHitsDto searchByResId(String resId, @Nullable String text) {
        try {
            BoolQuery.Builder builder = new BoolQuery.Builder();
            if (!StringUtils.isEmpty(text)) {
                Query content = MatchQuery.of(m -> m.field("content")
                        .query(text).analyzer("ik_smart"))._toQuery();
                Query resTitle = MatchQuery.of(m -> m.field("resTitle")
                        .query(text).analyzer("ik_smart"))._toQuery();
                builder.should(content, resTitle);
            }
            Query redId = TermQuery.of(m -> m.field("resId").value(resId))._toQuery();
            builder.must(redId);

            // 改进的高亮配置
            Highlight highlight = Highlight.of(t -> t
                .fields("content", HighlightField.of(k -> k
                    .type("unified")  // 使用更高级的unified highlighter
                    .fragmentSize(150)  // 更合理的片段大小
                    .numberOfFragments(10)  // 增加片段数量
                    .preTags("<em>")  // 自定义标签
                    .postTags("</em>")
                    .fragmenter(HighlighterFragmenter.Span)
                    .order(HighlighterOrder.Score)  // 按相关性排序片段
                )));

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(getIndexName())
                    .query(q -> q.bool(t -> builder))
                    .highlight(highlight));
            log.info("search:{}", searchRequest.toString());
            SearchResponse<FullTextSearchModel> search = elasticClient.getClient().search(searchRequest,
                    FullTextSearchModel.class);

            List<Hit<FullTextSearchModel>> hits = search.hits().hits();

            for (Hit<FullTextSearchModel> hit : hits) {
                ResourceHighlightHitsDto dto = new ResourceHighlightHitsDto();
                dto.setResource(hit.source());
                dto.setHighlights(hit.highlight().get("content"));
                return dto;
            }
        } catch (IOException ex) {
            log.error("findByResId err", ex);
        }
        return null;
    }

    @Override
    public boolean delete(String resId, Runnable onSuccess) {
        try {
            DeleteResponse delete = elasticClient.getClient().delete(b -> b.index(getIndexName()).id(resId));
            boolean result = delete.version() > 0;
            if (result) {
                onSuccess.run();
            }
            return result;
        } catch (IOException ex) {
            log.error("deleteByResId err {}", resId, ex);
            return false;
        }
    }

    @Override
    public boolean upsert(FullTextSearchModel model, Runnable onSuccess) {
        try {
            // 如果没有初始化索引，则创建。
            if (!searchConfig.getRuntime().getFtsInitIndex()) {
                createIndex(false);
            }
            ConfigProperties.Elastic elastic = searchConfig.getApp().getChat().getElastic();
            Integer maxSize = Optional.ofNullable(elastic.getHighlightMaxAnalyzedOffset()).orElse(1000000);
            if (model.getContentSize() > maxSize) {
                log.warn("文件内容过大，跳过：resId: {}, contentSize:{} > highlightMaxAnalyzedOffset:{}",
                        model.getResId(), model.getContentSize(), maxSize);
                onSuccess.run();
                return true;
            }
            IndexResponse response = elasticClient.getClient().index(i -> i
                    .index(getIndexName())
                    .id(model.getResId())
                    .document(model));
            boolean result = response.version() > 0;
            if (result) {
                onSuccess.run();
            }
            return result;
        } catch (IOException ex) {
            log.error("upsertData err: {} {} {}", model.getContentSize(), model.getResDir(), model.getResName(), ex);
            return false;
        }
    }

    @Override
    public void reset() {
        createIndex(true);
    }

    @Override
    public SearchResultDto commonSearch(CommonSearchDto searchDto) {
        // 如果没有初始化索引，则创建。
        if (!searchConfig.getRuntime().getFtsInitIndex()) {
            createIndex(false);
        }
        // 执行搜索
        SearchResultDto resp = new SearchResultDto();
        resp.setData(new ArrayList<>());

        try {
            // 构建主查询
            BoolQuery.Builder mainQueryBuilder = new BoolQuery.Builder();

            if (!StringUtils.isEmpty(searchDto.getSearchQuery())) {
                // 1. 多字段跨字段查询 - 主要匹配逻辑
                Query multiFieldQuery = MultiMatchQuery.of(m -> m
                    .query(searchDto.getSearchQuery())
                    .type(TextQueryType.CrossFields)
                    .operator(Operator.And)
                    .fields("content^1", "resName^10", "resTitle^5")
                    .minimumShouldMatch("60%")
                    .tieBreaker(0.3))._toQuery();
                
                // 2. 短语匹配 - 提高精确度
                Query contentPhrase = MatchPhraseQuery.of(m -> m
                    .field("content")
                    .query(searchDto.getSearchQuery())
                    .slop(3)
                    .boost(searchDto.getSearchQuery().length() > 5 ? 5.0f : 2.0f))._toQuery();
                
                // 3. 标题短语匹配 - 高优先级
                Query titlePhrase = MatchPhraseQuery.of(m -> m
                    .field("resTitle")
                    .query(searchDto.getSearchQuery())
                    .slop(0)
                    .boost(6.0f))._toQuery();
                
                // 4. 添加模糊匹配 - 容错
                Query fuzzyQuery = FuzzyQuery.of(f -> f
                    .field("content")
                    .value(searchDto.getSearchQuery())
                    .fuzziness("AUTO")
                    .prefixLength(2)
                    .maxExpansions(50)
                    .boost(0.8f))._toQuery();
                
                // 组装BoolQuery
                BoolQuery.Builder complexQuery = new BoolQuery.Builder();
                
                // 必须满足的条件
                complexQuery.must(multiFieldQuery);
                
                // 加分项
                complexQuery.should(contentPhrase, titlePhrase, fuzzyQuery);
                
                mainQueryBuilder.must(complexQuery.build()._toQuery());
            }
            
            // 处理其他过滤条件，沿用现有逻辑
            if (CollectionUtils.isNotEmpty(searchDto.getResTypeList())) {
                BoolQuery.Builder orResType = new BoolQuery.Builder();
                for (String resType : searchDto.getResTypeList()) {
                    Query resTypeQuery = TermQuery.of(m -> m
                            .field("resType")
                            .value(resType))._toQuery();
                    orResType.should(resTypeQuery);
                }
                mainQueryBuilder.must(orResType.build()._toQuery());
            }
            if (StringUtils.isNotEmpty(searchDto.getDirPrefix())) {
                Query resDir = TermQuery.of(m -> m
                        .field("resDir")
                        .value(searchDto.getDirPrefix()))._toQuery();
                mainQueryBuilder.must(resDir);
            }
            Field resSizeField = searchDto.getResSize();
            if (resSizeField != null) {
                Query resSize = this.buildRangeQuery("resSize", resSizeField.getValue(), resSizeField.getCompare());
                mainQueryBuilder.must(resSize);
            }
            Field modifiedAtField = searchDto.getModifiedAt();
            if (modifiedAtField != null) {
                String valueOf = String.valueOf(Instant.now().getEpochSecond() - Long.parseLong(modifiedAtField.getValue()));
                Query modifiedAt = this.buildRangeQuery("modifiedAt", valueOf, modifiedAtField.getCompare());
                mainQueryBuilder.must(modifiedAt);
            }
            
            // 排序
            var sortOptions = buildOrderByQuery(searchDto.getOrder());
            
            // 分页
            CommonSearchDto.Paging paging = searchDto.getPaging();
            
            // 最终查询
            Query finalQuery = mainQueryBuilder.build()._toQuery();
            
            // 创建搜索请求
            SearchRequest sReq = SearchRequest.of(s -> s
                .index(getIndexName())
                .query(finalQuery)
                // 优化返回字段，减少网络传输
                .source(SourceConfig.of(sc -> sc
                    .filter(f -> f.excludes("content"))
                ))
                .trackScores(true)          // 追踪分数
                .trackTotalHits(t -> t.enabled(true)) // 精确计算总数
                .timeout("5s")              // 设置超时
                .sort(sortOptions)
                .size(paging.getLimit())
                .from(paging.getOffset())
                // 添加优先级和缓存控制
                .preference("_local")       // 优先使用本地分片
                .requestCache(true));      // 使用请求缓存
                
            log.info("search:{}", sReq.toString());
            var sResp = elasticClient.getClient().search(sReq, FullTextSearchModel.class);
            TotalHits total = sResp.hits().total();
            List<Hit<FullTextSearchModel>> hits = sResp.hits().hits();

            resp.setSize(total.value());
            for (Hit<FullTextSearchModel> hit : hits) {
                resp.getData().add(hit.source());
            }
        } catch (IOException ex) {
            log.error("search err", ex);
        } catch (ElasticsearchException ex) {
            if (ex.getMessage().contains("index_not_found_exception")) {
                createIndex(true);
            } else {
                log.error("search err", ex);
            }
        }
        return resp;
    }

    private List<SortOptions> buildOrderByQuery(CommonSearchDto.OrderBy orderBy) {
        if (Objects.isNull(orderBy)) {
            return Collections.emptyList();
        }
        SortOrder sortOrder = orderBy.isAsc() ? SortOrder.Asc : SortOrder.Desc;
        var so = SortOptions.of(l -> l.field(m -> m.field(orderBy.getField()).order(sortOrder)));
        return Collections.singletonList(so);
    }

    @Nullable
    public Query buildRangeQuery(String field, String val, CompareEnum cmp) {
        if (CompareEnum.GT.equals(cmp)) {
            return RangeQuery.of(t -> t.number(l -> l.field(field).gt(Double.parseDouble(val) * 1000)))._toQuery();
        } else if (CompareEnum.LT.equals(cmp)) {
            return RangeQuery.of(t -> t.number(l -> l.field(field).lt(Double.parseDouble(val) * 1000)))._toQuery();
        }
        return null;
    }

    @Override
    public SearchResultDto randomSearch(RandomSearchDto searchDto) {
        // 如果没有初始化索引，则创建。
        if (!searchConfig.getRuntime().getFtsInitIndex()) {
            createIndex(false);
        }
        // 执行搜索
        SearchResultDto resp = new SearchResultDto();
        resp.setData(new ArrayList<>());

        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s.index(getIndexName())
                    .query(q -> q.functionScore(r -> {
                        return r.query(t -> t.matchAll(k -> k))
                                .functions(FunctionScore.of(l -> l.randomScore(m -> m)));
                    }))
                    .source(l -> l.filter(m -> m.excludes("content")))
                    .size(searchDto.getLimit()));
            SearchResponse<FullTextSearchModel> search = elasticClient.getClient().search(searchRequest,
                    FullTextSearchModel.class);
            TotalHits total = search.hits().total();
            resp.setSize(total.value());

            List<Hit<FullTextSearchModel>> hits = search.hits().hits();

            for (Hit<FullTextSearchModel> hit : hits) {
                resp.getData().add(hit.source());
            }
        } catch (IOException ex) {
            log.error("randomSearch err", ex);
        }
        return resp;
    }

    @Override
    public LLMSearchResultDto llmSearch(LLMSearchDto searchDto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'llmSearch'");
    }

    @Override
    public void forceMerge() {
        ForcemergeRequest forcemergeRequest = ForcemergeRequest.of(t -> t.maxNumSegments(1L).onlyExpungeDeletes(true));
        try {
            log.info("run forceMerge");
            elasticClient.getClient().indices().forcemerge(forcemergeRequest);
        } catch (IOException e) {
            log.error("forceMerge error.", e);
        }
    }
}
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
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
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
                boolean indexExist = elasticClient.getClient().indices().exists(b -> b.index(getIndexName())).value();

                if (indexExist && deleteIfExist) {
                    log.info("删除已存在的索引 {}", getIndexName());
                    DeleteIndexResponse delete = elasticClient.getClient().indices()
                            .delete(c -> c.index(getIndexName()));
                    log.info("索引删除结果: {}", delete.acknowledged());
                }

                if (!indexExist || deleteIfExist) {
                    log.info("创建新索引 {}, mapping配置: {}", getIndexName(), ElasticMapping.generate());
                    CreateIndexResponse response = elasticClient.getClient().indices()
                            .create(c -> c.index(getIndexName()).mappings(d -> d.properties(ElasticMapping.generate()))
                                    .settings(s -> s.analysis(k -> k.analyzer("path_tokenizer",
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
            GetResponse<FullTextSearchModel> response = elasticClient.getClient()
                    .get(t -> t.index(getIndexName()).id(resId), FullTextSearchModel.class);
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

            // 使用与主搜索相同的复杂查询逻辑，提高一致性和准确性
            if (!StringUtils.isEmpty(text)) {
                // 使用优化的复合查询构建器
                BoolQuery.Builder textQuery = getComplexQuery(text);
                builder.should(textQuery.build()._toQuery());
            }

            Query redId = TermQuery.of(m -> m.field("resId").value(resId))._toQuery();
            builder.must(redId);

            // 改进的高亮配置
            Highlight highlight = Highlight.of(t -> t
                    // 高亮内容字段
                    .fields("content", HighlightField.of(k -> k.type("unified") // 使用统一高亮器，更适合中文
                            .fragmentSize(350) // 增加片段大小，提供更多上下文
                            .numberOfFragments(20) // 增加片段数量，确保捕获更多相关片段
                            .fragmentOffset(150) // 增加片段偏移量，保证更多上下文
                            .preTags("<em>") // 高亮开始标签
                            .postTags("</em>") // 高亮结束标签
                            .fragmenter(HighlighterFragmenter.Span) // 使用Span分段器更精确
                            .order(HighlighterOrder.Score) // 按相关性排序片段
                            .noMatchSize(200) // 如果没有匹配，返回的文本大小
                            .requireFieldMatch(false) // 不要求字段匹配，可以匹配多个字段
                    ))
                    // 增加标题字段的高亮
                    .fields("resTitle", HighlightField
                            .of(k -> k.type("unified").preTags("<em>").postTags("</em>").numberOfFragments(0) // 0表示不分片，返回完整字段
                            ))
                    // 增加文件名的高亮
                    .fields("resName", HighlightField
                            .of(k -> k.type("unified").preTags("<em>").postTags("</em>").numberOfFragments(0) // 0表示不分片，返回完整字段
                            )));

            SearchRequest searchRequest = SearchRequest
                    .of(s -> s.index(getIndexName()).query(q -> q.bool(t -> builder)).highlight(highlight)
                            // 使用全量源字段，确保所有字段都可用于高亮
                            .source(SourceConfig.of(sc -> sc.filter(f -> f.includes("*")))));

            log.info("search:{}", searchRequest.toString());
            SearchResponse<FullTextSearchModel> search = elasticClient.getClient().search(searchRequest,
                    FullTextSearchModel.class);

            List<Hit<FullTextSearchModel>> hits = search.hits().hits();

            for (Hit<FullTextSearchModel> hit : hits) {
                ResourceHighlightHitsDto dto = new ResourceHighlightHitsDto();
                dto.setResource(hit.source());

                // 合并内容和标题的高亮结果
                List<String> contentHighlights = hit.highlight().get("content");
                List<String> titleHighlights = hit.highlight().get("resTitle");
                List<String> nameHighlights = hit.highlight().get("resName");

                List<String> allHighlights = new ArrayList<>();
                if (contentHighlights != null) {
                    allHighlights.addAll(contentHighlights);
                }
                if (titleHighlights != null) {
                    allHighlights.addAll(titleHighlights);
                }
                if (nameHighlights != null) {
                    allHighlights.addAll(nameHighlights);
                }

                dto.setHighlights(allHighlights);
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
                log.warn("文件内容过大，跳过：resId: {}, contentSize:{} > highlightMaxAnalyzedOffset:{}", model.getResId(),
                        model.getContentSize(), maxSize);
                onSuccess.run();
                return true;
            }
            IndexResponse response = elasticClient.getClient()
                    .index(i -> i.index(getIndexName()).id(model.getResId()).document(model));
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
                BoolQuery.Builder complexQuery = getComplexQuery(searchDto.getSearchQuery());
                mainQueryBuilder.must(complexQuery.build()._toQuery());
            }

            // 处理其他过滤条件，沿用现有逻辑
            if (CollectionUtils.isNotEmpty(searchDto.getResTypeList())) {
                BoolQuery.Builder orResType = new BoolQuery.Builder();
                for (String resType : searchDto.getResTypeList()) {
                    Query resTypeQuery = TermQuery.of(m -> m.field("resType").value(resType))._toQuery();
                    orResType.should(resTypeQuery);
                }
                mainQueryBuilder.must(orResType.build()._toQuery());
            }
            if (StringUtils.isNotEmpty(searchDto.getDirPrefix())) {
                Query resDir = TermQuery.of(m -> m.field("resDir").value(searchDto.getDirPrefix()))._toQuery();
                mainQueryBuilder.must(resDir);
            }
            Field resSizeField = searchDto.getResSize();
            if (resSizeField != null) {
                Query resSize = this.buildRangeQuery("resSize", resSizeField.getValue(), resSizeField.getCompare());
                mainQueryBuilder.must(resSize);
            }
            Field modifiedAtField = searchDto.getModifiedAt();
            if (modifiedAtField != null) {
                String valueOf = String
                        .valueOf(Instant.now().getEpochSecond() - Long.parseLong(modifiedAtField.getValue()));
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
            SearchRequest sReq = SearchRequest.of(s -> s.index(getIndexName()).query(finalQuery)
                    // 优化返回字段，减少网络传输
                    .source(SourceConfig.of(sc -> sc.filter(f -> f.excludes("content")))).trackScores(true) // 追踪分数
                    .trackTotalHits(t -> t.enabled(true)) // 精确计算总数
                    .timeout("5s") // 设置超时
                    .sort(sortOptions).size(paging.getLimit()).from(paging.getOffset())
                    // 添加优先级和缓存控制
                    .preference("_local") // 优先使用本地分片
                    .requestCache(true)); // 使用请求缓存

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

    /**
     * 构建复合查询
     * 
     * @param searchQuery
     * @return
     */
    private BoolQuery.Builder getComplexQuery(String searchQuery) {
        // 对搜索查询进行预处理，移除多余空格
        String cleanQuery = searchQuery.trim().replaceAll("\\s+", " ");
        int queryLength = cleanQuery.length();
        boolean isLongQuery = queryLength > 10;
        boolean isVeryLongQuery = queryLength > 20;

        // 组装BoolQuery - 改进的复合查询结构
        BoolQuery.Builder complexQuery = new BoolQuery.Builder();

        // 长句子搜索优化：切换策略处理长短查询
        if (isVeryLongQuery) {
            // 超长查询：特殊处理 - 使用OR条件和低匹配度要求
            Query multiMatchLong = MultiMatchQuery
                    .of(m -> m.query(cleanQuery).type(TextQueryType.BestFields).operator(Operator.Or) // 使用OR操作符增加匹配可能性
                            .fields("content^1", "resName^8", "resTitle^4").minimumShouldMatch("30%")) // 降低最小匹配度
                    ._toQuery();

            // 添加短语匹配但增大slop值
            Query contentPhraseLoose = MatchPhraseQuery.of(m -> m.field("content").query(cleanQuery).slop(10) // 大幅增加slop容忍度
                    .boost(4.0f))._toQuery();

            // 使用带权重的分段短语搜索
            BoolQuery.Builder phraseParts = new BoolQuery.Builder();
            String[] parts = splitLongQuery(cleanQuery);
            for (String part : parts) {
                if (part.length() > 3) {
                    phraseParts.should(
                            MatchPhraseQuery.of(m -> m.field("content").query(part).slop(5).boost(3.0f))._toQuery());
                }
            }

            // 主查询使用OR条件，确保召回率
            complexQuery.should(multiMatchLong, contentPhraseLoose, phraseParts.build()._toQuery());

            // 设置最小应匹配条件
            complexQuery.minimumShouldMatch("1");

        } else {
            // 常规查询：使用原先的逻辑
            // 1. 优化跨字段搜索 - 按照字段重要性调整权重
            Query multiFieldQuery = MultiMatchQuery.of(
                    m -> m.query(cleanQuery).type(isLongQuery ? TextQueryType.BestFields : TextQueryType.CrossFields)
                            .operator(isLongQuery ? Operator.Or : Operator.And) // 长查询使用OR
                            .fields("content^1", "resName^15", "resTitle^8", "resType^2")
                            .minimumShouldMatch(isLongQuery ? "40%" : "60%") // 长查询降低匹配度要求
                            .tieBreaker(0.4))
                    ._toQuery();

            // 2. 短语匹配 - 提高精确匹配项的权重
            Query contentPhrase = MatchPhraseQuery.of(m -> m.field("content").query(cleanQuery)
                    .slop(isLongQuery ? 5 : 2).boost(isLongQuery ? 7.0f : 3.0f))._toQuery();

            // 3. 标题短语匹配 - 更高优先级
            Query titlePhrase = MatchPhraseQuery.of(m -> m.field("resTitle").query(cleanQuery).slop(1).boost(8.0f))
                    ._toQuery();

            // 4. 文件名短语匹配 - 最高优先级
            Query namePhrase = MatchPhraseQuery.of(m -> m.field("resName").query(cleanQuery).slop(0).boost(12.0f))
                    ._toQuery();

            // 5. 增加前缀匹配，提高部分匹配精度
            Query prefixQuery = null;
            if (queryLength >= 3 && queryLength <= 15) {
                prefixQuery = WildcardQuery.of(w -> w.field("resName").wildcard("*" + cleanQuery.toLowerCase() + "*")
                        .caseInsensitive(true).boost(3.0f))._toQuery();
            }

            // 至少要满足多字段匹配（基础查询）
            complexQuery.must(multiFieldQuery);

            // 加分项 - 匹配任一项将提高得分
            complexQuery.should(contentPhrase, titlePhrase, namePhrase);
            if (prefixQuery != null) {
                complexQuery.should(prefixQuery);
            }

            // 对于长查询，增加短语匹配的权重
            if (isLongQuery) {
                complexQuery.should(s -> s.matchPhrase(mp -> mp.field("content").query(cleanQuery).boost(10.0f)));
            }
        }

        return complexQuery;
    }

    /**
     * 将长查询分割成有意义的片段
     */
    private String[] splitLongQuery(String query) {
        // 按标点或空格分割
        return query.split("[,，.。;；!！?？\\s]+");
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
            SearchRequest searchRequest = SearchRequest
                    .of(s -> s.index(getIndexName()).query(q -> q.functionScore(r -> {
                        return r.query(t -> t.matchAll(k -> k)).functions(FunctionScore.of(l -> l.randomScore(m -> m)));
                    })).source(l -> l.filter(m -> m.excludes("content"))).size(searchDto.getLimit()));
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
        // 修复互斥参数问题：onlyExpungeDeletes和maxNumSegments不能同时使用
        // 选择使用maxNumSegments参数，这通常更有用于提高查询性能
        ForcemergeRequest forcemergeRequest = ForcemergeRequest.of(t -> t.maxNumSegments(1L)
        // .onlyExpungeDeletes(true) - 不能与maxNumSegments同时使用
        );

        try {
            log.info("执行索引强制合并操作，将分段数量减少到 1");
            elasticClient.getClient().indices().forcemerge(forcemergeRequest);
        } catch (IOException e) {
            log.error("forceMerge 操作失败", e);
        }
    }
}
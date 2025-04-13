package noogel.xyz.search.infrastructure.repo.impl.elastic;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.*;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

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

    @Nullable
    @Override
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

            Highlight highlight = Highlight.of(t -> t.fields("content", HighlightField.of(k -> k
                    .type("plain")
                    // 上下文内容
                    .fragmentSize(200)
                    // 8 条
                    .numberOfFragments(8)
                    // 高亮标记
                    .fragmenter(HighlighterFragmenter.Span))));

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(getIndexName())
                    .query(q -> q.bool(t -> builder))
//                    .source(l -> l.filter(m -> m.excludes("content")))
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
            BoolQuery.Builder builder = new BoolQuery.Builder();

            if (!StringUtils.isEmpty(searchDto.getSearchQuery())) {
                Query content = MatchQuery.of(m -> m
                        .field("content")
                        .query(searchDto.getSearchQuery())
                        .analyzer("ik_smart"))._toQuery();
                Query resName = MatchQuery.of(m -> m
                        .field("resName")
                        .query(searchDto.getSearchQuery())
                        .boost(10.f)
                        .analyzer("ik_smart"))._toQuery();
                BoolQuery.Builder orSearch = new BoolQuery.Builder();
                orSearch.should(content, resName);
                builder.must(l -> l.bool(orSearch.build()));
            }
            if (CollectionUtils.isNotEmpty(searchDto.getResTypeList())) {
                BoolQuery.Builder orResType = new BoolQuery.Builder();
                for (String resType : searchDto.getResTypeList()) {
                    Query resTypeQuery = TermQuery.of(m -> m
                            .field("resType")
                            .value(resType))._toQuery();
                    orResType.should(resTypeQuery);
                }
                builder.must(orResType.build()._toQuery());
            }
            if (StringUtils.isNotEmpty(searchDto.getDirPrefix())) {
                Query resDir = TermQuery.of(m -> m
                        .field("resDir")
                        .value(searchDto.getDirPrefix()))._toQuery();
                builder.must(resDir);
            }
            Field resSizeField = searchDto.getResSize();
            if (resSizeField != null) {
                Query resSize = this.buildRangeQuery("resSize", resSizeField.getValue(), resSizeField.getCompare());
                builder.must(resSize);
            }
            Field modifiedAtField = searchDto.getModifiedAt();
            if (modifiedAtField != null) {
                String valueOf = String.valueOf(Instant.now().getEpochSecond() - Long.parseLong(modifiedAtField.getValue()));
                Query modifiedAt = this.buildRangeQuery("modifiedAt", valueOf, modifiedAtField.getCompare());
                builder.must(modifiedAt);
            }
            // 排序
            var sortOptions = buildOrderByQuery(searchDto.getOrder());
            // 分页
            CommonSearchDto.Paging paging = searchDto.getPaging();
            SearchRequest sReq = SearchRequest.of(s -> s.index(getIndexName())
                    .query(q -> q.bool(t -> builder))
                    .source(l -> l.filter(m -> m.excludes("content")))
                    .sort(sortOptions)
                    .size(paging.getLimit())
                    .from(paging.getOffset()));
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
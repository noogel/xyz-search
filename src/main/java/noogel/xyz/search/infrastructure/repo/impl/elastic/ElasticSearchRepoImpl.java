package noogel.xyz.search.infrastructure.repo.impl.elastic;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.HighlighterFragmenter;
import co.elastic.clients.elasticsearch.core.search.Hit;
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
        try {
            // 查询是否存在
            boolean indexExist = elasticClient.getClient().indices().exists(b -> b.index(getIndexName())).value();
            if (indexExist && deleteIfExist) {
                // 删除索引
                DeleteIndexResponse delete = elasticClient.getClient().indices().delete(c -> c.index(getIndexName()));
                log.info("DeleteIndexResponse delete: {}", delete.acknowledged());
            }
            if (!indexExist || deleteIfExist) {
                // 创建索引和 mapping
                CreateIndexResponse response = elasticClient.getClient().indices().create(c -> c
                        .index(getIndexName()).mappings(d -> d.properties(ElasticMapping.generate()))
                        .settings(s -> s.analysis(k ->
                        // 自定义路径分词工具
                        k.analyzer("path_tokenizer", a -> a.custom(l -> l.tokenizer("path_hierarchy"))))));
                log.info("CreateIndexResponse delete: {}", response.acknowledged());
                // 持久化配置
                searchConfig.getRuntime().setFtsInitIndex(true);
                searchConfig.overrideToFile();
            }
            return true;
        } catch (IOException ex) {
            log.error("createIndex err", ex);
            return false;
        }
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
                Query searchableText = MatchQuery.of(m -> m.field("searchableText")
                        .query(text).analyzer("ik_smart"))._toQuery();
                Query resTitle = MatchQuery.of(m -> m.field("resTitle")
                        .query(text).analyzer("ik_smart"))._toQuery();
                builder.should(searchableText, resTitle);
            }
            Query redId = TermQuery.of(m -> m.field("resId").value(resId))._toQuery();
            builder.must(redId);

            Highlight highlight = Highlight.of(t -> t.fields("searchableText", HighlightField.of(k -> k
                    .type("plain")
                    // 上下文内容
                    .fragmentSize(200)
                    // 30 条
                    .numberOfFragments(8)
                    // 高亮标记
                    .fragmenter(HighlighterFragmenter.Span))));

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(getIndexName())
                    .query(q -> q.bool(t -> builder))
                    .source(l -> l.filter(m -> m.excludes("searchableText")))
                    .highlight(highlight));
            log.info("search:{}", searchRequest.toString());
            SearchResponse<FullTextSearchModel> search = elasticClient.getClient().search(searchRequest,
                    FullTextSearchModel.class);

            List<Hit<FullTextSearchModel>> hits = search.hits().hits();

            for (Hit<FullTextSearchModel> hit : hits) {
                ResourceHighlightHitsDto dto = new ResourceHighlightHitsDto();
                dto.setResource(hit.source());
                dto.setHighlights(hit.highlight().get("searchableText"));
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
            IndexResponse response = elasticClient.getClient().index(i -> i
                    .index(getIndexName())
                    .id(model.getResId())
                    .document(model));
            return response.version() > 0;
        } catch (IOException ex) {
            log.error("upsertData err", ex);
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
                Query searchableText = MatchQuery.of(m -> m
                        .field("searchableText")
                        .query(searchDto.getSearchQuery())
                        .analyzer("ik_smart"))._toQuery();
                Query resName = MatchQuery.of(m -> m
                        .field("resName")
                        .query(searchDto.getSearchQuery())
                        .boost(10.f)
                        .analyzer("ik_smart"))._toQuery();
                BoolQuery.Builder orSearch = new BoolQuery.Builder();
                orSearch.should(searchableText, resName);
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
                Query resId = TermQuery.of(m -> m
                        .field("resDir")
                        .value(searchDto.getDirPrefix()))._toQuery();
                builder.must(resId);
            }
            Field resSizeField = searchDto.getResSize();
            if (resSizeField != null) {
                Query resSize = this.buildRangeQuery("resSize", resSizeField.getValue(), resSizeField.getCompare());
                builder.must(resSize);
            }
            Field modifiedAtField = searchDto.getModifiedAt();
            if (modifiedAtField != null) {
                String valueOf = String
                        .valueOf(Instant.now().getEpochSecond() - Long.parseLong(modifiedAtField.getValue()));
                Query modifiedAt = this.buildRangeQuery("modifiedAt", valueOf, modifiedAtField.getCompare());
                builder.must(modifiedAt);
            }
            SearchRequest searchRequest = SearchRequest.of(s -> s.index(getIndexName())
                    .query(q -> q.bool(t -> builder))
                    .source(l -> l.filter(m -> m.excludes("searchableText")))
                    .sort(StringUtils.isNoneBlank(searchDto.getDirPrefix()) ? Collections.singletonList(SortOptions
                            .of(l -> l.field(m -> m.field("rank").order(SortOrder.Asc)))) : Collections.emptyList())
                    .size(searchDto.getPaging().getLimit())
                    .from(searchDto.getPaging().getOffset()));
            log.info("search:{}", searchRequest.toString());
            SearchResponse<FullTextSearchModel> search = elasticClient.getClient().search(searchRequest,
                    FullTextSearchModel.class);
            TotalHits total = search.hits().total();
            resp.setSize(total.value());

            List<Hit<FullTextSearchModel>> hits = search.hits().hits();

            for (Hit<FullTextSearchModel> hit : hits) {
                resp.getData().add(hit.source());
            }
        } catch (IOException ex) {
            log.error("search err", ex);
        }
        return resp;
    }

    @Nullable
    public Query buildRangeQuery(String field, String val, CompareEnum cmp) {
        if (CompareEnum.GT.equals(cmp)) {
            return RangeQuery.of(t -> t.number(l -> l.field(field).gt(Double.parseDouble(val))))._toQuery();
        } else if (CompareEnum.LT.equals(cmp)) {
            return RangeQuery.of(t -> t.number(l -> l.field(field).lt(Double.parseDouble(val))))._toQuery();
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
            BoolQuery.Builder builder = new BoolQuery.Builder();

            SearchRequest searchRequest = SearchRequest.of(s -> s.index(getIndexName())
                    .query(q -> q.functionScore(r -> {
                        return r.query(t -> t.matchAll(k -> k))
                                .functions(FunctionScore.of(l -> l.randomScore(m -> m)));
                    }))
                    .source(l -> l.filter(m -> m.excludes("searchableText")))
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
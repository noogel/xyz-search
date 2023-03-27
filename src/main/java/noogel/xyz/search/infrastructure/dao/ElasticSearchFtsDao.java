package noogel.xyz.search.infrastructure.dao;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ForcemergeRequest;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ElasticsearchConfig;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import noogel.xyz.search.infrastructure.dto.ResourceHighlightHitsDto;
import noogel.xyz.search.infrastructure.dto.SearchBaseQueryDto;
import noogel.xyz.search.infrastructure.dto.SearchResultDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.model.ResourceModel;
import noogel.xyz.search.infrastructure.utils.ElasticSearchQueryHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Repository
@Slf4j
public class ElasticSearchFtsDao {

    @Resource
    private ElasticsearchConfig config;
    @Resource
    private volatile SearchPropertyConfig.SearchConfig searchConfig;

    /**
     * 创建 mapping
     */
    public boolean createIndex(boolean deleteIfExist) {
        try {
            // 查询是否存在
            boolean indexExist = config.getClient().indices().exists(b -> b.index(getIndexName())).value();
            if (indexExist && deleteIfExist) {
                // 删除索引
                DeleteIndexResponse delete = config.getClient().indices().delete(c -> c.index(getIndexName()));
                log.info("DeleteIndexResponse delete: {}", delete.acknowledged());
            }
            if (!indexExist || deleteIfExist) {
                // 创建索引和 mapping
                CreateIndexResponse response = config.getClient().indices().create(c -> c
                        .index(getIndexName()).mappings(d -> d.properties(ResourceModel.generateEsMapping()))
                        .settings(s ->
                                s.analysis(k ->
                                        // 自定义路径分词工具
                                        k.analyzer("path_tokenizer", a->
                                                a.custom(l-> l.tokenizer("path_hierarchy"))))));
                log.info("CreateIndexResponse delete: {}", response.acknowledged());
                // 持久化配置
                searchConfig.setInitIndex(true);
                searchConfig.saveToFile();
            }
            return true;
        } catch (IOException ex) {
            log.error("createIndex err", ex);
            return false;
        }
    }

    /**
     * 获取索引名称
     * @return
     */
    private String getIndexName() {
        return searchConfig.getFtsIndexName();
    }

    public boolean upsertData(ResourceModel model) {
        try {
            // 如果没有初始化索引，则创建。
            if (!searchConfig.isInitIndex()) {
                createIndex(false);
            }
            /** Demo
             * ResourceModel(resId=4997a7dd90fe3d4c81f7d8802ae553f4,
             * resName=43丨Socket通信：遇上特大项目，要学会和其他公司合作.html,
             * resDir=/home/xyz/DockerSharingData/TestSearch/极客时间/15-趣谈Linux操作系统/09-核心原理篇：第八部分 网络系统 (7讲),
             * resHash=9a9c4bd4d34faa4fd6ce726cdc6a532d,
             * resType=FILE:HTML,
             * resSize=1657045,
             * modifiedAt=1664959701496,
             * searchableText=极客时间 | 趣谈Linux操作系统,
             * textHash=dfb16b1b5ba04b745b51b48e16959c4a,
             * textSize=5287,
             * taskId=1665580844,
             * taskOpAt=1665580844)
             */
            IndexResponse response = config.getClient().index(i -> i
                    .index(getIndexName())
                    .id(model.getResId())
                    .document(model)
            );
            return response.version() > 0;
        } catch (IOException ex) {
            log.error("upsertData err", ex);
            return false;
        }

    }

    public boolean deleteByResId(ResourceModel res) {
        try {
            DeleteResponse delete = config.getClient().delete(b -> b.index(getIndexName()).id(res.getResId()));
            boolean result = delete.version() > 0;
            log.info("deleteByResId {}", res.calculateAbsolutePath());
            return result;
        } catch (IOException ex) {
            log.error("deleteByResId err", ex);
            return false;
        }
    }

    public boolean forceMerge() {
        ForcemergeRequest forcemergeRequest = ForcemergeRequest.of(t -> t.maxNumSegments(1L).onlyExpungeDeletes(true));
        try {
            log.info("run forceMerge");
            config.getClient().indices().forcemerge(forcemergeRequest);
            return true;
        } catch (IOException e) {
            log.error("forceMerge error.", e);
            return false;
        }
    }

    public ResourceModel findByResId(String resId) {
        try {
            GetResponse<ResourceModel> response = config.getClient().get(t -> t.index(getIndexName()).id(resId),
                    ResourceModel.class);
            return response.source();
        } catch (Exception ex) {
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(ex);
        }
    }

    public SearchResultDto searchOldRes(String resDir, Long taskOpAt) {
        SearchResultDto resp = new SearchResultDto();
        resp.setData(new ArrayList<>());
        try {
            BoolQuery.Builder builder = new BoolQuery.Builder();
            if (StringUtils.isNotBlank(resDir)) {
                Query q1 = TermQuery.of(m -> m.field("resDir").value(resDir))._toQuery();
                builder.must(q1);
            }
            Query q2 = ElasticSearchQueryHelper.buildRangeQuery("taskOpAt",
                    String.format("%s:%s", "LT", taskOpAt), t -> t);
            builder.must(q2);
            SearchResponse<ResourceModel> search = config.getClient().search(s -> s
                            .index(getIndexName())
                            .query(q -> q.bool(t -> builder))
                            .source(l -> l.filter(m -> m.excludes("searchableText")))
                            .size(100),
                    ResourceModel.class);

            TotalHits total = search.hits().total();
            resp.setExactSize(total.relation() == TotalHitsRelation.Eq);
            resp.setSize(total.value());

            List<Hit<ResourceModel>> hits = search.hits().hits();
            for (Hit<ResourceModel> hit : hits) {
                resp.getData().add(hit.source());
            }
        } catch (IOException ex) {
            log.error("searchOldRes err", ex);
        }
        return resp;
    }

    @Nullable
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
                    .highlight(highlight)
            );
            log.info("search:{}", searchRequest.toString());
            SearchResponse<ResourceModel> search = config.getClient().search(searchRequest, ResourceModel.class);

            List<Hit<ResourceModel>> hits = search.hits().hits();

            for (Hit<ResourceModel> hit : hits) {
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

    public SearchResultDto search(SearchBaseQueryDto queryDto) {
        // 如果没有初始化索引，则创建。
        if (!searchConfig.isInitIndex()) {
            createIndex(false);
        }
        // 执行搜索
        SearchResultDto resp = new SearchResultDto();
        resp.setData(new ArrayList<>());

        try {
            BoolQuery.Builder builder = new BoolQuery.Builder();

            if (!StringUtils.isEmpty(queryDto.getSearch())) {
                Query searchableText = MatchQuery.of(m -> m
                        .field("searchableText")
                        .query(queryDto.getSearch())
                        .analyzer("ik_smart")
                )._toQuery();
                Query resName = MatchQuery.of(m -> m
                        .field("resName")
                        .query(queryDto.getSearch())
                        .boost(10.f)
                        .analyzer("ik_smart")
                )._toQuery();
                BoolQuery.Builder orSearch = new BoolQuery.Builder();
                orSearch.should(searchableText, resName);
                builder.must(l-> l.bool(orSearch.build()));
            }
            if (!StringUtils.isEmpty(queryDto.getResType())) {
                Query resType = TermQuery.of(m -> m
                        .field("resType")
                        .value(queryDto.getResType())
                )._toQuery();
                builder.must(resType);
            }
            if (!StringUtils.isEmpty(queryDto.getResDirPrefix())) {
                Query resId = TermQuery.of(m -> m
                        .field("resDir")
                        .value(queryDto.getResDirPrefix())
                )._toQuery();
                builder.must(resId);
            }
            if (!StringUtils.isEmpty(queryDto.getResSize())) {
                Query resSize = ElasticSearchQueryHelper.buildRangeQuery("resSize", queryDto.getResSize(), t -> t);
                builder.must(resSize);
            }
            if (!StringUtils.isEmpty(queryDto.getModifiedAt())) {
                Function<String, String> fn = (t) -> String.valueOf(Instant.now().getEpochSecond() - Long.parseLong(t));
                Query modifiedAt = ElasticSearchQueryHelper.buildRangeQuery("modifiedAt",
                        queryDto.getModifiedAt(), fn);
                builder.must(modifiedAt);
            }
            SearchRequest searchRequest = SearchRequest.of(s -> s.index(getIndexName())
                    .query(q -> q.functionScore(r -> {
                        if (queryDto.emptyQuery()) {
                            return r.query(t -> t.matchAll(k -> k))
                                    .functions(FunctionScore.of(l -> l.randomScore(m -> m)));
                        } else {
                            return r.query(l -> l.bool(t -> builder));
                        }
                    }))
                    .source(l -> l.filter(m -> m.excludes("searchableText")))
                    .sort(queryDto.dirQuery() ? Collections.singletonList(SortOptions
                            .of(l -> l.field(m -> m.field("rank").order(SortOrder.Asc)))) : Collections.emptyList())
                    .size(queryDto.getLimit())
                    .from(queryDto.getOffset()));
            log.info("search:{}", searchRequest.toString());
            SearchResponse<ResourceModel> search = config.getClient().search(searchRequest, ResourceModel.class);
            TotalHits total = search.hits().total();
            resp.setExactSize(total.relation() == TotalHitsRelation.Eq);
            resp.setSize(total.value());

            List<Hit<ResourceModel>> hits = search.hits().hits();

            for (Hit<ResourceModel> hit : hits) {
                resp.getData().add(hit.source());
            }
        } catch (IOException ex) {
            log.error("search err", ex);
        }
        return resp;
    }

}

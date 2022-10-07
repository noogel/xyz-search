package noogel.xyz.search.infrastructure.dao;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ElasticsearchConfig;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import noogel.xyz.search.infrastructure.dto.ResourceHighlightHitsDto;
import noogel.xyz.search.infrastructure.dto.SearchQueryDto;
import noogel.xyz.search.infrastructure.dto.SearchResultDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.model.ResourceModel;
import noogel.xyz.search.infrastructure.utils.ElasticSearchQueryHelper;
import org.springframework.stereotype.Repository;
import org.thymeleaf.util.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
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
                        .index(getIndexName()).mappings(d -> d.properties(ResourceModel.generateEsMapping())));
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

    public boolean deleteByResId(String resId) {
        try {
            DeleteResponse delete = config.getClient().delete(b -> b.index(getIndexName()).id(resId));
            boolean result = delete.version() > 0;
            log.info("deleteByResId {} {}", resId, result);
            return result;
        } catch (IOException ex) {
            log.error("deleteByResId err", ex);
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

    public SearchResultDto searchOldRes(String resPathHash, Long taskOpAt) {
        SearchResultDto resp = new SearchResultDto();
        resp.setData(new ArrayList<>());

        try {
            BoolQuery.Builder builder = new BoolQuery.Builder();
            Query q1 = TermQuery.of(m -> m.field("resPathHash").value(resPathHash))._toQuery();
            builder.must(q1);
            Query q2 = ElasticSearchQueryHelper.buildRangeQuery("taskOpAt",
                    String.format("%s:%s", "LT", taskOpAt), t -> t);
            builder.must(q2);
            SearchResponse<ResourceModel> search = config.getClient().search(s -> s
                            .index(getIndexName())
                            .query(q -> q.bool(t -> builder))
                            .source(l -> l.filter(m -> m.excludes("searchableText")))
                            .size(10),
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
    public ResourceHighlightHitsDto searchByResId(String resId, @Nullable String searchableText) {
        try {
            BoolQuery.Builder builder = new BoolQuery.Builder();
            if (!StringUtils.isEmpty(searchableText)) {
                Query byName = MatchQuery.of(m -> m.field("searchableText")
                        .query(searchableText).analyzer("smartcn"))._toQuery();
                builder.must(byName);
            }
            Query redId = TermQuery.of(m -> m.field("resId").value(resId))._toQuery();
            builder.must(redId);

            Highlight highlight = Highlight.of(t -> t.fields("searchableText", HighlightField.of(k -> k
                    .type("plain")
                    // 上下文内容
                    .fragmentSize(100)
                    // 30 条
                    .numberOfFragments(30)
                    // 高亮标记
                    .fragmenter(HighlighterFragmenter.Span))));

            SearchResponse<ResourceModel> search = config.getClient().search(s -> s
                            .index(getIndexName())
                            .query(q -> q.bool(t -> builder))
                            .source(l -> l.filter(m -> m.excludes("searchableText")))
                            .highlight(highlight),
                    ResourceModel.class);

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

    public SearchResultDto search(SearchQueryDto queryDto) {
        SearchResultDto resp = new SearchResultDto();
        resp.setData(new ArrayList<>());

        try {
            BoolQuery.Builder builder = new BoolQuery.Builder();
            if (!StringUtils.isEmpty(queryDto.getSearch())) {
                Query searchableText = MatchQuery.of(m -> m
                        .field("searchableText")
                        .query(queryDto.getSearch())
                        .analyzer("smartcn")
                )._toQuery();
                builder.must(searchableText);
            }
            if (!StringUtils.isEmpty(queryDto.getResId())) {
                Query resId = TermQuery.of(m -> m
                        .field("resId")
                        .value(queryDto.getResId())
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
            SearchResponse<ResourceModel> search = config.getClient().search(s -> s
                            .index(getIndexName())
                            .query(q -> q.bool(t -> builder))
                            .source(l -> l.filter(m -> m.excludes("searchableText")))
                            .size(queryDto.getLimit())
                            .from(queryDto.getOffset()),
                    ResourceModel.class);
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

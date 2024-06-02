package noogel.xyz.search.infrastructure.dao.elastic;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ForcemergeRequest;
import co.elastic.clients.util.ObjectBuilder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ElasticsearchConfig;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import noogel.xyz.search.infrastructure.dto.ResourceHighlightHitsDto;
import noogel.xyz.search.infrastructure.dto.SearchBaseQueryDto;
import noogel.xyz.search.infrastructure.dto.SearchResultDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.model.elastic.FileEsModel;
import noogel.xyz.search.infrastructure.utils.ElasticSearchQueryHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

@Repository
@Slf4j
public class ElasticDao {

    @Resource
    private ElasticsearchConfig config;
    @Resource
    private volatile SearchPropertyConfig.SearchConfig searchConfig;

    private static void parseSearchResult(SearchResultDto resp, SearchResponse<FileEsModel> search) {
        TotalHits total = search.hits().total();
        resp.setExactSize(Objects.nonNull(total) && total.relation() == TotalHitsRelation.Eq);
        resp.setSize(Optional.ofNullable(total).map(TotalHits::value).orElse(0L));
        List<FileEsModel> hits = search.hits().hits().stream().map(Hit::source).toList();
        resp.getData().addAll(hits);
    }

    /**
     * 查询
     *
     * @param queryDto
     * @return
     */
    private static Function<Query.Builder, ObjectBuilder<Query>> genQueryBuilderFunction(
            SearchBaseQueryDto queryDto) {
        return q -> q.functionScore(r -> {
            // 随机
            if (Boolean.TRUE.equals(queryDto.getRandomScore())) {
                return r.query(t -> t.matchAll(k -> k))
                        .functions(FunctionScore.of(l -> l.randomScore(m -> m)));
            } else {
                // 查询
                BoolQuery.Builder builder = genQueryBuilder(queryDto);
                return r.query(l -> l.bool(t -> builder));
            }
        });
    }

    private static BoolQuery.Builder genQueryBuilder(SearchBaseQueryDto queryDto) {
        BoolQuery.Builder builder = new BoolQuery.Builder();

        if (!StringUtils.isEmpty(queryDto.getSearch())) {
            Query searchableText = MatchQuery.of(m -> m
                    .field("searchableText")
                    .query(queryDto.getSearch())
                    .fuzziness("auto")
                    .analyzer("ik_smart")
            )._toQuery();
            Query searchableTextPhrase = MatchPhraseQuery.of(m -> m
                    .field("searchableText")
                    .query(queryDto.getSearch())
                    .slop(50)
                    .boost(100.F)
                    .analyzer("ik_smart")
            )._toQuery();
            Query resName = MatchQuery.of(m -> m
                    .field("resName")
                    .query(queryDto.getSearch())
                    .fuzziness("auto")
                    .analyzer("ik_smart")
            )._toQuery();
            Query resNamePhrase = MatchPhraseQuery.of(m -> m
                    .field("resName")
                    .query(queryDto.getSearch())
                    .slop(10)
                    .boost(500.F)
                    .analyzer("ik_smart")
            )._toQuery();
            BoolQuery.Builder orSearch = new BoolQuery.Builder();
            orSearch.should(searchableText, searchableTextPhrase, resName, resNamePhrase);
            builder.must(l -> l.bool(orSearch.build()));
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
        return builder;
    }

    /**
     * 排序
     *
     * @param order
     * @return
     */
    private static List<SortOptions> genSortList(SearchBaseQueryDto.QueryOrderDto order) {
        return Objects.nonNull(order)
                ? Collections.singletonList(SortOptions.of(l -> l.field(m -> m.field(order.getField()).order(order.isAscOrder() ? SortOrder.Asc : SortOrder.Desc))))
                : Collections.emptyList();
    }

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
                        .index(getIndexName()).mappings(d -> d.properties(FileEsModel.generateEsMapping()))
                        .settings(s ->
                                s.analysis(k ->
                                        // 自定义路径分词工具
                                        k.analyzer("path_tokenizer", a ->
                                                a.custom(l -> l.tokenizer("path_hierarchy"))))));
                log.info("CreateIndexResponse delete: {}", response.acknowledged());
                // 持久化配置
                searchConfig.getRuntime().setInitIndex(true);
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
     *
     * @return
     */
    private String getIndexName() {
        return searchConfig.getBase().getFtsIndexName();
    }

    public boolean upsertData(FileEsModel model) {
        try {
            // 如果没有初始化索引，则创建。
            if (!searchConfig.getRuntime().isInitIndex()) {
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
            log.info("deleteByResId {}", resId);
            return result;
        } catch (IOException ex) {
            log.error("deleteByResId err", ex);
            return false;
        }
    }

    public void forceMerge() {
        ForcemergeRequest forcemergeRequest = ForcemergeRequest
                .of(t -> t.onlyExpungeDeletes(true));
        try {
            log.info("run forceMerge");
            config.getClient().indices().forcemerge(forcemergeRequest);
        } catch (IOException e) {
            log.error("forceMerge error.", e);
        }
    }

    public FileEsModel findByResId(String resId) {
        try {
            GetResponse<FileEsModel> response = config.getClient().get(t -> t.index(getIndexName()).id(resId),
                    FileEsModel.class);
            return response.source();
        } catch (Exception ex) {
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(ex);
        }
    }

    public List<FileEsModel> findByResHash(String resHash) {
        try {
            Query query = TermQuery.of(m -> m.field("resHash").value(resHash))._toQuery();
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(getIndexName())
                    .query(q -> q.bool(t -> t.must(query)))
                    .source(l -> l.filter(m -> m.excludes("searchableText")))
            );
            SearchResponse<FileEsModel> search = config.getClient().search(searchRequest, FileEsModel.class);
            List<Hit<FileEsModel>> hits = search.hits().hits();

            List<FileEsModel> resp = new ArrayList<>();
            for (Hit<FileEsModel> hit : hits) {
                resp.add(hit.source());
            }
            return resp;
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
            SearchResponse<FileEsModel> search = config.getClient().search(s -> s
                            .index(getIndexName())
                            .query(q -> q.bool(t -> builder))
                            .source(l -> l.filter(m -> m.excludes("searchableText")))
                            .size(100),
                    FileEsModel.class);

            parseSearchResult(resp, search);
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
            SearchResponse<FileEsModel> search = config.getClient().search(searchRequest, FileEsModel.class);

            List<Hit<FileEsModel>> hits = search.hits().hits();

            for (Hit<FileEsModel> hit : hits) {
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
        if (!searchConfig.getRuntime().isInitIndex()) {
            createIndex(false);
        }
        // 执行搜索
        SearchResultDto resp = new SearchResultDto();
        resp.setData(new ArrayList<>());

        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s.index(getIndexName())
                    .query(genQueryBuilderFunction(queryDto))
                    .source(l -> l.filter(m -> m.excludes("searchableText")))
                    .sort(genSortList(queryDto.getOrder()))
                    .size(queryDto.getLimit())
                    .from(queryDto.getOffset()));
            log.info("search:{}", searchRequest.toString());
            SearchResponse<FileEsModel> search = config.getClient().search(searchRequest, FileEsModel.class);
            parseSearchResult(resp, search);
        } catch (ElasticsearchException ex) {
            if (ex.getMessage().contains("index_not_found_exception")) {
                createIndex(true);
            }
            log.error("search err", ex);
        } catch (IOException ex) {
            log.error("search err", ex);
        }
        return resp;
    }

}

package noogel.xyz.search.infrastructure.dao.elastic;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
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
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.dto.ResourceHighlightHitsDto;
import noogel.xyz.search.infrastructure.dto.SearchQueryDto;
import noogel.xyz.search.infrastructure.dto.SearchResultDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.model.FileEsModel;
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
@Deprecated
public class ElasticDao {

    @Resource
    private ElasticsearchClient elasticsearchClient;
    @Resource
    private volatile ConfigProperties configProperties;

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
            SearchQueryDto queryDto) {
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

    private static BoolQuery.Builder genQueryBuilder(SearchQueryDto queryDto) {
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
            Query resSize = ElasticSearchQueryHelper.buildRangeQuery("resSize", queryDto.getResSize(), Double::valueOf);
            builder.must(resSize);
        }
        if (!StringUtils.isEmpty(queryDto.getModifiedAt())) {
            Function<String, Double> fn = (t) -> Double.valueOf(Instant.now().getEpochSecond() - Long.parseLong(t));
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
    private static List<SortOptions> genSortList(SearchQueryDto.QueryOrderDto order) {
        return Objects.nonNull(order)
                ? Collections.singletonList(SortOptions.of(l -> l.field(m -> m.field(order.getField()).order(order.isAscOrder() ? SortOrder.Asc : SortOrder.Desc))))
                : Collections.emptyList();
    }

    /**
     * 获取索引名称
     *
     * @return
     */
    private String getIndexName() {
        return configProperties.getBase().getFtsIndexName();
    }

    public FileEsModel findByResId(String resId) {
        try {
            GetResponse<FileEsModel> response = elasticsearchClient.get(t -> t.index(getIndexName()).id(resId),
                    FileEsModel.class);
            return response.source();
        } catch (Exception ex) {
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(ex);
        }
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
                    .source(l -> l.filter(m -> m.excludes(StringUtils.isBlank(text) ? Collections.emptyList() : Collections.singletonList("searchableText"))))
                    .highlight(highlight)
            );
            log.info("search:{}", searchRequest.toString());
            SearchResponse<FileEsModel> search = elasticsearchClient.search(searchRequest, FileEsModel.class);

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

    public SearchResultDto search(SearchQueryDto queryDto) {
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
            SearchResponse<FileEsModel> search = elasticsearchClient.search(searchRequest, FileEsModel.class);
            parseSearchResult(resp, search);
        } catch (IOException ex) {
            log.error("search err", ex);
        }
        return resp;
    }

}

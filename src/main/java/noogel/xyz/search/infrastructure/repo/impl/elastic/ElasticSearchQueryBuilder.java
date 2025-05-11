package noogel.xyz.search.infrastructure.repo.impl.elastic;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.client.ElasticClient;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.dto.SearchResultDto;
import noogel.xyz.search.infrastructure.dto.repo.CommonSearchDto;
import noogel.xyz.search.infrastructure.dto.repo.CommonSearchDto.CompareEnum;
import noogel.xyz.search.infrastructure.dto.repo.CommonSearchDto.Field;
import noogel.xyz.search.infrastructure.model.FullTextSearchModel;

/**
 * Elasticsearch查询构建器
 * 负责构建高效的搜索查询并处理结果
 */
@Component
@Slf4j
public class ElasticSearchQueryBuilder {

    private final ElasticClient elasticClient;
    private final ConfigProperties searchConfig;

    // 文档大小阈值常量（字节数）
    private static final double LARGE_DOC_SIZE = 500000.0; // 500KB
    private static final double SMALL_DOC_SIZE = 50000.0;   // 50KB

    public ElasticSearchQueryBuilder(ElasticClient elasticClient, ConfigProperties searchConfig) {
        this.elasticClient = elasticClient;
        this.searchConfig = searchConfig;
    }

    /**
     * 构建文本搜索查询
     * 优化查询结构，降低复杂度
     * 
     * @param searchQuery 搜索查询字符串
     * @return 优化后的查询构建器
     */
    public Query buildTextSearchQuery(String searchQuery) {
        if (StringUtils.isEmpty(searchQuery)) {
            return null;
        }
        
        // 预处理查询字符串
        String cleanQuery = searchQuery.trim().replaceAll("\\s+", " ");
        
        // 创建主查询结构
        BoolQuery.Builder mainQuery = new BoolQuery.Builder();
        
        // 1. 精确短语匹配 - 高权重但不强制要求
        mainQuery.should(
            MatchPhraseQuery.of(m -> m
                .field("content")
                .query(cleanQuery)
                .slop(0)
                .boost(10.0f))
            ._toQuery()
        );
        
        // 2. 近似短语匹配 - 中等权重
        mainQuery.should(
            MatchPhraseQuery.of(m -> m
                .field("content")
                .query(cleanQuery)
                .slop(3)
                .boost(5.0f))
            ._toQuery()
        );
        
        // 3. 多字段匹配 - 确保基本召回率
        mainQuery.should(
            MultiMatchQuery.of(m -> m
                .query(cleanQuery)
                .type(TextQueryType.BestFields)
                .operator(Operator.Or)
                .fields("content^1", "resName^8", "resTitle^5")
                .minimumShouldMatch("50%")
                .boost(3.0f))
            ._toQuery()
        );
        
        // 4. 处理长查询的分段匹配
        if (cleanQuery.length() > 10) {
            String[] parts = splitQuery(cleanQuery);
            for (String part : parts) {
                if (part.length() > 3) { // 只处理有意义的片段
                    float partBoost = 2.0f + Math.min(part.length() / 2.0f, 3.0f); // 动态权重（2.0-5.0）
                    mainQuery.should(
                        MatchPhraseQuery.of(m -> m
                            .field("content")
                            .query(part)
                            .slop(1)
                            .boost(partBoost))
                        ._toQuery()
                    );
                }
            }
        }
        
        // 5. 文档大小感知（轻量级实现）
        // 大文档需要更精确的匹配
        BoolQuery.Builder largeDocQuery = new BoolQuery.Builder();
        largeDocQuery.filter(RangeQuery.of(r -> r.number(n -> n.field("contentSize").gt(LARGE_DOC_SIZE)))._toQuery());
        largeDocQuery.must(MatchPhraseQuery.of(m -> m.field("content").query(cleanQuery).slop(5))._toQuery());
        largeDocQuery.boost(0.8f); // 轻微降低大文档权重
        
        // 小文档可以更宽松匹配
        BoolQuery.Builder smallDocQuery = new BoolQuery.Builder();
        smallDocQuery.filter(RangeQuery.of(r -> r.number(n -> n.field("contentSize").lt(SMALL_DOC_SIZE)))._toQuery());
        smallDocQuery.boost(1.2f); // 轻微提升小文档权重
        
        // 添加文档大小感知查询
        mainQuery.should(largeDocQuery.build()._toQuery());
        mainQuery.should(smallDocQuery.build()._toQuery());
        
        // 确保至少匹配一个条件
        mainQuery.minimumShouldMatch("1");
        
        return mainQuery.build()._toQuery();
    }
    
    /**
     * 智能分割搜索查询
     * 
     * @param query 原始查询
     * @return 分割后的有意义片段
     */
    private String[] splitQuery(String query) {
        return query.split("[,，.。;；!！?？:：\"'()（）\\[\\]【】{}《》<>\\s]+");
    }

    /**
     * 执行通用搜索
     * 优化搜索逻辑和结果处理
     * 
     * @param searchDto 搜索参数
     * @return 搜索结果
     */
    public SearchResultDto commonSearch(CommonSearchDto searchDto) {
        SearchResultDto result = new SearchResultDto();
        result.setData(new ArrayList<>());

        try {
            // 构建主查询
            BoolQuery.Builder mainQuery = new BoolQuery.Builder();
            
            // 1. 添加文本搜索条件
            Query textQuery = buildTextSearchQuery(searchDto.getSearchQuery());
            if (textQuery != null) {
                mainQuery.must(textQuery);
            }
            
            // 2. 添加资源类型过滤
            if (CollectionUtils.isNotEmpty(searchDto.getResTypeList())) {
                BoolQuery.Builder typeQuery = new BoolQuery.Builder();
                for (String resType : searchDto.getResTypeList()) {
                    typeQuery.should(TermQuery.of(t -> t.field("resType").value(resType))._toQuery());
                }
                mainQuery.must(typeQuery.build()._toQuery());
            }
            
            // 3. 添加目录前缀过滤
            if (StringUtils.isNotEmpty(searchDto.getDirPrefix())) {
                mainQuery.must(TermQuery.of(t -> t.field("resDir").value(searchDto.getDirPrefix()))._toQuery());
            }
            
            // 4. 添加资源大小过滤
            Field resSizeField = searchDto.getResSize();
            if (resSizeField != null) {
                Query sizeQuery = buildRangeQuery("resSize", resSizeField.getValue(), resSizeField.getCompare());
                if (sizeQuery != null) {
                    mainQuery.must(sizeQuery);
                }
            }
            
            // 5. 添加修改时间过滤
            Field modifiedAtField = searchDto.getModifiedAt();
            if (modifiedAtField != null) {
                long timeValue = Instant.now().getEpochSecond() - Long.parseLong(modifiedAtField.getValue());
                Query timeQuery = buildRangeQuery("modifiedAt", String.valueOf(timeValue), modifiedAtField.getCompare());
                if (timeQuery != null) {
                    mainQuery.must(timeQuery);
                }
            }
            
            // 获取分页参数
            CommonSearchDto.Paging paging = searchDto.getPaging();
            int limit = paging.getLimit();
            int offset = paging.getOffset();
            
            // 构建排序参数
            List<SortOptions> sortOptions = buildOrderByQuery(searchDto.getOrder());
            
            // 构建搜索请求
            SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                .index(getIndexName())
                .query(mainQuery.build()._toQuery())
                .from(offset)
                .size(limit)
                .source(SourceConfig.of(sc -> sc.filter(f -> f.excludes("content"))))
                .trackTotalHits(t -> t.enabled(true))
                .timeout("5s");
            
            // 添加排序
            if (!sortOptions.isEmpty()) {
                requestBuilder.sort(sortOptions);
            }
            
            // 添加资源类型聚合（用于结果多样性）
            boolean useTypeAggregation = CollectionUtils.isEmpty(searchDto.getResTypeList()) && 
                                         limit > 5 && textQuery != null;
            
            if (useTypeAggregation) {
                requestBuilder.aggregations("resTypes", 
                    Aggregation.of(a -> a.terms(t -> t.field("resType").size(10)))
                );
            }
            
            // 执行搜索
            if (log.isDebugEnabled()) {
                log.debug("Search query: {}", mainQuery.build().toString());
            }
            
            var searchResponse = elasticClient.getClient().search(
                requestBuilder.build(), 
                FullTextSearchModel.class
            );
            
            // 处理结果总数
            TotalHits total = searchResponse.hits().total();
            result.setSize(total != null ? total.value() : 0);
            
            // 获取匹配文档
            List<Hit<FullTextSearchModel>> hits = searchResponse.hits().hits();
            
            // 基本结果处理
            if (!useTypeAggregation || hits.size() <= 5) {
                // 简单模式：直接返回排序后的结果
                for (Hit<FullTextSearchModel> hit : hits) {
                    if (hit.source() != null) {
                        result.getData().add(hit.source());
                    }
                }
            } else {
                // 高级模式：应用结果多样性处理
                result.setData(applyResultDiversity(hits));
            }
            
        } catch (IOException ex) {
            log.error("搜索执行异常", ex);
        } catch (ElasticsearchException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("index_not_found_exception")) {
                log.error("索引不存在，请先创建索引");
            } else {
                log.error("Elasticsearch异常", ex);
            }
        } catch (Exception ex) {
            log.error("搜索处理异常", ex);
        }
        
        return result;
    }
    
    /**
     * 应用结果多样性算法
     * 确保不同类型的文档都有展示机会
     * 
     * @param hits 原始搜索结果
     * @return 优化后的结果列表
     */
    private List<FullTextSearchModel> applyResultDiversity(List<Hit<FullTextSearchModel>> hits) {
        if (hits.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<FullTextSearchModel> result = new ArrayList<>();
        Map<String, List<FullTextSearchModel>> typeGroups = new HashMap<>();
        
        // 按类型分组
        for (Hit<FullTextSearchModel> hit : hits) {
            FullTextSearchModel model = hit.source();
            if (model == null) continue;
            
            String type = model.getResType();
            if (!typeGroups.containsKey(type)) {
                typeGroups.put(type, new ArrayList<>());
            }
            typeGroups.get(type).add(model);
        }
        
        // 确保至少添加最相关结果
        if (!hits.isEmpty() && hits.get(0).source() != null) {
            result.add(hits.get(0).source());
        }
        
        // 轮询各类型添加结果
        int maxRounds = 5; // 最多轮询5轮
        int maxPerType = 3; // 每种类型最多3个结果
        
        for (int round = 0; round < maxRounds; round++) {
            boolean addedAny = false;
            
            for (Map.Entry<String, List<FullTextSearchModel>> entry : typeGroups.entrySet()) {
                List<FullTextSearchModel> typeHits = entry.getValue();
                
                // 计算当前类型已添加数量
                long typeCount = result.stream()
                    .filter(m -> m.getResType().equals(entry.getKey()))
                    .count();
                
                // 如果未达到上限且有可添加结果
                if (typeCount < maxPerType && typeHits.size() > typeCount) {
                    FullTextSearchModel modelToAdd = typeHits.get((int)typeCount);
                    
                    // 避免重复添加
                    if (result.stream().noneMatch(m -> m.getResId().equals(modelToAdd.getResId()))) {
                        result.add(modelToAdd);
                        addedAny = true;
                    }
                }
            }
            
            // 如果无法添加更多结果，退出循环
            if (!addedAny) {
                break;
            }
        }
        
        // 如果结果不足，添加剩余结果
        if (result.size() < hits.size()) {
            for (Hit<FullTextSearchModel> hit : hits) {
                FullTextSearchModel model = hit.source();
                if (model == null) continue;
                
                // 避免重复添加
                if (result.stream().noneMatch(m -> m.getResId().equals(model.getResId()))) {
                    result.add(model);
                    if (result.size() >= hits.size()) {
                        break;
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * 构建范围查询
     * 优化数值处理
     * 
     * @param field 字段名
     * @param value 比较值
     * @param compareOp 比较操作
     * @return 范围查询
     */
    @Nullable
    public Query buildRangeQuery(String field, String value, CompareEnum compareOp) {
        if (StringUtils.isEmpty(value) || compareOp == null) {
            return null;
        }
        
        try {
            double numericValue = Double.parseDouble(value);
            
            // 针对不同字段应用不同的乘数因子
            double multiplier = 1.0;
            if ("resSize".equals(field) || "contentSize".equals(field) || "modifiedAt".equals(field)) {
                multiplier = 1000.0; // 转换为毫秒/字节
            }
            
            numericValue *= multiplier;

            final double finalNumericValue = numericValue;
            
            if (CompareEnum.GT.equals(compareOp)) {
                return RangeQuery.of(r -> r.number(n -> n.field(field).gt(finalNumericValue)))._toQuery();
            } else if (CompareEnum.LT.equals(compareOp)) {
                return RangeQuery.of(r -> r.number(n -> n.field(field).lt(finalNumericValue)))._toQuery();
            }
            return null;
        } catch (NumberFormatException e) {
            log.warn("范围查询值格式错误: field={}, value={}", field, value);
            return null;
        }
    }

    /**
     * 构建排序选项
     * 
     * @param orderBy 排序参数
     * @return 排序选项
     */
    private List<SortOptions> buildOrderByQuery(CommonSearchDto.OrderBy orderBy) {
        if (Objects.isNull(orderBy) || StringUtils.isEmpty(orderBy.getField())) {
            return Collections.emptyList();
        }
        
        SortOrder sortOrder = orderBy.isAsc() ? SortOrder.Asc : SortOrder.Desc;
        var sortOption = SortOptions.of(s -> s.field(f -> f.field(orderBy.getField()).order(sortOrder)));
        
        return Collections.singletonList(sortOption);
    }

    /**
     * 获取索引名称
     * 
     * @return 索引名称
     */
    private String getIndexName() {
        return searchConfig.getRuntime().getFtsIndexName();
    }

    /**
     * 构建复合查询
     * 为保持向后兼容，保留此方法
     * 
     * @param searchQuery 搜索查询字符串
     * @return 复合查询构建器
     */
    public BoolQuery.Builder getComplexQuery(String searchQuery) {
        // 调用优化后的方法获取查询
        Query optimizedQuery = buildTextSearchQuery(searchQuery);
        
        // 转换为BoolQuery.Builder返回
        BoolQuery.Builder builder = new BoolQuery.Builder();
        builder.must(optimizedQuery);
        
        return builder;
    }
}
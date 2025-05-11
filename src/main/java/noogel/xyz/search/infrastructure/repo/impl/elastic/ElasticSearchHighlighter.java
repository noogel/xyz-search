package noogel.xyz.search.infrastructure.repo.impl.elastic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.BoundaryScanner;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.HighlighterEncoder;
import co.elastic.clients.elasticsearch.core.search.HighlighterFragmenter;
import co.elastic.clients.elasticsearch.core.search.HighlighterOrder;
import co.elastic.clients.elasticsearch.core.search.HighlighterTagsSchema;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.client.ElasticClient;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.dto.ResourceHighlightHitsDto;
import noogel.xyz.search.infrastructure.model.FullTextSearchModel;

/**
 * Elasticsearch高亮处理工具
 * 负责构建和执行高亮相关操作
 */
@Component
@Slf4j
public class ElasticSearchHighlighter {
    
    private final ElasticClient elasticClient;
    private final ConfigProperties searchConfig;
    private final ElasticSearchQueryBuilder queryBuilder;
    
    public ElasticSearchHighlighter(ElasticClient elasticClient, ConfigProperties searchConfig, 
                                  ElasticSearchQueryBuilder queryBuilder) {
        this.elasticClient = elasticClient;
        this.searchConfig = searchConfig;
        this.queryBuilder = queryBuilder;
    }
    
    /**
     * 根据资源ID和搜索文本搜索并高亮
     * 
     * @param resId 资源ID
     * @param text 搜索文本
     * @return 包含资源和高亮结果的DTO
     */
    @Nullable
    public ResourceHighlightHitsDto searchByResId(String resId, @Nullable String text) {
        try {
            Query searchQuery = createHighlightQuery(resId, text);

            // 构建高级高亮配置
            Highlight highlight = buildHighlightConfig(searchQuery);

            // 创建搜索请求
            SearchRequest searchRequest = SearchRequest
                    .of(s -> s.index(getIndexName())
                            .query(searchQuery)
                            .highlight(highlight)
                            // 使用全量源字段，确保所有字段都可用于高亮
                            .source(SourceConfig.of(sc -> sc.filter(f -> f.includes("*"))))
                            // 增加高亮超时设置，避免复杂查询超时
                            .timeout("10s"));

            log.info("highlight search:{}", searchRequest.toString());
            SearchResponse<FullTextSearchModel> search = elasticClient.getClient().search(searchRequest,
                    FullTextSearchModel.class);

            List<Hit<FullTextSearchModel>> hits = search.hits().hits();

            for (Hit<FullTextSearchModel> hit : hits) {
                ResourceHighlightHitsDto dto = new ResourceHighlightHitsDto();
                dto.setResource(hit.source());

                // 合并内容和标题的高亮结果
                List<String> allHighlights = mergeHighlights(hit);
                dto.setHighlights(allHighlights);
                return dto;
            }
        } catch (IOException ex) {
            log.error("highlight searchByResId error", ex);
        }
        return null;
    }
    
    /**
     * 构建高亮配置
     * 使用Fast Vector Highlighter替代Unified Highlighter，提高短语匹配高亮效果
     */
    private Highlight buildHighlightConfig(Query searchQuery) {
        return Highlight.of(t -> t
                // 高亮内容字段
                .fields("content", HighlightField.of(k -> k
                        .type("fvh") // 使用Fast Vector Highlighter，更适合精确短语匹配和复杂查询
                        .fragmentSize(300) // 增加片段大小，更全面展示上下文
                        .numberOfFragments(25) // 增加片段数量，提供更多匹配结果
                        .fragmentOffset(100) // 增加片段偏移量，获取更多上下文
                        .preTags("<em>") // 高亮开始标签
                        .postTags("</em>") // 高亮结束标签
                        .fragmenter(HighlighterFragmenter.Span) // 使用Span分段器，更适合短语匹配
                        .order(HighlighterOrder.Score) // 按相关性排序片段
                        .noMatchSize(150) // 如果没有匹配，返回的文本大小
                        .requireFieldMatch(false) // 不要求字段匹配，可以匹配多个字段
                        .phraseLimit(50) // 限制检查的短语数量，提高性能
                        .boundaryScanner(BoundaryScanner.Sentence) // 使用句子作为边界，更自然
                        .boundaryScannerLocale("zh_CN") // 支持中文边界识别
                        .highlightQuery(searchQuery) // 使用与主查询相同的查询来高亮
                ))
                // 标题字段高亮优化
                .fields("resTitle", HighlightField.of(k -> k
                        .type("fvh") // Fast Vector Highlighter
                        .preTags("<em>")
                        .postTags("</em>")
                        .numberOfFragments(0) // 0表示不分片，返回完整字段
                        .requireFieldMatch(true) // 标题需精确匹配
                        .forceSource(true) // 强制使用源字段，提高准确性
                        .boundaryScanner(BoundaryScanner.Sentence) // 使用句子边界
                        .boundaryScannerLocale("zh_CN") // 中文支持
                ))
                // 文件名高亮优化
                .fields("resName", HighlightField.of(k -> k
                        .type("fvh") // Fast Vector Highlighter
                        .preTags("<em>")
                        .postTags("</em>")
                        .numberOfFragments(0) // 0表示不分片，返回完整字段
                        .requireFieldMatch(true) // 文件名需精确匹配
                        .forceSource(true) // 强制使用源字段，提高准确性
                ))
                // 全局高亮设置
                .encoder(HighlighterEncoder.Html) // HTML编码高亮片段，防止XSS
                .tagsSchema(HighlighterTagsSchema.Styled) // 使用预定义的标签样式
                .maxAnalyzedOffset(10000000) // 增加分析的最大偏移量，适应大型文档
        );
    }
    
    /**
     * 合并不同字段的高亮结果，并优化结果排序
     */
    private List<String> mergeHighlights(Hit<FullTextSearchModel> hit) {
        // 合并内容和标题的高亮结果
        List<String> contentHighlights = hit.highlight().get("content");
        List<String> titleHighlights = hit.highlight().get("resTitle");
        List<String> nameHighlights = hit.highlight().get("resName");

        List<String> allHighlights = new ArrayList<>();
        
        // 优先添加标题和文件名高亮，它们通常更重要
        if (titleHighlights != null && !titleHighlights.isEmpty()) {
            allHighlights.addAll(titleHighlights);
        }
        
        if (nameHighlights != null && !nameHighlights.isEmpty()) {
            allHighlights.addAll(nameHighlights);
        }
        
        // 然后添加内容高亮
        if (contentHighlights != null && !contentHighlights.isEmpty()) {
            allHighlights.addAll(contentHighlights);
        }
        
        return allHighlights;
    }
    
    /**
     * 创建优化的高亮查询
     */
    private Query createHighlightQuery(String resId, String text) {
        BoolQuery.Builder builder = new BoolQuery.Builder();
        
        // 添加文本搜索条件
        if (!StringUtils.isEmpty(text)) {
            // 使用优化的复合查询构建器
            BoolQuery.Builder textQuery = queryBuilder.getComplexQuery(text);
            builder.should(textQuery.build()._toQuery());
        }
        
        // 添加资源ID精确匹配条件
        Query idQuery = TermQuery.of(m -> m.field("resId").value(resId))._toQuery();
        builder.must(idQuery);
        
        return builder.build()._toQuery();
    }
    
    /**
     * 获取索引名称
     */
    private String getIndexName() {
        return searchConfig.getRuntime().getFtsIndexName();
    }
} 
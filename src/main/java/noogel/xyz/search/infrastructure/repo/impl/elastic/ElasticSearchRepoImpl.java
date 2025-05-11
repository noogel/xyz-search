package noogel.xyz.search.infrastructure.repo.impl.elastic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
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
    @Resource
    private ElasticSearchQueryBuilder queryBuilder;
    @Resource
    private ElasticSearchHighlighter highlighter;

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
        // 使用专门的高亮处理器进行搜索和高亮
        return highlighter.searchByResId(resId, text);
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
        // 使用查询构建器执行搜索
        return queryBuilder.commonSearch(searchDto);
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
package noogel.xyz.search.infrastructure.lucene;

import static noogel.xyz.search.infrastructure.lucene.LuceneAnalyzer.STOPWORDS;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LRUQueryCache;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryCache;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.UsageTrackingQueryCachingPolicy;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.model.lucene.FullTextSearchModel;
import noogel.xyz.search.infrastructure.utils.cache.LocalQueryCache;

@Slf4j
public class LuceneSearcher {

    private final FSDirectory directory;
    private final SearcherManager searcherManager;
    private final QueryCache queryCache;
    private final ScheduledExecutorService executor;
    private final LocalQueryCache localQueryCache;

    public LuceneSearcher(Path dir) {
        try {
            // 使用 MMapDirectory 来提升性能，如果系统支持的话
            if (MMapDirectory.UNMAP_SUPPORTED) {
                this.directory = MMapDirectory.open(dir);
            } else {
                this.directory = FSDirectory.open(dir);
            }

            // 创建 Lucene 查询缓存，优化配置
            this.queryCache = new LRUQueryCache(4000, // 增加缓存最大文档数到4000
                    256 * 1024 * 1024 // 增加缓存大小限制到256MB
            );

            // 初始化本地查询缓存，设置30秒过期时间和2000个最大缓存条目
            this.localQueryCache = new LocalQueryCache(30 * 1000, 2000);

            // 创建 DirectoryReader
            DirectoryReader reader = DirectoryReader.open(directory);

            // 创建 SearcherManager
            SearcherFactory searcherFactory = new SearcherFactory() {
                @Override
                public IndexSearcher newSearcher(IndexReader reader, IndexReader previousReader) throws IOException {
                    // 创建自定义线程池，设置合理的队列长度和拒绝策略
                    ThreadPoolExecutor executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2, // 增加核心线程数
                            Runtime.getRuntime().availableProcessors() * 4, // 增加最大线程数
                            60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2000), // 增加队列长度
                            r -> {
                                Thread thread = new Thread(r);
                                thread.setName("lucene-search-" + Thread.currentThread().getId());
                                thread.setDaemon(true);
                                return thread;
                            }, new ThreadPoolExecutor.CallerRunsPolicy());

                    IndexSearcher searcher = new IndexSearcher(reader, executor);
                    searcher.setQueryCache(queryCache);
                    searcher.setQueryCachingPolicy(new UsageTrackingQueryCachingPolicy());

                    // 优化BM25参数：调整k1和b参数以提高准确性
                    searcher.setSimilarity(new BM25Similarity(1.5f, 0.8f));

                    return searcher;
                }
            };

            this.searcherManager = new SearcherManager(reader, searcherFactory);

            // 创建定时刷新任务
            this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "lucene-refresh-thread");
                thread.setDaemon(true);
                return thread;
            });

            // 增加刷新频率到每15秒一次
            this.executor.scheduleAtFixedRate(() -> {
                try {
                    searcherManager.maybeRefresh();
                } catch (IOException e) {
                    // 忽略刷新异常
                }
            }, 0, 15, TimeUnit.SECONDS);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取 IndexSearcher
     */
    private IndexSearcher getSearcher() throws IOException {
        return searcherManager.acquire();
    }

    /**
     * 释放 IndexSearcher
     */
    private void releaseSearcher(IndexSearcher searcher) throws IOException {
        if (searcher != null) {
            searcherManager.release(searcher);
        }
    }

    /**
     * 关闭资源
     */
    public void close() {
        if (executor != null) {
            executor.shutdown();
        }
        if (searcherManager != null) {
            try {
                searcherManager.close();
            } catch (IOException e) {
                // 忽略关闭异常
            }
        }
        if (directory != null) {
            try {
                directory.close();
            } catch (IOException e) {
                // 忽略关闭异常
            }
        }
        // 清理本地缓存
        if (localQueryCache != null) {
            localQueryCache.clear();
        }
    }

    @Nullable
    public FtsDocument findFirst(Query query) {
        String cacheKey = "findFirst:" + query.toString();
        return localQueryCache.getOrCompute(cacheKey, () -> {
            IndexSearcher searcher = null;
            try {
                searcher = getSearcher();
                TopDocs topDocs = searcher.search(query, 1);
                TotalHits totalHits = topDocs.totalHits;
                if (totalHits.value < 1) {
                    return null;
                }
                int docId = topDocs.scoreDocs[0].doc;
                Document document = searcher.getIndexReader().storedFields().document(docId);
                return convert(document);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (searcher != null) {
                    try {
                        releaseSearcher(searcher);
                    } catch (IOException e) {
                        // 忽略关闭异常
                    }
                }
            }
        });
    }

    @Nullable
    public Pair<FtsDocument, List<String>> findFirstWithHighlight(Query query, HighlightOptions options) {
        IndexSearcher searcher = null;
        try {
            searcher = getSearcher();
            TopDocs topDocs = searcher.search(query, 1);
            TotalHits totalHits = topDocs.totalHits;
            if (totalHits.value < 1) {
                return null;
            }
            int docId = topDocs.scoreDocs[0].doc;
            Document document = searcher.getIndexReader().storedFields().document(docId);

            // 高亮
            Formatter formatter = new SimpleHTMLFormatter("<em>", "</em>");
            QueryScorer scorer = new QueryScorer(query);
            Highlighter highlighter = new Highlighter(formatter, scorer);
            Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, options.getFragmentSize());
            highlighter.setTextFragmenter(fragmenter);
            // 高亮文本
            try (Analyzer analyzer = new SmartChineseAnalyzer(STOPWORDS)) {
                String[] highlightedText = highlighter.getBestFragments(analyzer, "content", document.get("content"),
                        options.getMaxNumFragments());
                return Pair.of(convert(document), Arrays.asList(highlightedText));
            }
        } catch (IOException | InvalidTokenOffsetsException e) {
            throw new RuntimeException(e);
        } finally {
            if (searcher != null) {
                try {
                    releaseSearcher(searcher);
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
        }
    }

    public Pair<List<FtsDocument>, List<String>> llmSearch(Query query, Paging paging, HighlightOptions options) {
        String cacheKey = String.format("llmSearch:%s:%s:%s", query.toString(), paging.toString(), options.toString());
        return localQueryCache.getOrCompute(cacheKey, () -> {
            IndexSearcher searcher = null;
            try {
                searcher = getSearcher();
                TopDocs topDocs = searcher.search(query, paging.calculateNextOffset());

                // 高亮
                Formatter formatter = new SimpleHTMLFormatter(options.getPreTag(), options.getPostTag());
                QueryScorer scorer = new QueryScorer(query);
                Highlighter highlighter = new Highlighter(formatter, scorer);
                Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, options.getFragmentSize());
                highlighter.setTextFragmenter(fragmenter);

                List<List<TextFragment>> textFragmentList = new ArrayList<>();
                List<FtsDocument> documents = new ArrayList<>();
                for (ScoreDoc scoreDoc : List.of(topDocs.scoreDocs).subList(paging.calculateOffset(),
                        paging.calculateNextOffset(Math.toIntExact(topDocs.totalHits.value)))) {
                    int docId = scoreDoc.doc;
                    Document document = searcher.getIndexReader().storedFields().document(docId);
                    try (Analyzer analyzer = new SmartChineseAnalyzer(STOPWORDS)) {
                        // 高亮文本
                        TokenStream tokenStream = analyzer.tokenStream("content", document.get("content"));
                        TextFragment[] highlightedText = highlighter.getBestTextFragments(tokenStream,
                                document.get("content"), true, options.getMaxNumFragments());
                        textFragmentList.add(Arrays.stream(highlightedText).toList());
                    }
                }
                if (CollectionUtils.isEmpty(textFragmentList)) {
                    return Pair.of(documents, Collections.emptyList());
                }
                // 排序，切割
                List<String> fragments = textFragmentList.stream().flatMap(Collection::stream)
                        .filter(l -> l.getScore() > 100.0)
                        .sorted((l, r) -> (r.getScore() + r.getFragNum()) > (l.getScore() + r.getFragNum()) ? 1 : -1)
                        .map(TextFragment::toString).toList();
                fragments = fragments.subList(0, Math.min(options.getMaxNumFragments(), fragments.size()));
                return Pair.of(documents, fragments);

            } catch (IOException | InvalidTokenOffsetsException e) {
                throw new RuntimeException(e);
            } finally {
                if (searcher != null) {
                    try {
                        releaseSearcher(searcher);
                    } catch (IOException e) {
                        // 忽略关闭异常
                    }
                }
            }
        });
    }

    public Pair<Integer, List<FtsDocument>> pagingSearch(Query query, Paging paging, @Nullable OrderBy order) {
        String cacheKey = String.format("pagingSearch:%s:%s:%s", query.toString(), paging.toString(),
                order != null ? order.toString() : "null");
        return localQueryCache.getOrCompute(cacheKey, () -> {
            IndexSearcher searcher = null;
            try {
                searcher = getSearcher();
                // 获取最大匹配条数
                int count = searcher.count(query);
                // 排序
                Sort sort = null;
                if (Objects.nonNull(order)) {
                    String sortedName = String.format("%s_sorted", order.getField());
                    sort = new Sort(new SortField(sortedName, order.getType(), !order.isAsc()));
                }
                // 获取当前页数据
                TopDocs topDocs = Objects.nonNull(sort) ? searcher.search(query, paging.calculateNextOffset(), sort)
                        : searcher.search(query, paging.calculateNextOffset());
                List<FtsDocument> documents = new ArrayList<>();
                for (ScoreDoc scoreDoc : List.of(topDocs.scoreDocs).subList(paging.calculateOffset(),
                        paging.calculateNextOffset(Math.toIntExact(topDocs.totalHits.value)))) {
                    int docId = scoreDoc.doc;
                    Document document = searcher.getIndexReader().storedFields().document(docId);
                    documents.add(convert(document));
                }

                return Pair.of(count, documents);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (searcher != null) {
                    try {
                        releaseSearcher(searcher);
                    } catch (IOException e) {
                        // 忽略关闭异常
                    }
                }
            }
        });
    }

    /**
     * 使用 searchAfter 进行高效分页查询
     * 
     * @param query    查询条件
     * @param pageSize 每页大小
     * @param lastDoc  上一页最后一个文档的 ScoreDoc，首页传 null
     * @param order    排序条件
     * @return 分页结果
     */
    public Pair<List<FtsDocument>, ScoreDoc> searchAfter(Query query, int pageSize, @Nullable ScoreDoc lastDoc,
            @Nullable OrderBy order) {
        String cacheKey = String.format("searchAfter:%s:%d:%s:%s", query.toString(), pageSize,
                lastDoc != null ? lastDoc.toString() : "null", order != null ? order.toString() : "null");
        return localQueryCache.getOrCompute(cacheKey, () -> {
            IndexSearcher searcher = null;
            try {
                searcher = getSearcher();
                // 排序
                Sort sort = null;
                if (Objects.nonNull(order)) {
                    String sortedName = String.format("%s_sorted", order.getField());
                    sort = new Sort(new SortField(sortedName, order.getType(), !order.isAsc()));
                } else {
                    // 如果没有指定排序，默认按照文档得分排序
                    sort = new Sort(SortField.FIELD_SCORE);
                }

                // 使用 searchAfter 进行分页
                TopDocs topDocs = lastDoc == null ? searcher.search(query, pageSize, sort)
                        : searcher.searchAfter(lastDoc, query, pageSize, sort);

                List<FtsDocument> documents = new ArrayList<>();
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document document = searcher.getIndexReader().storedFields().document(scoreDoc.doc);
                    documents.add(convert(document));
                }

                // 返回文档列表和最后一个文档的 ScoreDoc（用于下一页查询）
                ScoreDoc lastScoreDoc = topDocs.scoreDocs.length > 0 ? topDocs.scoreDocs[topDocs.scoreDocs.length - 1]
                        : null;
                return Pair.of(documents, lastScoreDoc);

            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (searcher != null) {
                    try {
                        releaseSearcher(searcher);
                    } catch (IOException e) {
                        // 忽略关闭异常
                    }
                }
            }
        });
    }

    public FullTextSearchModel convert(Document document) {
        FullTextSearchModel model = new FullTextSearchModel();
        for (Field declaredField : model.getClass().getDeclaredFields()) {
            String name = declaredField.getName();
            String val = document.get(name);
            if (Objects.isNull(val)) {
                continue;
            }
            Class<?> declaringClass = declaredField.getType();
            declaredField.setAccessible(true);
            try {
                if (String.class.equals(declaringClass)) {
                    declaredField.set(model, val);
                } else if (Integer.class.equals(declaringClass)) {
                    declaredField.set(model, Integer.parseInt(val));
                } else if (Long.class.equals(declaringClass)) {
                    declaredField.set(model, Long.parseLong(val));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return model;
    }

    /**
     * 预热索引
     */
    public void warmUp() {
        IndexSearcher searcher = null;
        try {
            searcher = getSearcher();
            // 预热所有字段的terms
            for (String field : Arrays.asList("content")) {
                searcher.getIndexReader().leaves().forEach(leaf -> {
                    try {
                        leaf.reader().terms(field).iterator().next();
                    } catch (IOException e) {
                        // 忽略预热异常
                    }
                });
            }

            // 预热常用查询
            Query[] warmupQueries = new Query[] { new WildcardQuery(new Term("content", "*")), };

            for (Query query : warmupQueries) {
                searcher.search(query, 1);
            }
        } catch (IOException e) {
            // 忽略预热异常
        } catch (Exception e) {
            log.error("warmUp error", e);
        } finally {
            if (searcher != null) {
                try {
                    releaseSearcher(searcher);
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
        }
    }
}

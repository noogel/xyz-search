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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
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
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.store.FSDirectory;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.Nullable;
import noogel.xyz.search.infrastructure.model.lucene.FullTextSearchModel;

public class LuceneSearcher {

    private final FSDirectory directory;
    private final SearcherManager searcherManager;
    private final QueryCache queryCache;
    private final ScheduledExecutorService executor;

    public LuceneSearcher(Path dir) {
        try {
            this.directory = FSDirectory.open(dir);

            // 创建 Lucene 查询缓存
            this.queryCache = new LRUQueryCache(1000, 512 * 1024 * 1024);

            // 创建 SearcherManager
            DirectoryReader reader = DirectoryReader.open(directory);
            this.searcherManager = new SearcherManager(reader, new SearcherFactory() {
                @Override
                public IndexSearcher newSearcher(IndexReader reader, IndexReader previousReader) throws IOException {
                    IndexSearcher searcher = new IndexSearcher(reader);
                    searcher.setQueryCache(queryCache);
                    return searcher;
                }
            });

            // 创建定时刷新任务
            this.executor = Executors.newSingleThreadScheduledExecutor();
            this.executor.scheduleAtFixedRate(() -> {
                try {
                    searcherManager.maybeRefresh();
                } catch (IOException e) {
                    // 忽略刷新异常
                }
            }, 0, 30, TimeUnit.SECONDS);

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
    }

    @Nullable
    public LuceneDocument findFirst(Query query) {
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
    }

    @Nullable
    public Pair<LuceneDocument, List<String>> findFirstWithHighlight(Query query, HighlightOptions options) {
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
                String[] highlightedText = highlighter.getBestFragments(
                        analyzer, "content", document.get("content"), options.getMaxNumFragments());
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

    public Pair<List<LuceneDocument>, List<String>> llmSearch(Query query, Paging paging, HighlightOptions options) {
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
            List<LuceneDocument> documents = new ArrayList<>();
            for (ScoreDoc scoreDoc : List.of(topDocs.scoreDocs).subList(paging.calculateOffset(),
                    paging.calculateNextOffset(Math.toIntExact(topDocs.totalHits.value)))) {
                int docId = scoreDoc.doc;
                Document document = searcher.getIndexReader().storedFields().document(docId);
                try (Analyzer analyzer = new SmartChineseAnalyzer(STOPWORDS)) {
                    // 高亮文本
                    TokenStream tokenStream = analyzer.tokenStream("content", document.get("content"));
                    TextFragment[] highlightedText = highlighter.getBestTextFragments(
                            tokenStream, document.get("content"), true, options.getMaxNumFragments());
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
    }

    public Pair<Integer, List<LuceneDocument>> pagingSearch(Query query, Paging paging, @Nullable OrderBy order) {
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
            List<LuceneDocument> documents = new ArrayList<>();
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
}

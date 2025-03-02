package noogel.xyz.search.infrastructure.lucene;

import jakarta.annotation.Nullable;
import noogel.xyz.search.infrastructure.model.lucene.FullTextSearchModel;
import noogel.xyz.search.infrastructure.utils.cache.QueryCache;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.FSDirectory;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static noogel.xyz.search.infrastructure.lucene.LuceneAnalyzer.STOPWORDS;

public class LuceneSearcher {

    private final FSDirectory directory;
    private final AtomicReference<DirectoryReader> readerRef = new AtomicReference<>();
    private final PerFieldAnalyzerWrapper wrapper;
    private final QueryCache queryCache;

    public LuceneSearcher(Path dir, PerFieldAnalyzerWrapper wrapper) {
        try {
            this.directory = FSDirectory.open(dir);
            this.wrapper = wrapper;
            // 创建查询缓存，30秒过期，最多1000条
            this.queryCache = new QueryCache(30 * 1000, 1000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取或刷新 IndexReader
     */
    private DirectoryReader getReader() throws IOException {
        DirectoryReader reader = readerRef.get();
        if (reader == null) {
            synchronized (this) {
                reader = readerRef.get();
                if (reader == null) {
                    reader = DirectoryReader.open(directory);
                    readerRef.set(reader);
                    return reader;
                }
            }
        }

        // 检查是否有新的变更，如果有则刷新 reader
        DirectoryReader newReader = DirectoryReader.openIfChanged(reader);
        if (newReader != null && newReader != reader) {
            synchronized (this) {
                DirectoryReader oldReader = readerRef.getAndSet(newReader);
                if (oldReader != null && oldReader != newReader) {
                    try {
                        oldReader.close();
                    } catch (IOException e) {
                        // 忽略关闭异常
                    }
                }
                // 清空查询缓存
                queryCache.clear();
            }
            return newReader;
        }

        return reader;
    }

    /**
     * 关闭资源
     */
    public void close() {
        DirectoryReader reader = readerRef.getAndSet(null);
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                // 忽略关闭异常
            }
        }
        queryCache.clear();
    }

    @Nullable
    public LuceneDocument findFirst(Query query) {
        String cacheKey = "findFirst:" + query.toString();

        return queryCache.getOrCompute(cacheKey, () -> {
            try {
                DirectoryReader reader = getReader();
                IndexSearcher searcher = new IndexSearcher(reader);
                // 获取最大匹配条数
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
            }
        });
    }

    @Nullable
    public Pair<LuceneDocument, List<String>> findFirstWithHighlight(Query query, HighlightOptions options) {
        String cacheKey = "findFirstWithHighlight:" + query.toString() + ":" + options.toString();

        return queryCache.getOrCompute(cacheKey, () -> {
            try {
                DirectoryReader reader = getReader();
                IndexSearcher searcher = new IndexSearcher(reader);
                // 获取最大匹配条数
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
            }
        });
    }

    public Pair<List<LuceneDocument>, List<String>> llmSearch(Query query, Paging paging, HighlightOptions options) {
        String cacheKey = "pagingSearch:" + query.toString() + ":paging" + paging.toString() + ":options" + options.toString();
        return queryCache.getOrCompute(cacheKey, () -> {
            try {
                DirectoryReader reader = getReader();
                IndexSearcher searcher = new IndexSearcher(reader);
                // 获取当前页数据
                TopDocs topDocs = searcher.search(query, paging.calculateNextOffset());

                // 高亮
                Formatter formatter = new SimpleHTMLFormatter(options.getPreTag(), options.getPostTag());
                QueryScorer scorer = new QueryScorer(query);
                Highlighter highlighter = new Highlighter(formatter, scorer);
                Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, options.getFragmentSize());
                highlighter.setTextFragmenter(fragmenter);

                List<List<TextFragment>> textFragmentList = new ArrayList<>();
                List<LuceneDocument> documents = new ArrayList<>();
                for (ScoreDoc scoreDoc : List.of(topDocs.scoreDocs).subList(paging.calculateOffset(), paging.calculateNextOffset(Math.toIntExact(topDocs.totalHits.value)))) {
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
            }
        });
    }

    public Pair<Integer, List<LuceneDocument>> pagingSearch(Query query, Paging paging, @Nullable OrderBy order) {
        // 使用查询和分页信息构建缓存键
        String cacheKey = "pagingSearch:" + query.toString() + ":offset" + paging.calculateOffset() + ":limit" + paging.calculateNextOffset();

        return queryCache.getOrCompute(cacheKey, () -> {
            try {
                DirectoryReader reader = getReader();
                IndexSearcher searcher = new IndexSearcher(reader);
                // 获取最大匹配条数
                int count = searcher.count(query);
                // 排序
                Sort sort = null;
                if (Objects.nonNull(order)) {
                    String sortedName = String.format("%s_sorted", order.getField());
                    sort = new Sort(new SortField(sortedName, order.getType(), !order.isAsc()));
                }
                // 获取当前页数据
                TopDocs topDocs = Objects.nonNull(sort) ?
                        searcher.search(query, paging.calculateNextOffset(), sort)
                        : searcher.search(query, paging.calculateNextOffset());
                List<LuceneDocument> documents = new ArrayList<>();
                for (ScoreDoc scoreDoc : List.of(topDocs.scoreDocs).subList(paging.calculateOffset(), paging.calculateNextOffset(Math.toIntExact(topDocs.totalHits.value)))) {
                    int docId = scoreDoc.doc;
                    Document document = searcher.getIndexReader().storedFields().document(docId);
                    documents.add(convert(document));
                }

                return Pair.of(count, documents);
            } catch (IOException e) {
                throw new RuntimeException(e);
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
}

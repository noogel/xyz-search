package noogel.xyz.search.infrastructure.lucene;

import jakarta.annotation.Nullable;
import noogel.xyz.search.infrastructure.model.lucene.FullTextSearchModel;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class LuceneSearcher {

    private final FSDirectory directory;

    public LuceneSearcher(Path dir, PerFieldAnalyzerWrapper wrapper) {
        try {
            directory = FSDirectory.open(dir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public LuceneDocument findFirst(Query query) {
        try (IndexReader reader = DirectoryReader.open(directory)) {
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
    }

    @Nullable
    public Pair<LuceneDocument, List<String>> findFirstWithHighlight(Query query) {
        try (IndexReader reader = DirectoryReader.open(directory)) {
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
            Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 100);
            highlighter.setTextFragmenter(fragmenter);
            // 高亮文本
            String[] highlightedText = highlighter.getBestFragments(new SmartChineseAnalyzer(), "content", document.get("content"), 10);

            return Pair.of(convert(document), Arrays.asList(highlightedText));
        } catch (IOException | InvalidTokenOffsetsException e) {
            throw new RuntimeException(e);
        }
    }

    public Pair<Integer, List<LuceneDocument>> pagingSearch(Query query, Paging paging) {
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            // 获取最大匹配条数
            int count = searcher.count(query);
            // 获取当前页数据
            TopDocs topDocs = searcher.search(query, paging.calculateNextOffset());
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

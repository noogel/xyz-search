package noogel.xyz.search.infrastructure.lucene;

import com.rometools.utils.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class LuceneSearcher {

    private final FSDirectory directory;

    public LuceneSearcher(Path dir, PerFieldAnalyzerWrapper wrapper) {
        try {
            directory = FSDirectory.open(dir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void search(String queryStr) {
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = new QueryParser("content", new SmartChineseAnalyzer()).parse(queryStr);
            TopDocs topDocs = searcher.search(query, 10); // 获取前10条结果
            //Uses HTML &lt;B&gt;&lt;/B&gt; tag to highlight the searched terms
            Formatter formatter = new SimpleHTMLFormatter("<em>", "</em>");

            //It scores text fragments by the number of unique query terms found
            //Basically the matching score in layman terms
            QueryScorer scorer = new QueryScorer(query);

            //used to markup highlighted terms found in the best sections of a text
            Highlighter highlighter = new Highlighter(formatter, scorer);

            //It breaks text up into same-size texts but does not split up spans
            Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 100);

            //set fragmenter to highlighter
            highlighter.setTextFragmenter(fragmenter);
            TotalHits totalHits = topDocs.totalHits;
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                int docId = scoreDoc.doc;
                Document document = searcher.getIndexReader().storedFields().document(docId);
                String[] highlightedText = highlighter.getBestFragments(new SmartChineseAnalyzer(), "content", document.get("content"), 10);
                System.out.println("文档 ID: " + docId);
                System.out.println("标题: " + document.get("resTitle"));
                System.out.println("内容: " + String.join(" | ", highlightedText));
                System.out.println("-------------------");
            }
            System.out.println(1);
        } catch (IOException | ParseException | InvalidTokenOffsetsException e) {
            throw new RuntimeException(e);
        }
    }


    public Pair<TotalHits, List<Document>> pagingSearch(Query query, Paging paging) {

        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            // 获取最大匹配条数
            TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
            TotalHits totalHits = topDocs.totalHits;
            System.out.println("==" + totalHits.value);
            // 获取当前页数据
            topDocs = searcher.search(query, paging.calculateNextOffset());
            List<Document> documents = new ArrayList<>();
            for (ScoreDoc scoreDoc : List.of(topDocs.scoreDocs).subList(paging.calculateOffset(), paging.calculateNextOffset(Math.toIntExact(topDocs.totalHits.value)))) {
                int docId = scoreDoc.doc;
                Document document = searcher.getIndexReader().storedFields().document(docId);
                documents.add(document);
            }
            return Pair.of(totalHits, documents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

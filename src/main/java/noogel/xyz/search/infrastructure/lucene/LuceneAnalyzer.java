package noogel.xyz.search.infrastructure.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;

import java.util.HashMap;
import java.util.Map;

public class LuceneAnalyzer {

    public static final Analyzer DEFAULT_ANALYZER = new WhitespaceAnalyzer();
    private static final Analyzer ID_ANALYZER = new Analyzer() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            return new TokenStreamComponents(new KeywordTokenizer());
        }
    };
    public static final Map<String, Analyzer> ANALYZER_MAP = generateAnalyzer();
    public static final PerFieldAnalyzerWrapper ANALYZER_WRAPPER = new PerFieldAnalyzerWrapper(DEFAULT_ANALYZER, ANALYZER_MAP);

    private static Map<String, Analyzer> generateAnalyzer() {
        Map<String, Analyzer> documentMap = new HashMap<>();
        documentMap.put("resId", ID_ANALYZER);
        // path_tokenizer
        documentMap.put("resDir", new KeywordAnalyzer());
        documentMap.put("resName", new SmartChineseAnalyzer());
        documentMap.put("resTitle", new SmartChineseAnalyzer());
        documentMap.put("resHash", new WhitespaceAnalyzer());
        documentMap.put("resType", new WhitespaceAnalyzer());
        documentMap.put("resSize", new WhitespaceAnalyzer());
        documentMap.put("modifiedAt", new WhitespaceAnalyzer());
        // 自定义权重
        documentMap.put("resRank", new WhitespaceAnalyzer());
        documentMap.put("content", new SmartChineseAnalyzer());
        documentMap.put("contentHash", new WhitespaceAnalyzer());
        documentMap.put("contentSize", new WhitespaceAnalyzer());
        return documentMap;
    }

}
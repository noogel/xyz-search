package noogel.xyz.search.infrastructure.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;

import java.util.HashMap;
import java.util.Map;

public class LuceneAnalyzer {

    public static final Analyzer DEFAULT_ANALYZER = new WhitespaceAnalyzer();
    public static final Map<String, Analyzer> ANALYZER_MAP = generateAnalyzer();
    public static final PerFieldAnalyzerWrapper ANALYZER_WRAPPER = new PerFieldAnalyzerWrapper(DEFAULT_ANALYZER, ANALYZER_MAP);

    private static Map<String, Analyzer> generateAnalyzer() {
        Map<String, Analyzer> documentMap = new HashMap<>();
        documentMap.put("resId", new KeywordAnalyzer());
        // path_tokenizer
        documentMap.put("resDir", new KeywordAnalyzer());
        documentMap.put("resName", new SmartChineseAnalyzer());
        documentMap.put("resTitle", new SmartChineseAnalyzer());
        documentMap.put("resHash", new KeywordAnalyzer());
        documentMap.put("resType", new KeywordAnalyzer());
        documentMap.put("resSize", new WhitespaceAnalyzer());
        documentMap.put("modifiedAt", new WhitespaceAnalyzer());
        // 自定义权重
        documentMap.put("resRank", new WhitespaceAnalyzer());
        documentMap.put("content", new SmartChineseAnalyzer());
        documentMap.put("contentHash", new KeywordAnalyzer());
        documentMap.put("contentSize", new WhitespaceAnalyzer());
        return documentMap;
    }
}
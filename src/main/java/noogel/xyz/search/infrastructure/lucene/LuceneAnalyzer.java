package noogel.xyz.search.infrastructure.lucene;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
    public static CharArraySet STOPWORDS;

    static {
        try {
            STOPWORDS = CharArraySet.unmodifiableSet(
                    WordlistLoader.getWordSet(
                            IOUtils.getDecodingReader(
                                    new ClassPathResource("/ai/dict/stopwords.txt").getInputStream(),
                                    StandardCharsets.UTF_8
                            ),
                            "//" // 注释标识符
                    )
            );
        } catch (Exception ex) {
            log.error("load stopWords error.", ex);
            STOPWORDS = CharArraySet.EMPTY_SET;
        }
    }

    private static Map<String, Analyzer> generateAnalyzer() {
        Map<String, Analyzer> documentMap = new HashMap<>();
        documentMap.put("resId", ID_ANALYZER);
        // path_tokenizer
        documentMap.put("resDir", new KeywordAnalyzer());
        documentMap.put("resName", new SmartChineseAnalyzer(STOPWORDS));
        documentMap.put("resTitle", new SmartChineseAnalyzer(STOPWORDS));
        documentMap.put("resHash", new WhitespaceAnalyzer());
        documentMap.put("resType", new WhitespaceAnalyzer());
        documentMap.put("resSize", new WhitespaceAnalyzer());
        documentMap.put("modifiedAt", new WhitespaceAnalyzer());
        // 自定义权重
        documentMap.put("resRank", new WhitespaceAnalyzer());
        documentMap.put("content", new SmartChineseAnalyzer(STOPWORDS));
        documentMap.put("contentHash", new WhitespaceAnalyzer());
        documentMap.put("contentSize", new WhitespaceAnalyzer());
        return documentMap;
    }

}
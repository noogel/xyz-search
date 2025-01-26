package demo;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.StringReader;

public class SmartChineseAnalyzerExample {
    public static void main(String[] args) throws Exception {
        String text = "今天是个好日子，心情很好天气也好。";

        try (Analyzer analyzer = new SmartChineseAnalyzer();
             TokenStream tokenStream = analyzer.tokenStream("field", new StringReader(text))) {

            CharTermAttribute charTermAttr = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();

            while (tokenStream.incrementToken()) {
                System.out.println(charTermAttr.toString());
            }

            tokenStream.end();
        }
    }
}

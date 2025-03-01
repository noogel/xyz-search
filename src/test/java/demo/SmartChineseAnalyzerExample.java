package demo;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static noogel.xyz.search.infrastructure.lucene.LuceneAnalyzer.STOPWORDS;

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

        // 测试代码（参考 CSDN 博客示例）
        text = "比特币的作者是谁？";
        try (Analyzer analyzer = new SmartChineseAnalyzer(STOPWORDS)) {
            TokenStream stream = analyzer.tokenStream("", text);
            CharTermAttribute attr = stream.addAttribute(CharTermAttribute.class);
            stream.reset();

            List<String> tokens = new ArrayList<>();
            while (stream.incrementToken()) {
                tokens.add(attr.toString());
            }
            stream.end();

            System.out.println("过滤后结果：" + tokens);
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }
}

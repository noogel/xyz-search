package noogel.xyz.search.service;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

public class LuceneConfiguration {

    /**
     * 写索引实例
     */
    private IndexWriter writer;

    /**
     * 构造方法 实例化IndexWriter
     *
     * @param indexDir
     * @throws IOException
     */
    public LuceneConfiguration(String indexDir) throws IOException {
        // 得到索引所在目录的路径
        Directory directory = FSDirectory.open(Paths.get(indexDir));
        // 标准分词器
        Analyzer analyzer = new StandardAnalyzer();
        // 保存用于创建IndexWriter的所有配置。
        IndexWriterConfig iwConfig = new IndexWriterConfig(analyzer);
        // 实例化IndexWriter
        writer = new IndexWriter(directory, iwConfig);
    }

    /**
     * 关闭写索引
     *
     * @return 索引了多少个文件
     * @throws Exception
     */
    public void close() throws IOException {
        writer.close();
    }

    public int index(String dataDir) throws Exception {
        File[] files = new File(dataDir).listFiles();
        for (File file : files) {
            // 索引指定文件
            indexFile(file);
        }
        // 返回索引了多少个文件
        return writer.numRamDocs();

    }

    /**
     * 索引指定文件
     *
     * @param f
     */
    private void indexFile(File f) throws Exception {
        // 输出索引文件的路径
        System.out.println("索引文件：" + f.getCanonicalPath());
        // 获取文档，文档里再设置每个字段
        Document doc = getDocument(f);
        // 开始写入,就是把文档写进了索引文件里去了；
        writer.addDocument(doc);
    }

    /**
     * 获取文档，文档里再设置每个字段
     *
     * @param f
     * @return document
     */
    private Document getDocument(File f) throws Exception {
        Document doc = new Document();
        // 把设置好的索引加到Document里，以便在确定被索引文档
        doc.add(new TextField("contents", new FileReader(f)));
        // Field.Store.YES：把文件名存索引文件里，为NO就说明不需要加到索引文件里去
        doc.add(new TextField("fileName", f.getName(), Field.Store.YES));
        // 把完整路径存在索引文件里
        doc.add(new TextField("fullPath", f.getCanonicalPath(), Field.Store.YES));
        return doc;
    }

}

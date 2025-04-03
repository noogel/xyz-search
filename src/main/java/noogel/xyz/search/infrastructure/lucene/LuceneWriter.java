package noogel.xyz.search.infrastructure.lucene;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import noogel.xyz.search.infrastructure.lucene.annotation.KeyWordId;
import noogel.xyz.search.infrastructure.lucene.annotation.PkId;
import noogel.xyz.search.infrastructure.lucene.annotation.SortedId;

public class LuceneWriter {

    private final Directory directory;
    private final PerFieldAnalyzerWrapper wrapper;
    private final AtomicReference<IndexWriter> writerRef = new AtomicReference<>();

    public LuceneWriter(Path dir, PerFieldAnalyzerWrapper wrapper) {
        try {
            this.directory = FSDirectory.open(dir);
            this.wrapper = wrapper;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        initLuceneIndex();
    }

    private void initLuceneIndex() {
        try {
            IndexWriter writer = getWriter();
            writer.commit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取共享的 IndexWriter 实例
     */
    private IndexWriter getWriter() {
        IndexWriter writer = writerRef.get();
        if (writer == null) {
            synchronized (this) {
                writer = writerRef.get();
                if (writer == null) {
                    try {
                        IndexWriterConfig config = new IndexWriterConfig(wrapper);
                        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                        config.setRAMBufferSizeMB(256);
                        config.setMaxBufferedDocs(2000);
                        config.setMergePolicy(new LogByteSizeMergePolicy());
                        config.setMergeScheduler(new ConcurrentMergeScheduler());
                        config.setUseCompoundFile(false);
                        writer = new IndexWriter(directory, config);
                        writerRef.set(writer);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return writer;
    }

    /**
     * 关闭 IndexWriter
     */
    public void close() {
        IndexWriter writer = writerRef.getAndSet(null);
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void forceMerge() {
        try {
            IndexWriter writer = getWriter();
            writer.forceMerge(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reset() {
        try {
            IndexWriter writer = getWriter();
            writer.deleteAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean update(FtsDocument data) {
        try {
            IndexWriter writer = getWriter();
            Document doc = new Document();
            Term term = null;
            for (var declaredField : data.getClass().getDeclaredFields()) {
                String name = declaredField.getName();
                Class<?> declaringClass = declaredField.getType();
                PkId pkId = declaredField.getAnnotation(PkId.class);
                SortedId sortedId = declaredField.getAnnotation(SortedId.class);
                KeyWordId keyWordId = declaredField.getAnnotation(KeyWordId.class);
                declaredField.setAccessible(true);
                Object val = declaredField.get(data);
                convert(declaringClass, name, val, pkId, sortedId, keyWordId).forEach(doc::add);
                if (Objects.nonNull(pkId)) {
                    term = new Term(name, Objects.isNull(val) ? "" : ((String) val));
                }
            }
            return writer.updateDocument(term, doc) > 0;
        } catch (IOException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean delete(FtsDocument data) {
        try {
            IndexWriter writer = getWriter();
            Term term = null;
            for (var declaredField : data.getClass().getDeclaredFields()) {
                String name = declaredField.getName();
                PkId pkId = declaredField.getAnnotation(PkId.class);
                declaredField.setAccessible(true);
                Object val = declaredField.get(data);
                if (Objects.nonNull(pkId)) {
                    term = new Term(name, Objects.isNull(val) ? "" : ((String) val));
                    break;
                }
            }
            return writer.deleteDocuments(term) > 0;
        } catch (IOException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 提交更改到索引
     */
    public void commit() {
        try {
            IndexWriter writer = writerRef.get();
            if (writer != null) {
                writer.commit();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Field> convert(Class<?> clazz, String name, Object value,
            PkId pkId, SortedId sortedId, KeyWordId keyWordId) {
        List<Field> result = new ArrayList<>();
        if (Objects.nonNull(pkId)) {
            StringField fd = new StringField(name, Objects.isNull(value) ? "" : ((String) value), Field.Store.YES);
            result.add(fd);
        } else if (String.class.equals(clazz)) {
            if (Objects.nonNull(sortedId)) {
                String sortedName = String.format("%s_sorted", name);
                SortedDocValuesField sfd = new SortedDocValuesField(sortedName,
                        new BytesRef((Objects.isNull(value) ? "" : ((String) value)).getBytes()));
                result.add(sfd);
            }
            // Lucene 字段类型决定是否支持搜索
            // TextField：会分词并索引，适用于全文搜索。
            // StringField：不分词，作为整体索引，适合精确匹配（如 ID、状态码）。
            // StoredField：仅存储，不索引，无法用于搜索。
            if (Objects.isNull(keyWordId)) {
                TextField fd = new TextField(name, Objects.isNull(value) ? "" : ((String) value), Field.Store.YES);
                result.add(fd);
            } else {
                StringField fd = new StringField(name, Objects.isNull(value) ? "" : ((String) value), Field.Store.YES);
                result.add(fd);
            }
        } else if (Long.class.equals(clazz)) {
            if (Objects.nonNull(sortedId)) {
                String sortedName = String.format("%s_sorted", name);
                NumericDocValuesField nfd = new NumericDocValuesField(sortedName,
                        Objects.isNull(value) ? 0L : ((Long) value));
                result.add(nfd);
            }
            LongField fd = new LongField(name, Objects.isNull(value) ? 0L : ((Long) value), Field.Store.YES);
            result.add(fd);
        } else if (Integer.class.equals(clazz)) {
            if (Objects.nonNull(sortedId)) {
                String sortedName = String.format("%s_sorted", name);
                NumericDocValuesField nfd = new NumericDocValuesField(sortedName,
                        Objects.isNull(value) ? 0 : ((Long) value));
                result.add(nfd);
            }
            IntField fd = new IntField(name, Objects.isNull(value) ? 0 : ((Integer) value), Field.Store.YES);
            result.add(fd);
        } else {
            throw new IllegalArgumentException();
        }
        return result;
    }
}

package noogel.xyz.search.infrastructure.lucene;

import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class LuceneWriter {

    private final Directory directory;
    private final PerFieldAnalyzerWrapper wrapper;


    public LuceneWriter(Path dir, PerFieldAnalyzerWrapper wrapper) {
        try {
            this.directory = FSDirectory.open(dir);
            this.wrapper = wrapper;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean write(LuceneDocument data) {
        IndexWriterConfig config = new IndexWriterConfig(wrapper);
        try (IndexWriter writer = new IndexWriter(directory, config)) {
            Document doc = new Document();
            for (var declaredField : data.getClass().getDeclaredFields()) {
                String name = declaredField.getName();
                Class<?> declaringClass = declaredField.getType();
                PkId pkId = declaredField.getAnnotation(PkId.class);
                declaredField.setAccessible(true);
                Object val = declaredField.get(data);
                doc.add(convert(declaringClass, name, val, pkId));
            }
            return writer.addDocument(doc) > 0;
        } catch (IOException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void forceMerge() {
        IndexWriterConfig config = new IndexWriterConfig(wrapper);
        try (IndexWriter writer = new IndexWriter(directory, config)) {
            writer.forceMerge(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reset() {
        IndexWriterConfig config = new IndexWriterConfig(wrapper);
        try (IndexWriter writer = new IndexWriter(directory, config)) {
            writer.deleteAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean update(LuceneDocument data) {
        IndexWriterConfig config = new IndexWriterConfig(wrapper);
        try (IndexWriter writer = new IndexWriter(directory, config)) {
            Document doc = new Document();
            Term term = null;
            for (var declaredField : data.getClass().getDeclaredFields()) {
                String name = declaredField.getName();
                Class<?> declaringClass = declaredField.getType();
                PkId pkId = declaredField.getAnnotation(PkId.class);
                declaredField.setAccessible(true);
                Object val = declaredField.get(data);
                doc.add(convert(declaringClass, name, val, pkId));
                if (Objects.nonNull(pkId)) {
                    term = new Term(name, Objects.isNull(val) ? "" : ((String) val));
                }
            }
            return writer.updateDocument(term, doc) > 0;
        } catch (IOException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean delete(LuceneDocument data) {
        IndexWriterConfig config = new IndexWriterConfig(wrapper);
        try (IndexWriter writer = new IndexWriter(directory, config)) {
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

    public Field convert(Class<?> clazz, String name, Object value, PkId pkId) {
        if (Objects.nonNull(pkId)) {
            return new StoredField(name, Objects.isNull(value) ? "" : ((String) value));
        } else if (String.class.equals(clazz)) {
            return new TextField(name, Objects.isNull(value) ? "" : ((String) value), Field.Store.YES);
        } else if (Long.class.equals(clazz)) {
            return new LongField(name, Objects.isNull(value) ? 0L : ((Long) value), Field.Store.YES);
        } else if (Integer.class.equals(clazz)) {
            return new IntField(name, Objects.isNull(value) ? 0 : ((Integer) value), Field.Store.YES);
        } else {
            throw new IllegalArgumentException();
        }
    }
}

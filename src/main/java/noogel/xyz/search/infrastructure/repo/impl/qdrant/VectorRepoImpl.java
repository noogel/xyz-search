package noogel.xyz.search.infrastructure.repo.impl.qdrant;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.dao.ChapterDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.repo.VectorRepo;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class VectorRepoImpl implements VectorRepo {

    @Resource
    private VectorStore vectorStore;

    @Override
    public void upsert(FileResReadDto res, FileResContentDto content) {
        var tokenTextSplitter = new TokenTextSplitter();
        List<Document> documentList = new ArrayList<>();
        for (ChapterDto chapterDto : content.getChapterList()) {
            Document document = new Document(
                    res.getResId(),
                    chapterDto.getContent(),
                    buildMetadata(res, content));
            documentList.add(document);
        }
        log.info("Parsing document, splitting, creating embeddings and storing in vector store...  this will take a while.");
        List<Document> apply = tokenTextSplitter.apply(documentList);
        this.vectorStore.accept(apply);
        log.info("Done parsing document, splitting, creating embeddings and storing in vector store");
    }

    @Override
    public void delete(String resId) {
        try {
            this.vectorStore.delete(List.of(resId));
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage().contains("Invalid UUID string")) {
                return;
            }
            throw ex;
        }
    }

    private Map<String, Object> buildMetadata(FileResReadDto res, FileResContentDto content) {
        Map<String, Object> metadata = new HashMap<>();
        Optional.ofNullable(content.getMetadata()).orElse(new HashMap<>()).forEach((key,val)->{
            if (Objects.nonNull(key) && Objects.nonNull(val)) {
                metadata.put(key, val);
            }
        });
        metadata.put("resId", res.getResId());
        metadata.put("filePath", res.calFilePath());
        return metadata;
    }

}

package noogel.xyz.search.infrastructure.repo.impl.qdrant;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.dao.ChapterDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.repo.VectorRepo;
import noogel.xyz.search.infrastructure.utils.MD5Helper;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VectorRepoImpl implements VectorRepo {

    @Resource
    private VectorStore vectorStore;

    @Override
    public void append(FileResContentDto dto) {
        var tokenTextSplitter = new TokenTextSplitter();
        List<Document> documentList = new ArrayList<>();
        for (ChapterDto chapterDto : dto.getChapterList()) {
            Document document = new Document(
                    MD5Helper.getMD5(chapterDto.getContent()),
                    chapterDto.getContent(),
                    Optional.ofNullable(dto.getMetadata()).orElse(new HashMap<>())
                            .entrySet().stream()
                            .filter(t-> Objects.nonNull(t.getKey()))
                            .filter(t-> Objects.nonNull(t.getValue()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            documentList.add(document);
        }
        log.info("Parsing document, splitting, creating embeddings and storing in vector store...  this will take a while.");
        List<Document> apply = tokenTextSplitter.apply(documentList);
        this.vectorStore.accept(apply);
        log.info("Done parsing document, splitting, creating embeddings and storing in vector store");
    }
}

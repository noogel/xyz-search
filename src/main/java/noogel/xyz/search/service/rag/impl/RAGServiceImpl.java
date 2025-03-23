package noogel.xyz.search.service.rag.impl;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.config.VectorClient;
import noogel.xyz.search.service.rag.RAGService;

@Service
@Slf4j
public class RAGServiceImpl implements RAGService {

    @Resource
    private VectorClient vectorClient;
    @Resource
    private ConfigProperties configProperties;

    @Override
    public List<Document> search(String query) {
        if (vectorClient.getVectorStore() == null) {
            log.warn("向量存储未初始化");
            return List.of();
        } else {
            return vectorClient.getVectorStore().similaritySearch(query);
        }
    }

    @Override
    public Document searchById(String resId) {
        if (vectorClient.getVectorStore() == null) {
            log.warn("向量存储未初始化");
            return null;
        } else {
            // todo
            return null;
        }
    }

    @Override
    public void asyncAppend(String resId) {
        if (vectorClient.getVectorStore() == null) {
            log.warn("向量存储未初始化");
            return;
        } else {
            // todo
        }
    }
}
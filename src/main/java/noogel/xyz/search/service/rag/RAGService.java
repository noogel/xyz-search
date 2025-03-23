package noogel.xyz.search.infrastructure.rag;

import java.util.List;

import org.springframework.ai.document.Document;

public interface RAGService {

    /**
     * 根据查询条件搜索文档
     * @param query 查询条件
     * @return 搜索到的文档列表
     */
    List<Document> search(String query);

    /**
     * 根据文档ID搜索文档
     * @param id 文档ID
     * @return 搜索到的文档
     */
    Document searchById(String resId);

    /**
     * 异步追加文档
     * @param resId 文档ID
     */
    void asyncAppend(String resId);
}

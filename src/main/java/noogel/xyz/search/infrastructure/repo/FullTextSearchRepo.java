package noogel.xyz.search.infrastructure.repo;

import noogel.xyz.search.infrastructure.dto.ResourceHighlightHitsDto;
import noogel.xyz.search.infrastructure.dto.SearchResultDto;
import noogel.xyz.search.infrastructure.dto.repo.CommonSearchDto;
import noogel.xyz.search.infrastructure.dto.repo.RandomSearchDto;
import noogel.xyz.search.infrastructure.model.FileEsModel;
import noogel.xyz.search.infrastructure.model.lucene.FullTextSearchModel;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 全文搜索资源库
 */
public interface FullTextSearchRepo {

    /**
     * 删除
     * @param resId
     * @return
     */
    boolean delete(String resId);

    /**
     * 创建或更新
     * @param model
     * @return
     */
    boolean upsert(FullTextSearchModel model);

    /**
     * 压缩数据
     */
    void forceMerge();

    /**
     * 重置索引
     */
    void reset();

    /**
     * 查询
     * @param resId
     * @return
     */
    FullTextSearchModel findByResId(String resId);

    /**
     * 查询
     * @param resHash
     * @return
     */
    List<FullTextSearchModel> findByResHash(String resHash);

    /**
     * 按资源搜索
     * @param resId
     * @param text
     * @return
     */
    ResourceHighlightHitsDto searchByResId(String resId, @Nullable String text);

    /**
     * 通用搜索
     * @param searchDto
     * @return
     */
    SearchResultDto commonSearch(CommonSearchDto searchDto);

    /**
     * 随机搜索
     * @param searchDto
     * @return
     */
    SearchResultDto randomSearch(RandomSearchDto searchDto);

}

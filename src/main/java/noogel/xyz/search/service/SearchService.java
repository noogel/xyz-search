package noogel.xyz.search.service;

import noogel.xyz.search.infrastructure.dto.OPDSResultShowDto;
import noogel.xyz.search.infrastructure.dto.ResourceDownloadDto;
import noogel.xyz.search.infrastructure.dto.SearchQueryDto;
import noogel.xyz.search.infrastructure.dto.page.ResourcePageDto;
import noogel.xyz.search.infrastructure.dto.page.ResourceSimpleDto;
import noogel.xyz.search.infrastructure.dto.page.SearchResultShowDto;

import java.util.List;

public interface SearchService {
    /**
     * 主页面搜索
     *
     * @param query
     * @return
     */
    SearchResultShowDto pageSearch(SearchQueryDto query);

    /**
     * opds 资源搜索
     *
     * @param query
     * @return
     */
    OPDSResultShowDto opdsSearch(SearchQueryDto query);

    /**
     * 根据资源搜索
     *
     * @param resId
     * @param search
     * @return
     */
    ResourcePageDto searchByResId(String resId, String search);

    /**
     * 获取资源下载信息
     *
     * @param resId
     * @return
     */
    ResourceDownloadDto getDownloadResource(String resId);
}

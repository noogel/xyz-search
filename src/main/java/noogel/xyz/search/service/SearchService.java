package noogel.xyz.search.service;

import noogel.xyz.search.infrastructure.dto.OPDSResultShowDto;
import noogel.xyz.search.infrastructure.dto.ResourceDownloadDto;
import noogel.xyz.search.infrastructure.dto.SearchQueryDto;
import noogel.xyz.search.infrastructure.dto.api.SearchQueryApiDto;
import noogel.xyz.search.infrastructure.dto.api.SearchResultApiDto;
import noogel.xyz.search.infrastructure.dto.page.ResourcePageDto;
import noogel.xyz.search.infrastructure.dto.page.SearchResultShowDto;

public interface SearchService {
    /**
     * 主页面搜索
     *
     * @param query
     * @return
     */
    SearchResultShowDto pageSearch(SearchQueryDto query);

    /**
     * api 搜索
     * @param query
     * @return
     */
    SearchResultApiDto apiSearch(SearchQueryApiDto query);

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

    /**
     * 获取资源内容
     * @param resId
     * @return
     */
    String getResourceContent(String resId);
}

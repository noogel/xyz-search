package noogel.xyz.search.service;

import noogel.xyz.search.infrastructure.dto.*;
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
     * 根据文件 MD5 搜索
     *
     * @param resHash
     * @return
     */
    List<ResourceSimpleDto> searchByResHash(String resHash);

    /**
     * 获取资源下载信息
     *
     * @param resId
     * @return
     */
    ResourceDownloadDto getDownloadResource(String resId);
}

package noogel.xyz.search.service;

import noogel.xyz.search.infrastructure.dto.ResourcePageDto;
import noogel.xyz.search.infrastructure.dto.SearchQueryDto;
import noogel.xyz.search.infrastructure.dto.SearchResultShowDto;

public interface SearchService {
    SearchResultShowDto search(SearchQueryDto query);
    ResourcePageDto searchByResId(String resId, String search);

    String getResourcePath(String resId);
}

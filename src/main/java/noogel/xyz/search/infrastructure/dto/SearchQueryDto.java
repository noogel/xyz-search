package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import static noogel.xyz.search.infrastructure.utils.UrlHelper.ct;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SearchQueryDto extends SearchBaseQueryDto {
    private String relativeResDir;
    private String resId;

    public String getUrlQuery(long offset) {
        return String.format("search=%s&resId=%s&resSize=%s&modifiedAt=%s&limit=%s&offset=%s&relativeResDir=%s&resType=%s",
                ct(getSearch()), ct(resId), ct(getResSize()), ct(getModifiedAt()),
                ct(getLimit()), ct(offset), ct(relativeResDir), ct(getResType()));
    }

    public static boolean indexEmptySearch(SearchQueryDto dto) {
        return StringUtils.isEmpty(dto.getSearch())
                && StringUtils.isEmpty(dto.getResDirPrefix())
                && StringUtils.isEmpty(dto.getResSize())
                && StringUtils.isEmpty(dto.getModifiedAt())
                && StringUtils.isEmpty(dto.getResType());
    }

    public static boolean dirEmptySearch(SearchQueryDto dto) {
        return StringUtils.isEmpty(dto.getSearch()) && StringUtils.isNotEmpty(dto.getResDirPrefix());
    }

}

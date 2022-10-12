package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static noogel.xyz.search.infrastructure.utils.UrlHelper.ct;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SearchQueryDto extends SearchBaseQueryDto {
    private String relativeResDir;
    private String resId;

    public String getUrlQuery(long offset) {
        return String.format("search=%s&resId=%s&resSize=%s&modifiedAt=%s&limit=%s&offset=%s&relativeResDir=%s",
                ct(getSearch()), ct(resId), ct(getResSize()), ct(getModifiedAt()),
                ct(getLimit()), ct(offset), ct(relativeResDir));
    }

}

package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import static noogel.xyz.search.infrastructure.utils.UrlHelper.ct;

@Data
public class SearchQueryDto {
    private String relativeResDir;
    private String resId;
    private String search;
    private String resDirPrefix;
    private String resSize;
    private String resType;
    private String modifiedAt;
    private Integer limit = 10;
    private Integer offset = 0;
    private Boolean randomScore;
    private QueryOrderDto order;

    @Data
    public static class QueryOrderDto {
        private String field;
        private boolean ascOrder;

        public static QueryOrderDto of(String field, boolean ascOrder) {
            QueryOrderDto dto = new QueryOrderDto();
            dto.setField(field);
            dto.setAscOrder(ascOrder);
            return dto;
        }
    }

    public static QueryOrderDto buildRankOrder(boolean ascOrder) {
        return QueryOrderDto.of("rank", ascOrder);
    }

    public static QueryOrderDto buildLatestOrder(boolean ascOrder) {
        return QueryOrderDto.of("modifiedAt", ascOrder);
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

    public String getUrlQuery(long offset) {
        return String.format("search=%s&resId=%s&resSize=%s&modifiedAt=%s&limit=%s&offset=%s&relativeResDir=%s&resType=%s",
                ct(getSearch()), ct(resId), ct(getResSize()), ct(getModifiedAt()),
                ct(getLimit()), ct(offset), ct(relativeResDir), ct(getResType()));
    }

}

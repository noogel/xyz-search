package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

import static noogel.xyz.search.infrastructure.utils.UrlHelper.ct;

@Data
public class SearchQueryDto {
    private String search;
    private String resDirPrefix;
    private String relativeResDir;
    private String resId;
    private String resSize;
    private List<String> resType;
    private String modifiedAt;
    private Integer limit = 10;
    private Integer page = 1;
    private Boolean randomScore;
    private QueryOrderDto order;

    public static QueryOrderDto buildRankOrder(boolean ascOrder) {
        return QueryOrderDto.of("resRank", ascOrder);
    }

    public static QueryOrderDto buildLatestOrder(boolean ascOrder) {
        return QueryOrderDto.of("modifiedAt", ascOrder);
    }

    public static boolean indexEmptySearch(SearchQueryDto dto) {
        return StringUtils.isEmpty(dto.getSearch())
                && StringUtils.isEmpty(dto.getResDirPrefix())
                && StringUtils.isEmpty(dto.getResSize())
                && StringUtils.isEmpty(dto.getModifiedAt())
                && CollectionUtils.isEmpty(dto.getResType());
    }

    public static boolean dirEmptySearch(SearchQueryDto dto) {
        return StringUtils.isEmpty(dto.getSearch()) && StringUtils.isNotEmpty(dto.getResDirPrefix());
    }

    public String getUrlQuery(long page) {
        String queryText = String.format("search=%s&resId=%s&resSize=%s&modifiedAt=%s&limit=%s&page=%s&relativeResDir=%s",
                ct(getSearch()), ct(resId), ct(getResSize()), ct(getModifiedAt()),
                ct(getLimit()), ct(page), ct(relativeResDir));
        if (!CollectionUtils.isEmpty(getResType())) {
            queryText += "&";
            queryText += getResType().stream()
                    .map(l -> String.format("resType=%s", l)).collect(Collectors.joining("&"));
        }
        return queryText;
    }

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

}

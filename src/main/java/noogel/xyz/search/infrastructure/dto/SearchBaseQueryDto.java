package noogel.xyz.search.infrastructure.dto;

import lombok.Data;

@Data
public class SearchBaseQueryDto {
    private String search;
    private String resDirPrefix;
    private String resSize;
    private String resType;
    private String modifiedAt;
    private Integer limit = 10;
    private Integer offset = 0;
    private Boolean randomScore;
    private QueryOrderDto order;

    public static QueryOrderDto buildRankOrder(boolean ascOrder) {
        return QueryOrderDto.of("rank", ascOrder);
    }

    public static QueryOrderDto buildLatestOrder(boolean ascOrder) {
        return QueryOrderDto.of("modifiedAt", ascOrder);
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

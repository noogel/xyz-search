package noogel.xyz.search.infrastructure.dto.repo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class CommonSearchDto {
    /**
     * 资源 ID
     */
    private String resId;
    /**
     * 搜索内容
     */
    private String searchQuery;
    /**
     * 目录前缀
     */
    private String dirPrefix;
    /**
     * 资源大小
     */
    private Field resSize;
    /**
     * 资源类型
     */
    private List<String> resTypeList;
    /**
     * 更新时间
     */
    private Field modifiedAt;
    /**
     * 分页
     */
    private Paging paging;
    /**
     * 排序
     */
    private OrderBy order;

    /**
     * 比较枚举
     */
    public enum CompareEnum {
        GT,
        LT,
        EQ,
    }

    @Data
    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    public static class Paging {
        private Integer limit = 10;
        private Integer offset = 0;
    }

    @Data
    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    public static class Field {
        /**
         * 比较
         */
        private CompareEnum compare;
        /**
         * 值
         */
        private String value;

        public static Field of(String val) {
            String[] tmp = val.split(":");
            return of(CompareEnum.valueOf(tmp[0]), tmp[1]);
        }
    }


    @Data
    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    public static class OrderBy {
        private String field;
        private boolean asc;
    }

}

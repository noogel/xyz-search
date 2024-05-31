package noogel.xyz.search.infrastructure.dto;

import lombok.Data;

@Data
public class PagingDto {
    private SearchQueryDto query;
    private long total;

    private long startOffset;
    private long prevOffset;
    private long nextOffset;
    private long endOffset;
    private boolean prevPageDisable;
    private boolean nextPageDisable;
    private String pageDesc;

    public static PagingDto of(SearchQueryDto query, long total) {
        PagingDto dto = new PagingDto();
        dto.query = query;
        dto.total = total;
        // 当前页
        long currentPage = query.getOffset() / query.getLimit() + 1;
        // 总页数
        long totalPage = (total / query.getLimit()) + (Math.min(1, total % query.getLimit()));

        // 计算按钮的 offset
        dto.startOffset = 0;
        dto.prevOffset = Math.max(currentPage - 1, 1) * query.getLimit() - query.getLimit();
        dto.nextOffset = Math.min(currentPage + 1, totalPage) * query.getLimit() - query.getLimit();
        dto.endOffset = totalPage * query.getLimit() - query.getLimit();
        // 页面的描述
        dto.pageDesc = String.format("第 %s 页 / 共 %s 页 | %s 条", currentPage, totalPage, total);
        // 禁用的计算
        dto.prevPageDisable = currentPage == 1;
        dto.nextPageDisable = currentPage == totalPage;

        return dto;
    }

    public String getStartPageQuery() {
        return query.getUrlQuery(startOffset);
    }

    public String getPrevPageQuery() {
        return query.getUrlQuery(prevOffset);
    }

    public String getNextPageQuery() {
        return query.getUrlQuery(nextOffset);
    }

    public String getEndPageQuery() {
        return query.getUrlQuery(endOffset);
    }

    public String getPageDesc() {
        return pageDesc;
    }

    public String getPrevPageDisable() {
        if (prevPageDisable) {
            return "disabled";
        }
        return "";
    }

    public String getNextPageDisable() {
        if (nextPageDisable) {
            return "disabled";
        }
        return "";
    }

}

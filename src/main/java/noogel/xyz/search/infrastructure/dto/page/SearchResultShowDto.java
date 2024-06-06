package noogel.xyz.search.infrastructure.dto.page;

import lombok.Data;
import noogel.xyz.search.infrastructure.dto.BreadcrumbDto;
import noogel.xyz.search.infrastructure.dto.PagingDto;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Data
public class SearchResultShowDto {
    /**
     * 数据
     */
    private List<ResourceSimpleDto> data;
    /**
     * 分页
     */
    private PagingDto paging;
    /**
     * 导航条
     */
    private BreadcrumbDto breadcrumb;

    public boolean showBreadCrumb() {
        return Objects.nonNull(breadcrumb) && !CollectionUtils.isEmpty(breadcrumb.getItems());
    }

    public boolean showSearchBar() {
        return !showBreadCrumb();
    }
}

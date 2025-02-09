package noogel.xyz.search.infrastructure.lucene;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class Paging {
    /**
     * 当前页
     */
    private int currentPage;
    /**
     * 每页数量
     */
    private int perPage;

    public int calculateOffset() {
        return (currentPage - 1) * perPage;
    }
    public int calculateNextOffset() {
        return currentPage * perPage;
    }
    public int calculateNextOffset(int max) {
        return Math.min(currentPage * perPage, max);
    }
}

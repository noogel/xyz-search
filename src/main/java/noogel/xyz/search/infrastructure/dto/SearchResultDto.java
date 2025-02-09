package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import noogel.xyz.search.infrastructure.lucene.LuceneDocument;
import noogel.xyz.search.infrastructure.model.FileEsModel;
import noogel.xyz.search.infrastructure.model.lucene.FullTextSearchModel;

import java.util.List;

@Data
public class SearchResultDto {
    /**
     * 分页数据
     */
    private List<FileEsModel> data;
    private List<FullTextSearchModel> data2;
    /**
     * 数量
     */
    private long size;
    /**
     * 是否准确数量
     */
    private boolean exactSize;
}

package noogel.xyz.search.infrastructure.dto;

import lombok.Data;

@Data
public class OPDSItemShowDto {
    /**
     * 资源完整路径 HASH
     */
    private String resId;
    /**
     * 资源文件名
     */
    private String resName;
    /**
     * 资源 meta 名称
     */
    private String resTitle;
    /**
     * 资源大小
     */
    private Long resSize;
    /**
     * 资源最近更新时间 秒
     */
    private Long modifiedAt;
    /**
     * 搜索内容
     */
    private String searchQuery;
    /**
     * 资源路径
     */
    private String resDir;
    /**
     * 内容类型
     */
    private String contentType;

}

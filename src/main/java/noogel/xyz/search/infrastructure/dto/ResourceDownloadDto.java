package noogel.xyz.search.infrastructure.dto;

import lombok.Data;

@Data
public class ResourceDownloadDto {
    /**
     * 资源 HASH
     */
    private String resId;
    /**
     * 资源名称
     */
    private String resTitle;
    /**
     * 资源位置
     */
    private String absolutePath;
    /**
     * 资源目录
     */
    private String resDir;
}

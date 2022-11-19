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
     * 资源大小
     */
    private String absolutePath;
}

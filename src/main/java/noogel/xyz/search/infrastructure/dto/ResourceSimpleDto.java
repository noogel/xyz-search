package noogel.xyz.search.infrastructure.dto;

import lombok.Data;

@Data
public class ResourceSimpleDto {
    /**
     * 资源 HASH
     */
    private String resHash;
    /**
     * 资源名称
     */
    private String resName;
    /**
     * 资源大小
     */
    private String resSize;
    /**
     * 更新时间 秒
     */
    private String modifiedAt;
}

package noogel.xyz.search.infrastructure.dto.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class FileDbDto {
    /**
     * 完整路径
     */
    private String path;

    /**
     * 是否目录
     */
    private Boolean dir;

    /**
     * 文件 ID
     */
    private Long fileId;

    /**
     * 文件大小
     */
    private Long size;

    /**
     * 文件最近更新时间
     */
    private Long modifiedAt;

    public boolean isFile() {
        return !isDirectory() || Objects.nonNull(fileId);
    }

    public boolean isDirectory() {
        return Objects.nonNull(dir) && dir;
    }
}

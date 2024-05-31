package noogel.xyz.search.infrastructure.dto.dao;

import lombok.Data;

import java.io.File;
import java.util.Map;

@Data
public class FileResWriteDto {
    /**
     * UUID
     */
    private String resId;
    /**
     * 目录
     */
    private String dir;
    /**
     * 文件名
     */
    private String name;
    /**
     * 文件大小
     */
    private Long size;
    /**
     * 最近更新时间
     */
    private Long modifiedAt;

    /**
     * 扩展
     * FILE:EPUB
     * FILE:TXT
     */
    private String type;
    /**
     * 文件 hash
     */
    private String hash;
    /**
     * 根据资源名称计算的分数
     */
    private Long rank;
    /**
     * 资源 meta 名称
     */
    private Map<String, String> options;

    public int calDirDep() {
        return dir.split("/").length;
    }

    public String calFilePath() {
        return String.format("%s/%s", dir, name);
    }

    public File genFile() {
        return new File(calFilePath());
    }
}

package noogel.xyz.search.infrastructure.dto.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class FileFsDto {

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
     * hash 计算
     */
    private String hash;
    /**
     * 最近更新时间
     */
    private Long modifiedAt;
    /**
     * 扩展信息
     */
    private OptionsDto options;
    /**
     * 内容列表
     */
    private List<ChapterDto> chapterList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class OptionsDto {
        private String title;
    }

    public int calDirDep() {
        return dir.split("/").length;
    }
}

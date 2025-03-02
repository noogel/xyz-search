package noogel.xyz.search.infrastructure.model.lucene;

import lombok.Data;
import noogel.xyz.search.infrastructure.lucene.LuceneDocument;
import noogel.xyz.search.infrastructure.lucene.annotation.PkId;
import noogel.xyz.search.infrastructure.lucene.annotation.SortedId;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Data
public class FullTextSearchModel implements LuceneDocument {

    /**
     * 资源完整路径 HASH
     */
    @PkId
    private String resId;
    /**
     * 资源路径
     */
    private String resDir;
    /**
     * 资源物理文件名
     */
    private String resName;
    /**
     * 资源 meta 名称
     */
    private String resTitle;
    /**
     * 文件 HASH
     */
    private String resHash;
    /**
     * 类型
     * FILE:EPUB
     * FILE:TXT
     */
    private String resType;
    /**
     * 资源大小
     */
    private Long resSize;
    /**
     * 资源最近更新时间 秒
     */
    @SortedId
    private Long modifiedAt;
    /**
     * 根据资源名称计算的分数
     */
    private Long resRank;
    /**
     * 搜索内容
     */
    private String content;
    /**
     * 内容 HASH
     */
    private String contentHash;
    /**
     * 内容大小
     */
    private Integer contentSize;

    /**
     * 计算完整路径
     *
     * @return
     */
    public String calculateAbsolutePath() {
        return String.format("%s/%s", resDir, resName);
    }

    /**
     * 计算相对路径
     *
     * @return
     */
    public String calculateRelativePath(List<String> rootDirs) {
        String absolutePath = calculateAbsolutePath();
        String resp = absolutePath;
        rootDirs = Optional.ofNullable(rootDirs).orElse(Collections.emptyList());
        for (String searchPath : rootDirs) {
            if (absolutePath.startsWith(searchPath)) {
                String substring = absolutePath.substring(searchPath.length());
                if (substring.length() < resp.length()) {
                    resp = substring;
                }
            }
        }
        return resp;
    }

    /**
     * 计算相对目录
     *
     * @return
     */
    public String calculateRelativeDir(List<String> rootDirs) {
        String resp = this.resDir;
        rootDirs = Optional.ofNullable(rootDirs).orElse(Collections.emptyList());
        for (String searchPath : rootDirs) {
            if (this.resDir.startsWith(searchPath)) {
                String substring = this.resDir.substring(searchPath.length());
                if (substring.length() < resp.length()) {
                    resp = substring;
                }
            }
        }
        return resp;
    }
}

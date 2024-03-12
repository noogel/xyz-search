package noogel.xyz.search.infrastructure.model.elastic;

import co.elastic.clients.elasticsearch._types.mapping.*;
import lombok.Data;

import java.util.*;

@Data
public class FileEsModel {

    /**
     * 资源完整路径 HASH
     */
    private String resId;
    /**
     * 资源路径
     */
    private String resDir;
    /**
     * 资源文件名
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
    private Long modifiedAt;
    /**
     * 根据资源名称计算的分数
     */
    private Long rank;

    /**
     * 搜索内容
     */
    private String searchableText;
    /**
     * 内容 HASH
     */
    private String textHash;
    /**
     * 内容大小
     */
    private Integer textSize;

    public static Map<String, Property> generateEsMapping() {
        Map<String, Property> documentMap = new HashMap<>();
        documentMap.put("resId", Property
                .of(p -> p.keyword(KeywordProperty.of(i -> i.index(true)))));
        documentMap.put("resName", Property
                .of(p -> p.text(TextProperty.of(i -> i.index(true).analyzer("ik_smart")))));
        documentMap.put("resTitle", Property
                .of(p -> p.text(TextProperty.of(i -> i.index(true).analyzer("ik_smart")))));
        documentMap.put("resChapter", Property
                .of(p -> p.text(TextProperty.of(i -> i.index(true).analyzer("ik_smart")))));
        documentMap.put("rank", Property
                .of(p -> p.long_(LongNumberProperty.of(i -> i.index(true)))));
        documentMap.put("resDir", Property
                .of(p -> p.text(TextProperty.of(i -> i.index(true).analyzer("path_tokenizer")))));
        documentMap.put("resHash", Property
                .of(p -> p.keyword(KeywordProperty.of(i -> i.index(true)))));
        documentMap.put("resType", Property
                .of(p -> p.keyword(KeywordProperty.of(i -> i.index(true)))));
        documentMap.put("resSize", Property
                .of(p -> p.long_(LongNumberProperty.of(i -> i.index(true)))));
        documentMap.put("modifiedAt", Property
                .of(p -> p.long_(LongNumberProperty.of(i -> i.index(true)))));

        documentMap.put("searchableText", Property
                .of(p -> p.text(TextProperty.of(i -> i.index(true).analyzer("ik_smart")))));
        documentMap.put("textHash", Property
                .of(p -> p.keyword(KeywordProperty.of(i -> i.index(true)))));
        documentMap.put("textSize", Property
                .of(p -> p.integer(IntegerNumberProperty.of(i -> i.index(true)))));
        return documentMap;
    }

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
    public String calculateRelativePath(List<String> searchDirs) {
        String absolutePath = calculateAbsolutePath();
        String resp = absolutePath;
        searchDirs = Optional.ofNullable(searchDirs).orElse(Collections.emptyList());
        for (String searchPath : searchDirs) {
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
    public String calculateRelativeDir(List<String> searchDirs) {
        String resp = this.resDir;
        searchDirs = Optional.ofNullable(searchDirs).orElse(Collections.emptyList());
        for (String searchPath : searchDirs) {
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

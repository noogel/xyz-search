package noogel.xyz.search.infrastructure.model;

import co.elastic.clients.elasticsearch._types.mapping.*;
import lombok.Data;
import noogel.xyz.search.infrastructure.dto.TaskDto;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.infrastructure.utils.MD5Helper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Data
public class ResourceModel {

    /**
     * 资源完整路径 HASH
     */
    private String resId;
    /**
     * 资源名称
     */
    private String resName;
    /**
     * 资源路径
     */
    private String resPath;
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


    /**
     * 任务 ID
     */
    private Long taskId;
    /**
     * 任务操作时间
     */
    private Long taskOpAt;

    public static Map<String, Property> generateEsMapping() {
        Map<String, Property> documentMap = new HashMap<>();
        documentMap.put("resId", Property
                .of(p -> p.keyword(KeywordProperty.of(i -> i.index(true)))));
        documentMap.put("resPath", Property
                .of(p -> p.text(TextProperty.of(i -> i.index(true)))));
        documentMap.put("resName", Property
                .of(p -> p.text(TextProperty.of(i -> i.index(true)))));
        documentMap.put("resHash", Property
                .of(p -> p.keyword(KeywordProperty.of(i -> i.index(true)))));
        documentMap.put("resType", Property
                .of(p -> p.keyword(KeywordProperty.of(i -> i.index(true)))));
        documentMap.put("resSize", Property
                .of(p -> p.long_(LongNumberProperty.of(i -> i.index(true)))));
        documentMap.put("modifiedAt", Property
                .of(p -> p.long_(LongNumberProperty.of(i -> i.index(true)))));

        documentMap.put("searchableText", Property
                .of(p -> p.text(TextProperty.of(i -> i.index(true).analyzer("smartcn")))));
        documentMap.put("textHash", Property
                .of(p -> p.keyword(KeywordProperty.of(i -> i.index(true)))));
        documentMap.put("textSize", Property
                .of(p -> p.integer(IntegerNumberProperty.of(i -> i.index(true)))));

        documentMap.put("taskId", Property
                .of(p -> p.long_(LongNumberProperty.of(i -> i.index(true)))));
        documentMap.put("taskOpAt", Property
                .of(p -> p.long_(LongNumberProperty.of(i -> i.index(true)))));
        return documentMap;
    }

    /**
     * 初始化基础信息
     * @param file
     * @return
     */
    public static ResourceModel buildBaseInfo(File file, String text, TaskDto task) {
        ResourceModel demo = new ResourceModel();
        demo.setResId(MD5Helper.getMD5(file.getAbsolutePath()));
        // 所在目录
        demo.setResPath(file.getParent());
        demo.setResName(file.getName());
        demo.setResType("FILE:" + FileHelper.getFileExtension(file.getName()).toUpperCase());
        demo.setResHash(MD5Helper.getMD5(file));
        demo.setResSize(file.length());
        demo.setModifiedAt(file.lastModified());

        demo.setSearchableText(text);
        demo.setTextHash(MD5Helper.getMD5(text));
        demo.setTextSize(text.length());

        demo.setTaskId(task.getTaskId());
        demo.setTaskOpAt(task.getTaskOpAt());

        return demo;
    }

    /**
     * 计算完整路径
     * @return
     */
    public String calculateFullPath() {
        return String.format("%s/%s", resPath, resName);
    }
}

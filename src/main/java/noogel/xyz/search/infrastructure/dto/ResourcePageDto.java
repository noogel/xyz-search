package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ResourcePageDto extends ResourceSimpleDto {
    /**
     * 资源路径
     */
    private String resPath;
    /**
     * 字与
     * DIR:DIR
     * FILE:EPUB
     * FILE:TXT
     */
    private String resType;

    /**
     * 文件类型
     * Content-Type(Mime-Type)
     */
    private String contentType;

    /**
     * 高亮 html
     */
    private String highlightHtml;
    /**
     * 是否支持查看
     */
    private boolean supportView;

}

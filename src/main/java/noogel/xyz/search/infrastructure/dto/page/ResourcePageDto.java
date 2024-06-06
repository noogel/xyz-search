package noogel.xyz.search.infrastructure.dto.page;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ResourcePageDto extends ResourceSimpleDto {

    /**
     * 资源路径
     */
    private String relativeResPath;
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
    /**
     * 查看 url
     */
    private String viewUrl;
    /**
     * 是否支持缩略图查看
     */
    private boolean supportThumbnailView;
    /**
     * 缩略图 url
     */
    private String thumbnailViewUrl;

    /**
     * 下载 url
     */
    private String downloadUrl;

    /**
     * 目录查看 url
     */
    private String dirViewUrl;

    /**
     * 内容片段
     */
    private String resTextSnippet;
}

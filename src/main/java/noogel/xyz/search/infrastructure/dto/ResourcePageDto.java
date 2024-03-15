package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
import java.util.Objects;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ResourcePageDto extends ResourceSimpleDto {
    /**
     * 资源路径
     */
    private String relativeResPath;
    /**
     * 资源相对目录
     */
    private String relativeResDir;
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
     * 计算查看 Url
     * @return
     */
    public String getViewUrl() {
        if (Objects.equals(resType, "FILE:PDF")) {
            return "/pdf.js/web/view?file=/file/view/" + getResId();
        } else if (Objects.equals(resType, "FILE:EPUB")) {
            return "/epub/web/view?book=" + getResId();
        } else if (List.of("FILE:MP4", "FILE:AVI", "FILE:MKV").contains(resType)) {
            return "/video/" + getResId();
        } else {
            return "/file/view/" + getResId();
        }
    }
}

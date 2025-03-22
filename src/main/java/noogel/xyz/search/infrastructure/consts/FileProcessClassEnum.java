package noogel.xyz.search.infrastructure.consts;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FileProcessClassEnum {
    VIDEO("视频"),
    IMAGE("图片"),
    TEXT("纯文本"),
    HTML("网页"),
    PDF("PDF"),
    EPUB("电子书"),
    OFFICE("办公文档"),
    OTHER("其它"),
    ;

    /**
     * 描述
     */
    private final String desc;
}

package noogel.xyz.search.infrastructure.consts;

import java.util.Arrays;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FileExtEnum {
    EPUB,
    HTML,
    XHTML,
    XML,
    TXT,
    PDF,
    MP4,
    DOCX,
    DOC,
    XLSX,
    XLS,
    AVI,
    MKV,
    JPEG,
    PSD,
    JFIF,
    SVG,
    SGI,
    JPG,
    PNG,
    WEBP,
    BMP,
    TIFF,
    HTM,
    HEIF,
    HEIC,
    MOBI,
    AZW3,
    AZW,
    CSV,
    MD,
    UNKNOWN_IGNORE,
    ;

    @Nonnull
    public static FileExtEnum parse(String val) {
        return Arrays.stream(values()).filter(t -> t.name().equalsIgnoreCase(val))
                .findFirst().orElse(UNKNOWN_IGNORE);
    }
}

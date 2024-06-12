package noogel.xyz.search.infrastructure.consts;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Arrays;

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
    AVI,
    MKV,
    JPEG,
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

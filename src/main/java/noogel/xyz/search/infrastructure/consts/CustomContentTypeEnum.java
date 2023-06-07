package noogel.xyz.search.infrastructure.consts;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum CustomContentTypeEnum {
    LRF("application-lrf"),
//    LRF("text-lrs"),
    MOBI("application-x-mobipocket-ebook"),
    TPZ("application-x-topaz-ebook"),
    AZW2("application-x-kindle-application"),
    AZW3("application-x-mobi8-ebook"),
    ;

    private String contentType;

    public static Optional<CustomContentTypeEnum> findByExt(String ext) {
        for (CustomContentTypeEnum value : values()) {
            if (value.name().equalsIgnoreCase(ext)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

}

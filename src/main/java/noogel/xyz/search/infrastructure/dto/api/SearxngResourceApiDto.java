package noogel.xyz.search.infrastructure.dto.api;

import lombok.Data;

@Data
public class SearxngResourceApiDto implements ResourceApiDto {
    private String resId;
    private String title;
    private String content;
    private String url;
}

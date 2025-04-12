package noogel.xyz.search.infrastructure.dto.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileResContentDto {
    public static final String META_TITLE = "title";
    public static final String META_CONTENT_TYPE = "content_type";
    private List<ChapterDto> chapterList;
    private Map<String, String> metadata;

    public static FileResContentDto of(List<ChapterDto> chapterList) {
        FileResContentDto dto = new FileResContentDto();
        dto.setChapterList(chapterList);
        dto.setMetadata(new HashMap<>());
        return dto;
    }

    public FileResContentDto metaData(Map<String, String> metadata) {
        if (metadata == null) {
            return this;
        }
        metadata.forEach(this::metaData);
        return this;
    }

    public FileResContentDto metaData(String key, String val) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(val)) {
            return this;
        }
        this.metadata.put(key, val);
        return this;
    }

    public String genContent() {
        StringBuilder sb = new StringBuilder(estimateContentLength() + 1024);

        for (ChapterDto dto : chapterList) {
            sb.append(dto.getContent());
        }
        return sb.toString();
    }

    private int estimateContentLength() {
        return chapterList.stream()
                .mapToInt(chapter -> chapter.getContent() != null ? chapter.getContent().length() : 0)
                .sum();
    }
}

package noogel.xyz.search.infrastructure.dto.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileResContentDto {
    private List<ChapterDto> chapterList;
    private Map<String, String> metadata;

    public static FileResContentDto of(List<ChapterDto> chapterList) {
        FileResContentDto dto = new FileResContentDto();
        dto.setChapterList(chapterList);
        dto.setMetadata(new HashMap<>());
        return dto;
    }

    public FileResContentDto metaData(String key, String val) {
        this.metadata.put(key, val);
        return this;
    }

    public String genContent() {
        StringBuilder sb = new StringBuilder();
        for (ChapterDto dto : chapterList) {
            sb.append(dto.getContent());
        }
        return sb.toString();
    }
}

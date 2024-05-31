package noogel.xyz.search.infrastructure.dto.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class FileResContentDto {
    private List<ChapterDto> chapterList;
    private String metaTitle;

    public String genContent() {
        StringBuilder sb = new StringBuilder();
        for (ChapterDto dto : chapterList) {
            sb.append(dto.getText());
        }
        return sb.toString();
    }
}

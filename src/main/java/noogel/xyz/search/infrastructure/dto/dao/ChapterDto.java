package noogel.xyz.search.infrastructure.dto.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class ChapterDto {
    private String chapterName;
    private String content;
}

package noogel.xyz.search.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;

@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class IndexedContentDto {
    /**
     * 资源ID
     */
    private String resId;
    /**
     * 资源内容
     */
    private FileResContentDto content;
}

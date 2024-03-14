package noogel.xyz.search.infrastructure.dto.dropzone;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class UploadRespDto {
    private String error;
}

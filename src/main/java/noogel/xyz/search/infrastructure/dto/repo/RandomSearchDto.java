package noogel.xyz.search.infrastructure.dto.repo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class RandomSearchDto {
    private Integer limit = 20;
}

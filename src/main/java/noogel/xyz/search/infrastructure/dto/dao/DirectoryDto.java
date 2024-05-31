package noogel.xyz.search.infrastructure.dto.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class DirectoryDto {
    private String path;

    public int calDirDep() {
        return path.split("/").length;
    }
}

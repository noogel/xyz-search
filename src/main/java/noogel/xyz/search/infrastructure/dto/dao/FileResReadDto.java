package noogel.xyz.search.infrastructure.dto.dao;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FileResReadDto extends FileResWriteDto {

    /**
     * 文件ID
     */
    private Long fieldId;
    /**
     * 文件状态
     */
    private Integer state;

}

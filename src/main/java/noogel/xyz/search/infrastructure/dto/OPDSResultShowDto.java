package noogel.xyz.search.infrastructure.dto;

import lombok.Data;

import java.util.List;

@Data
public class OPDSResultShowDto {
    /**
     * opds 资源展示项
     */
    private List<OPDSItemShowDto> data;
    /**
     * 数量
     */
    private int size;

}

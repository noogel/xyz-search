package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import noogel.xyz.search.infrastructure.model.elastic.FileEsModel;

import java.util.List;

@Data
public class ResourceHighlightHitsDto {
    private FileEsModel resource;
    private List<String> highlights;
}

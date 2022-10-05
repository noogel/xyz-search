package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import noogel.xyz.search.infrastructure.model.ResourceModel;

import java.util.List;

@Data
public class ResourceHighlightHitsDto {
    private ResourceModel resource;
    private List<String> highlights;
}

package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import noogel.xyz.search.infrastructure.model.lucene.FullTextSearchModel;

import java.util.List;

@Data
public class ResourceHighlightHitsDto {
    private FullTextSearchModel resource;
    private List<String> highlights;
}

package noogel.xyz.search.infrastructure.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class OPDSResMetaDataDto {
    private String id;
    private String uuid;
    private String title;
    private List<String> creator;
    private String contributor;
    private String date;
    private String publisher;
    private String ISBN;
    private String mobiASIN;
    private String language;
    private String cover;
    private String description;
    private String subject;
    private Map<String, String> meta;
    private String absoluteDir;
    private Object source;
}

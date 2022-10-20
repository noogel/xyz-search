package noogel.xyz.search.infrastructure.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import javax.annotation.Nullable;
import java.io.IOException;

public class XmlHelper {
    private static final XmlMapper XML_MAPPER = new XmlMapper();

    @Nullable
    public static JsonNode parseXml(String xml) throws IOException {
        return XML_MAPPER.readTree(xml.getBytes());
    }
}

package noogel.xyz.search.infrastructure.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class XmlHelper {
    private static final XmlMapper XML_MAPPER = new XmlMapper();

    @Nullable
    public static JsonNode parseXml(String xml) throws IOException {
        return XML_MAPPER.readTree(xml.getBytes());
    }
}

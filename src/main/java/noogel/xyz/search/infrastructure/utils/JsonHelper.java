package noogel.xyz.search.infrastructure.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.SneakyThrows;

import java.util.Map;


public class JsonHelper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Nullable
    @SneakyThrows
    public static String toJson(Object obj) {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }

    @Nullable
    @SneakyThrows
    public static <T> T fromJson(String json, Class<T> clazz) {
        return OBJECT_MAPPER.readValue(json, clazz);
    }

    @Nullable
    @SneakyThrows
    public static Map<String, String> fromJson(String json) {
        return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, String>>() {
        });
    }
}

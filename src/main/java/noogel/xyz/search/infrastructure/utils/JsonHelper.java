package noogel.xyz.search.infrastructure.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import javax.annotation.Nullable;


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
}

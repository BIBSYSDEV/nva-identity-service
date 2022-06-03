package no.unit.nva.identityservice.json;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;

public final class JsonConfig {

    private static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;

    private JsonConfig() {

    }

    public static <T> T readValue(String source, Class<T> type) throws IOException {
        return objectMapper.readValue(source, type);
    }

    public static <T> T readValue(String source, TypeReference<T> type) throws IOException {
        return objectMapper.readValue(source, type);
    }

    public static <T> T readValue(String source, JavaType type) throws IOException {
        return objectMapper.readValue(source, type);
    }

    public static <T> String writeValueAsString(T object) throws IOException {
        return objectMapper.writeValueAsString(object);
    }

    public static Map<String, Object> mapFrom(String source) throws IOException {
        var mapType = objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
        return objectMapper.readValue(source, mapType);
    }

    public static <T> List<T> listOfFrom(Class<T> type, String source) throws IOException {
        var listType = objectMapper.getTypeFactory().constructCollectionType(List.class, type);
        return objectMapper.readValue(source, listType);
    }

    public static TypeFactory getTypeFactory() {
        return objectMapper.getTypeFactory();
    }

    @Deprecated(since = "We rolled back to using normal jackson")
    public static String instantToString(Instant instant) {
        return nonNull(instant) ? instant.toString() : null;
    }

    @Deprecated(since = "We rolled back to using normal jackson")
    public static Instant stringToInstant(String instant) {
        return nonNull(instant) ? Instant.parse(instant) : null;
    }
}

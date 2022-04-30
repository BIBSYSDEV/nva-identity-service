package no.unit.nva.identityservice.json;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.jr.annotationsupport.JacksonAnnotationExtension;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class JsonConfig {

    private static final JSON objectMapper = JSON.builder()
        .register(JacksonAnnotationExtension.std)
        .disable(Feature.PRESERVE_FIELD_ORDERING)
        .build();

    private JsonConfig() {

    }

    public static <T> T beanFrom(Class<T> type,String source) throws IOException {
        return objectMapper.beanFrom(type, source);
    }

    public static <T> String asString(T object) throws IOException {
        return objectMapper.asString(object);
    }

    public static Map<String, Object> mapFrom(String source) throws IOException {
        return objectMapper.mapFrom(source);
    }

    public static <T> List<T> listOfFrom(Class<T> type, String source) throws IOException {
        return objectMapper.listOfFrom(type,source);
    }

    public static String instantToString(Instant createdDate) {
        return nonNull(createdDate) ? createdDate.toString() : null;
    }

    public static Instant stringToInstant(String createdDate) {
        return nonNull(createdDate) ? Instant.parse(createdDate) : null;
    }

}

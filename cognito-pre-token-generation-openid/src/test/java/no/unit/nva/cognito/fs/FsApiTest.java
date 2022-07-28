package no.unit.nva.cognito.fs;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import no.unit.nva.cognito.UserSelectionUponLoginHandler;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Path;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FsApiTest {

    private static final String EXAMPLE_COM_URI = "http://example.com";
    private static final String CONTENT_TYPE = "Content-type";
    private static final String APPLICATION_JSON_CONTENT_TYPE_VALUE = "application/json";
    private static final String SUPPORTED_NIN = "04127048571";
    private static final String PERSON_PATH = "personer";
    private static final String STUDENTUNDERVISNING_PATH = "studentundervisning";
    private static final String UNDERVISNING_SEMESTER_AR_PATH = "undervisning.semester.ar";
    private static final String PERSON_PERSONLOPENUMMER_PATH = "person.personlopenummer";
    private static final String OPPTATT_PATH = "opptatt";
    private static final String DB_IDENTIFIER = "dbId";
    private static final String LIMIT_IDENTIFIER = "limit";
    private static final String FIELDS_IDENTIFIER = "fields";
    public static final String FODSELSDATO_IDENTIFIER = "fodselsdato0";
    public static final String PERSONNUMMER_IDENTIFIER = "personnummer0";
    public static final String AUTHORIZATION = "Authorization";
    public static final String AUTHORIZATION_HEADER_VALUE = "Basic Ymlic3lzX29zbG9tZXQyOm1zUDNFS0FjcjhFdHBVa1I=";

    public static final ObjectMapper restApiMapper = JsonUtils.dtoObjectMapper;
    private ByteArrayOutputStream output;
    private UserSelectionUponLoginHandler handler;
    private Context context;


    void shouldReturnAllCoursesOfTeacherOfCurrentYearWheInputIsNinOfSomeTeacher(){

    }

    @Test
    void shouldReturnLopenummerToPersonUsingNin() throws IOException {
        final InputStream input = createRequest(SUPPORTED_NIN);

        final String responseBody = IoUtils.stringFromResources(Path.of("./fs/fsStudentResponse.json"));
        stubRequestForFsPerson("22099623945", responseBody);
        final var gatewayResponse = GatewayResponse.fromOutputStream(output, FsApiPersonResponse.class);
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());

    }

    private InputStream createRequest(String personPath)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        final URI topLevelCristinOrgId =
                UriWrapper.fromUri(EXAMPLE_COM_URI).addChild(personPath).getUri();
        return new HandlerRequestBuilder<Void>(restApiMapper)
                .withTopLevelCristinOrgId(topLevelCristinOrgId)
                .build();
    }

    private void stubRequestForFsPerson(final String nin, final String responseBody) {
        stubFor(get(urlPathEqualTo(PERSON_PATH))
                .withHeader(AUTHORIZATION, equalTo(AUTHORIZATION_HEADER_VALUE))
                .withQueryParam(DB_IDENTIFIER, equalTo("true"))
                .withQueryParam(LIMIT_IDENTIFIER,equalTo("0"))
                .withQueryParam(FIELDS_IDENTIFIER, equalTo("*"))
                .withQueryParam(FODSELSDATO_IDENTIFIER, equalTo(nin.substring(0,6)))
                .withQueryParam(PERSONNUMMER_IDENTIFIER, equalTo(nin.substring(6)))
                .willReturn(WireMock.ok()
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_CONTENT_TYPE_VALUE)
                        .withBody(responseBody)));
    }
}

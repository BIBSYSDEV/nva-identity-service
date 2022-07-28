package no.unit.nva.cognito.fs;

import no.unit.nva.commons.json.JsonUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Date;

import nva.commons.core.paths.UriWrapper;
import org.joda.time.DateTime;


public class FsApi {

    private final String baseUri;
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
    public static final String PROBLEMS_READING_RESPONSE_FROM_SERVER_MESSAGE = "Problems reading response from server";
    public static final String PROBLEMS_COMMUNICATING_WITH_SERVER_MESSAGE = "Problems communicating with server";
    private String username;
    private String password;
    private final HttpClient httpClient;


    public FsApi(String baseUri, String username, String password) {
        this.baseUri = baseUri;
        this.username = username;
        this.password = password;
        this.httpClient = HttpClient.newBuilder().build();
    }

    public FsApiPersonResponse getLopenummerToPersonFromFs(String nin) throws Exception {
        final String fodselsdato = nin.substring(0,6);
        final String personnummer = nin.substring(6);

        final URI uri = UriWrapper.fromUri(baseUri)
                .addChild(PERSON_PATH)
                .addQueryParameter(DB_IDENTIFIER, "true")
                .addQueryParameter(LIMIT_IDENTIFIER, Integer.toString(0))
                .addQueryParameter(FIELDS_IDENTIFIER, "*")
                .addQueryParameter(FODSELSDATO_IDENTIFIER, fodselsdato)
                .addQueryParameter(PERSONNUMMER_IDENTIFIER, personnummer)
                .getUri();

        final HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .setHeader("Authorization", getBasicAuthenticationHeader())
                .build();

        final HttpResponse<byte[]> response = sendRequest(request);
        final int statusCode = response.statusCode();
        if (statusCode == HttpURLConnection.HTTP_OK) {
            return convertResponseBodyToPerson(response.body());
        } else {
            throw new Exception(String.valueOf(statusCode));
        }
    }

    public FsApiCoursesResponse getCoursesToPersonFromFs(String nin) throws Exception {
        final String year = String.valueOf(new DateTime().getYear());
        final String lopenummer = getLopenummerToPersonFromFs(nin).getItems().get(0).getId().getPersonlopenummer().toString();

        final URI uri = UriWrapper.fromUri(baseUri)
                .addChild(STUDENTUNDERVISNING_PATH)
                .addQueryParameter(DB_IDENTIFIER, "true")
                .addQueryParameter(LIMIT_IDENTIFIER, Integer.toString(0))
                .addQueryParameter(UNDERVISNING_SEMESTER_AR_PATH, year)
                .addQueryParameter(PERSON_PERSONLOPENUMMER_PATH, lopenummer)
                .addQueryParameter(OPPTATT_PATH, "true")
                .getUri();

        final HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .setHeader("Authorization", getBasicAuthenticationHeader())
                .build();

        final HttpResponse<byte[]> response = sendRequest(request);
        final int statusCode = response.statusCode();
        if (statusCode == HttpURLConnection.HTTP_OK) {
            return convertResponseBodyToCourses(response.body());
        } else {
            throw new Exception(String.valueOf(statusCode));
        }
    }

    private FsApiPersonResponse convertResponseBodyToPerson(byte[] body) throws Exception {
        try {
            return JsonUtils.dtoObjectMapper.readValue(body, FsApiPersonResponse.class);
        } catch (IOException e) {
            throw new Exception(PROBLEMS_READING_RESPONSE_FROM_SERVER_MESSAGE);
        }
    }

    private FsApiCoursesResponse convertResponseBodyToCourses(byte[] body) throws Exception {
        try {
            return JsonUtils.dtoObjectMapper.readValue(body, FsApiCoursesResponse.class);
        } catch (IOException e) {
            throw new Exception(PROBLEMS_READING_RESPONSE_FROM_SERVER_MESSAGE);
        }
    }

    private HttpResponse<byte[]> sendRequest(HttpRequest request) throws Exception {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            throw new Exception(PROBLEMS_COMMUNICATING_WITH_SERVER_MESSAGE);
        }
    }

    private String getBasicAuthenticationHeader() {
        final String valueToEncode = this.username + ":" + this.password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }

}

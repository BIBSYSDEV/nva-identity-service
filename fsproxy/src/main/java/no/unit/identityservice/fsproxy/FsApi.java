package no.unit.identityservice.fsproxy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.Base64;
import java.util.List;
import no.unit.identityservice.fsproxy.model.FsCourseData;
import no.unit.identityservice.fsproxy.model.FsCoursesSearchResult;
import no.unit.identityservice.fsproxy.model.FsIdNumber;
import no.unit.identityservice.fsproxy.model.FsNin;
import no.unit.identityservice.fsproxy.model.FsPersonSearchResponse;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.joda.time.DateTime;

@SuppressWarnings("PMD")

public class FsApi {

    public static final String FODSELSDATO_IDENTIFIER = "fodselsdato0";
    public static final String PERSONNUMMER_IDENTIFIER = "personnummer0";
    private static final String PERSON_PATH = "personer";
    private static final String STUDENTUNDERVISNING_PATH = "studentundervisning";
    private static final String UNDERVISNING_SEMESTER_AR_PATH = "undervisning.semester.ar";
    private static final String PERSON_PERSONLOPENUMMER_PATH = "person.personlopenummer";
    private static final String DB_IDENTIFIER = "dbId";
    private static final String LIMIT_IDENTIFIER = "limit";
    private static final String LIMIT_VALUE = "0";
    private static final String FS_HOST = "FS_HOST";
    private final String username = new Environment().readEnv("FS_USERNAME");
    private final String password = new Environment().readEnv("FS_PASSWORD");

    private final HttpClient httpClient;
    private final String baseFsHostUrl;

    public FsApi(HttpClient httpClient, String baseFsHostUrl) {
        this.httpClient = httpClient;
        this.baseFsHostUrl = baseFsHostUrl;
    }

    @JacocoGenerated
    public FsApi() {
        this(HttpClient.newBuilder().build(), FS_HOST);
    }

    public FsIdNumber getFsId(FsNin nationalIdentityNumber) throws IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder().build();
        var httpRequest = HttpRequest.newBuilder(createSearchPersonUri(nationalIdentityNumber))
                              .header("Authorization", getBasicAuthenticationHeader())
                              .GET().build();

        var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        var responseBody = response.body();
        var fsIdSearchResult = JsonUtils.dtoObjectMapper.readValue(responseBody, FsPersonSearchResponse.class);

        if (fsIdSearchResult.getSearchResults().size() == 0) {
            throw new UserPrincipalNotFoundException(nationalIdentityNumber.toString());
        } else {

            return fsIdSearchResult.getSearchResults().get(0).getFsPerson().getFsIdNumber();
        }
    }

    public List<FsCourseData> getCourses(FsIdNumber fsIdNumber) throws IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder().build();
        var httpRequest = HttpRequest.newBuilder(createSearchCourseUri(fsIdNumber))
                              .header("Authorization", getBasicAuthenticationHeader())
                              .GET().build();

        var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        var responseBody = response.body();
        var fsCoursesSearchResult = JsonUtils.dtoObjectMapper.readValue(responseBody, FsCoursesSearchResult.class);
        return fsCoursesSearchResult.getFsCourseData();
    }

    private URI createSearchPersonUri(FsNin nin) {
        return UriWrapper.fromUri(baseFsHostUrl)
                   .addChild(PERSON_PATH)
                   .addQueryParameter(DB_IDENTIFIER, "true")
                   .addQueryParameter(LIMIT_IDENTIFIER, LIMIT_VALUE)
                   .addQueryParameter(FODSELSDATO_IDENTIFIER, nin.getBirthDate())
                   .addQueryParameter(PERSONNUMMER_IDENTIFIER, nin.getPersonalNumber())
                   .getUri();
    }

    private URI createSearchCourseUri(FsIdNumber FsIdNumber) {
        final String year = String.valueOf(new DateTime().getYear());
        return UriWrapper.fromUri(baseFsHostUrl)
                   .addChild(STUDENTUNDERVISNING_PATH)
                   .addQueryParameter(DB_IDENTIFIER, "true")
                   .addQueryParameter(LIMIT_IDENTIFIER, LIMIT_VALUE)
                   .addQueryParameter(PERSON_PERSONLOPENUMMER_PATH, FsIdNumber.toString())
                   .addQueryParameter(UNDERVISNING_SEMESTER_AR_PATH, year)
                   .getUri();
    }

    private String getBasicAuthenticationHeader() {
        final String valueToEncode = this.username + ":" + this.password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }
}

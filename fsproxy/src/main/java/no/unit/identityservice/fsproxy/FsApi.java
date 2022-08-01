package no.unit.identityservice.fsproxy;

import no.unit.identityservice.fsproxy.model.FsIdNumber;
import no.unit.identityservice.fsproxy.model.FsNin;
import no.unit.identityservice.fsproxy.model.FsPersonSearchResponse;
import no.unit.identityservice.fsproxy.model.FsCourseData;
import no.unit.identityservice.fsproxy.model.FsCoursesSearchResult;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.joda.time.DateTime;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;


public class FsApi {

    public static final String FODSELSDATO_IDENTIFIER = "fodselsdato0";
    public static final String PERSONNUMMER_IDENTIFIER = "personnummer0";
    public static final String FS_HOST = "api.fellesstudentsystem.no";
    private static final String PERSON_PATH = "personer";
    private static final String STUDENTUNDERVISNING_PATH = "studentundervisning";
    private static final String UNDERVISNING_SEMESTER_AR_PATH = "undervisning.semester.ar";
    private static final String PERSON_PERSONLOPENUMMER_PATH = "person.personlopenummer";
    private static final String DB_IDENTIFIER = "dbId";
    private static final String LIMIT_IDENTIFIER = "limit";
    private final String username = new Environment().readEnv("FS_USERNAME");
    private final String password = new Environment().readEnv("FS_PASSWORD");

    public FsIdNumber getFsId(FsNin nationalIdentityNumber) throws IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder().build();
        var httpRequest = HttpRequest.newBuilder(createSearchPersonUri(nationalIdentityNumber))
                .header("Authorization", getBasicAuthenticationHeader())
                .GET().build();

        var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        var responseBody = response.body();
        var fsIdSearchResult = JsonUtils.dtoObjectMapper.readValue(responseBody, FsPersonSearchResponse.class);
        return fsIdSearchResult.getSearchResults().get(0).getFsPerson().getFsIdNumber();
    }

    public List<FsCourseData> getCourses(FsNin nationalIdentityNumber) throws IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder().build();
        var httpRequest = HttpRequest.newBuilder(createSearchCourseUri(nationalIdentityNumber))
                .header("Authorization", getBasicAuthenticationHeader())
                .GET().build();

        var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        var responseBody = response.body();
        var fsCoursesSearchResult = JsonUtils.dtoObjectMapper.readValue(responseBody, FsCoursesSearchResult.class);
        return fsCoursesSearchResult.getFsCourseData();
    }

    private URI createSearchPersonUri(FsNin nin) {
        return UriWrapper.fromHost(FS_HOST)
                .addChild(PERSON_PATH)
                .addQueryParameter(DB_IDENTIFIER, "true")
                .addQueryParameter(LIMIT_IDENTIFIER, "0")
                .addQueryParameter(FODSELSDATO_IDENTIFIER, nin.getBirthDate())
                .addQueryParameter(PERSONNUMMER_IDENTIFIER, nin.getPersonalNumber())
                .getUri();

    }

    private URI createSearchCourseUri(FsNin nin) throws IOException, InterruptedException {
        final String year = String.valueOf(new DateTime().getYear());
        final String lopenummer = getFsId(nin).toString();

        return UriWrapper.fromHost(FS_HOST)
                .addChild(STUDENTUNDERVISNING_PATH)
                .addQueryParameter(DB_IDENTIFIER, "true")
                .addQueryParameter(LIMIT_IDENTIFIER, "0")
                .addQueryParameter(PERSON_PERSONLOPENUMMER_PATH, lopenummer)
                .addQueryParameter(UNDERVISNING_SEMESTER_AR_PATH, year)
                .getUri();
    }

    private String getBasicAuthenticationHeader() {
        final String valueToEncode = this.username + ":" + this.password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }


}

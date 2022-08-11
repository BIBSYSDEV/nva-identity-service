package no.unit.identityservice.fsproxy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.time.Year;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.identityservice.fsproxy.model.course.FsCourse;
import no.unit.identityservice.fsproxy.model.course.FsCoursesSearchResult;
import no.unit.identityservice.fsproxy.model.fagperson.FsCourseActivity;
import no.unit.identityservice.fsproxy.model.fagperson.FsRoleToStaffPerson;
import no.unit.identityservice.fsproxy.model.fagperson.FsRolesToPersonSearchResult;
import no.unit.identityservice.fsproxy.model.fagperson.FsUriToCourseActivity;
import no.unit.identityservice.fsproxy.model.fagperson.FsUriToCourseActivityContainer;
import no.unit.identityservice.fsproxy.model.person.FsIdNumber;
import no.unit.identityservice.fsproxy.model.person.FsPersonSearchResponse;
import no.unit.identityservice.fsproxy.model.person.NationalIdentityNumber;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class FsApi {

    public static final String FODSELSDATO_IDENTIFIER = "fodselsdato0";
    public static final String PERSONNUMMER_IDENTIFIER = "personnummer0";
    public static final String PERSONROLLER_PATH = "personroller";
    public static final String SEMESTER_AR_PATH = "semester.ar";
    public static final String UNDERVISNINGSAKTIVITETER_PATH = "undervisningsaktiviteter";
    public static final String PERSON_PATH = "personer";
    public static final String STUDENTUNDERVISNING_PATH = "studentundervisning";
    public static final String UNDERVISNING_SEMESTER_AR_PATH = "undervisning.semester.ar";
    public static final String PERSON_PERSONLOPENUMMER_PATH = "person.personlopenummer";
    public static final String DB_IDENTIFIER = "dbId";
    public static final String LIMIT_IDENTIFIER = "limit";
    public static final String LIMIT_VALUE = "0";
    private static final Environment ENVIRONMENT = new Environment();
    public static final URI FS_HOST = readFsHost();
    private final String username = ENVIRONMENT.readEnv("FS_USERNAME");
    private final String password = ENVIRONMENT.readEnv("FS_PASSWORD");
    private final URI baseFsHostUrl;

    @SuppressWarnings("PMD")
    public FsApi(HttpClient httpClient, URI baseFsHostUrl) {
        this.baseFsHostUrl = baseFsHostUrl;
    }

    @JacocoGenerated
    public FsApi() {
        this(HttpClient.newBuilder().build(), FS_HOST);
    }

    public FsIdNumber getFsId(NationalIdentityNumber nin) throws IOException, InterruptedException {

        var response = getResponse(createSearchPersonUri(nin));
        var responseBody = response.body();
        var fsIdSearchResult = JsonUtils.dtoObjectMapper.readValue(responseBody, FsPersonSearchResponse.class);

        if (fsIdSearchResult.getSearchResults().size() == 0) {
            throw new UserPrincipalNotFoundException(nin.toString());
        } else {
            return fsIdSearchResult.getSearchResults().get(0).getFsPerson().getFsIdNumber();
        }
    }

    @JacocoGenerated
    public List<FsCourse> getCourses(NationalIdentityNumber nin) throws IOException, InterruptedException {
        var fsId = getFsId(nin);
        var coursesIfStudent = getCoursesToStudent(fsId);
        var roles = getRolesToStaffPerson(fsId);
        var coursesUri = roles.stream().map(role -> {
            try {
                return getCourseUriToGivenRole(role);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        var coursesIfStaff = coursesUri.stream().map(uri -> {
            try {
                return getCourseToStaffPersonGivenUriToCourse(uri);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        return Stream.concat(coursesIfStaff.stream(), coursesIfStudent.stream()).collect(Collectors.toList());
    }

    public List<FsCourse> getCoursesToStudent(FsIdNumber fsIdNumber) throws IOException, InterruptedException {

        var response = getResponse(createSearchCourseUri(fsIdNumber));
        var responseBody = response.body();
        var fsCoursesSearchResult = JsonUtils.dtoObjectMapper.readValue(responseBody, FsCoursesSearchResult.class);
        return fsCoursesSearchResult.getItems()
                          .stream()
                          .map(c -> c.getId().getCourse())
                          .collect(Collectors.toList());
    }

    public Boolean getFsPersonStatus(NationalIdentityNumber nin) throws IOException, InterruptedException {
        var response = getResponse(createSearchPersonUri(nin));
        var responseBody = response.body();
        var fsIdSearchResult = JsonUtils.dtoObjectMapper.readValue(responseBody, FsPersonSearchResponse.class);

        return fsIdSearchResult.getSearchResults().get(0).getFsPerson().getStaffPerson().isEmpty();
    }

    public List<FsRoleToStaffPerson> getRolesToStaffPerson(FsIdNumber fsIdNumber)
        throws IOException, InterruptedException {

        var response = getResponse(createSearchRolesToStaffPersonUri(fsIdNumber));
        var responseBody = response.body();
        var fsRolesSearchResult = JsonUtils.dtoObjectMapper.readValue(responseBody, FsRolesToPersonSearchResult.class);

        return fsRolesSearchResult.getItems();
    }

    public FsUriToCourseActivity getCourseUriToGivenRole(FsRoleToStaffPerson role)
        throws IOException, InterruptedException {
        var response = getResponse(createSearchCourseToRoleUri(role.getUriToRole()));
        var responseBody = response.body();
        var fsCourse = JsonUtils.dtoObjectMapper.readValue(responseBody, FsUriToCourseActivityContainer.class);

        return fsCourse.getCourseUri();
    }

    public FsCourse getCourseToStaffPersonGivenUriToCourse(FsUriToCourseActivity course)
        throws IOException, InterruptedException {
        var response = getResponse(createSearchCourseForStaffPersonUri(course.getUri()));
        var responseBody = response.body();
        var fsCourse = JsonUtils.dtoObjectMapper.readValue(responseBody, FsCourseActivity.class);

        return fsCourse.getCourse();
    }

    public HttpResponse<String> getResponse(URI uri) throws IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder().build();
        var httpRequest = HttpRequest.newBuilder(uri)
                              .header("Authorization", getBasicAuthenticationHeader())
                              .GET()
                              .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    private static URI readFsHost() {
        var hostUriString = ENVIRONMENT.readEnv("FS_HOST");
        return URI.create(hostUriString);
    }

    private URI createSearchPersonUri(NationalIdentityNumber nin) {
        return UriWrapper.fromUri(baseFsHostUrl)
                   .addChild(PERSON_PATH)
                   .addQueryParameter(DB_IDENTIFIER, "true")
                   .addQueryParameter(LIMIT_IDENTIFIER, LIMIT_VALUE)
                   .addQueryParameter(FODSELSDATO_IDENTIFIER, nin.getBirthDate())
                   .addQueryParameter(PERSONNUMMER_IDENTIFIER, nin.getPersonalNumber())
                   .getUri();
    }

    @SuppressWarnings("PMD")
    private URI createSearchCourseUri(FsIdNumber FsIdNumber) {
        final String year = Year.now().toString();

        return UriWrapper.fromUri(baseFsHostUrl)
                   .addChild(STUDENTUNDERVISNING_PATH)
                   .addQueryParameter(DB_IDENTIFIER, "true")
                   .addQueryParameter(LIMIT_IDENTIFIER, LIMIT_VALUE)
                   .addQueryParameter(PERSON_PERSONLOPENUMMER_PATH, FsIdNumber.toString())
                   .addQueryParameter(UNDERVISNING_SEMESTER_AR_PATH, year)
                   .getUri();
    }

    private URI createSearchRolesToStaffPersonUri(FsIdNumber fsIdNumber) {
        final String year = Year.now().toString();

        return UriWrapper.fromUri(baseFsHostUrl)
                   .addChild(PERSONROLLER_PATH)
                   .addQueryParameter(PERSON_PERSONLOPENUMMER_PATH, fsIdNumber.toString())
                   .addQueryParameter(SEMESTER_AR_PATH, year)
                   .getUri();
    }

    private URI createSearchCourseToRoleUri(String href) {
        return UriWrapper.fromUri(baseFsHostUrl).addChild(PERSONROLLER_PATH).addChild(href).getUri();
    }

    private URI createSearchCourseForStaffPersonUri(String href) {
        return UriWrapper.fromUri(baseFsHostUrl).addChild(UNDERVISNINGSAKTIVITETER_PATH).addChild(href).getUri();
    }

    private String getBasicAuthenticationHeader() {
        final String valueToEncode = this.username + ":" + this.password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }
}

package no.unit.identityservice.fsproxy;

import no.unit.identityservice.fsproxy.model.course.FsCourse;
import no.unit.identityservice.fsproxy.model.course.FsCoursesSearchResult;
import no.unit.identityservice.fsproxy.model.person.FsIdNumber;
import no.unit.identityservice.fsproxy.model.person.FsPersonSearchResponse;
import no.unit.identityservice.fsproxy.model.person.NationalIdentityNumber;
import no.unit.identityservice.fsproxy.model.staffperson.FsCourseActivity;
import no.unit.identityservice.fsproxy.model.staffperson.FsRoleToStaffPerson;
import no.unit.identityservice.fsproxy.model.staffperson.FsRolesToPersonSearchResult;
import no.unit.identityservice.fsproxy.model.staffperson.FsUriToCourseActivity;
import no.unit.identityservice.fsproxy.model.staffperson.FsUriToCourseActivityContainer;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.time.Year;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nva.commons.core.attempt.Try.attempt;

public class FsApi {

    public static final String BIRTHDATE_IDENTIFIER = "fodselsdato0";
    public static final String PERSONAL_NUMBER_IDENTIFIER = "personnummer0";
    public static final String PERSON_ROLES = "personroller";
    public static final String SEMESTER_YEAR_PATH = "semester.ar";
    public static final String PERSON_PATH = "personer";
    public static final String STUDENT_TEACHING_PATH = "studentundervisning";
    public static final String TEACHING_SEMESTER_AR_PATH = "undervisning.semester.ar";
    public static final String FS_PERSON_ID_NUMBER_PATH = "person.personlopenummer";
    public static final String DB_IDENTIFIER = "dbId";
    public static final String LIMIT_IDENTIFIER = "limit";
    public static final String LIMIT_VALUE = "0";
    public static final String FETCH_FS_USER_EXCEPTION_MESSAGE = "Not possible to fetch fs id-number to user "
            + "with following national number: ";
    public static final String COURSES_FETCHED_SUCCESSFULLY = "Courses fetched successfully to user with"
            + " id-number ";
    private static final Environment ENVIRONMENT = new Environment();
    public static final URI FS_HOST = readFsHost();
    private final String username = ENVIRONMENT.readEnv("FS_USERNAME");
    private final String password = ENVIRONMENT.readEnv("FS_PASSWORD");
    private final URI baseFsHostUrl;
    private final HttpClient httpClient;

    private final Logger logger = LoggerFactory.getLogger(FsApi.class);

    public FsApi(HttpClient httpClient, URI baseFsHostUrl) {
        this.baseFsHostUrl = baseFsHostUrl;
        this.httpClient = httpClient;
    }

    @JacocoGenerated
    public FsApi() {
        this(HttpClient.newBuilder().build(), FS_HOST);
    }

    private static URI readFsHost() {
        var hostUriString = ENVIRONMENT.readEnv("FS_HOST");
        return URI.create(hostUriString);
    }

    public List<FsCourse> fetchCoursesForPerson(NationalIdentityNumber nin) throws IOException, InterruptedException {
        var fsId = fetchFsId(nin);
        var coursesIfStaff = fetchCoursesForStaffPerson(fsId);
        var coursesIfStudent = fetchCoursesToStudent(fsId);
        logger.info(COURSES_FETCHED_SUCCESSFULLY + fsId);

        return Stream.concat(coursesIfStaff.stream(), coursesIfStudent.stream()).collect(Collectors.toList());
    }

    public FsCourse fetchCourseToStaffPersonGivenUriToCourse(FsUriToCourseActivity course)
            throws IOException, InterruptedException {
        var responseBody = getResponse(createSearchCourseForStaffPersonUri(course)).body();
        var fsCourse = FsCourseActivity.fromJson(responseBody);

        return fsCourse.getCourse();
    }

    public FsIdNumber fetchFsId(NationalIdentityNumber nin) throws IOException, InterruptedException {
        var responseBody = getResponse(createSearchPersonUri(nin)).body();
        var fsIdSearchResult = FsPersonSearchResponse.fromJson(responseBody);

        return fsIdSearchResult.getSearchResult()
                .orElseThrow(() -> new UserPrincipalNotFoundException(FETCH_FS_USER_EXCEPTION_MESSAGE + nin))
                .getFsPerson()
                .getFsIdNumber();
    }

    public List<FsCourse> fetchCoursesToStudent(FsIdNumber fsIdNumber) throws IOException, InterruptedException {
        var responseBody = getResponse(createSearchCourseUri(fsIdNumber)).body();
        var fsCoursesSearchResult = FsCoursesSearchResult.fromJson(responseBody);

        return fsCoursesSearchResult.getItems() == null ? Collections.emptyList() : fsCoursesSearchResult.getItems()
                .stream()
                .map(c -> c.getId().getCourse())
                .collect(Collectors.toList());
    }

    public List<FsRoleToStaffPerson> fetchRolesToStaffPerson(FsIdNumber fsIdNumber)
            throws IOException, InterruptedException {
        var responseBody = getResponse(createSearchRolesToStaffPersonUri(fsIdNumber)).body();
        var fsRolesSearchResult = FsRolesToPersonSearchResult.fromJson(responseBody);

        return fsRolesSearchResult == null ? Collections.emptyList() : fsRolesSearchResult.getItems();
    }

    public FsUriToCourseActivity fetchCourseActivityUriToGivenRole(FsRoleToStaffPerson role)
            throws IOException, InterruptedException {
        var responseBody = getResponse(createSearchCourseToRoleUri(role)).body();
        var fsCourse = FsUriToCourseActivityContainer.fromJson(responseBody);

        return fsCourse.getCourseUri();
    }

    private List<FsCourse> fetchCoursesForStaffPerson(FsIdNumber fsId) throws IOException, InterruptedException {
        var roles = fetchRolesToStaffPerson(fsId);
        var coursesUri = fetchAllCourseUrisBasedOnRolesOfStaffPerson(roles);

        return fetchCourseDetails(coursesUri);
    }

    private List<FsCourse> fetchCourseDetails(Stream<FsUriToCourseActivity> coursesUri) {
        return coursesUri.map(attempt(this::fetchCourseToStaffPersonGivenUriToCourse))
                .map(Try::orElseThrow)
                .collect(Collectors.toList());
    }

    private Stream<FsUriToCourseActivity> fetchAllCourseUrisBasedOnRolesOfStaffPerson(List<FsRoleToStaffPerson> roles) {
        if (roles == null) {
            return Stream.empty();
        }
        return roles.stream().map(attempt(this::fetchCourseActivityUriToGivenRole)).map(Try::orElseThrow);
    }

    private HttpResponse<String> getResponse(URI uri) throws IOException, InterruptedException {
        var httpRequest = HttpRequest.newBuilder(uri)
                .header("Authorization", getBasicAuthenticationHeader())
                .GET()
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    private URI createSearchPersonUri(NationalIdentityNumber nin) {
        return UriWrapper.fromUri(baseFsHostUrl)
                .addChild(PERSON_PATH)
                .addQueryParameter(DB_IDENTIFIER, "true")
                .addQueryParameter(LIMIT_IDENTIFIER, LIMIT_VALUE)
                .addQueryParameter(BIRTHDATE_IDENTIFIER, nin.getBirthDate())
                .addQueryParameter(PERSONAL_NUMBER_IDENTIFIER, nin.getPersonalNumber())
                .getUri();
    }

    private URI createSearchCourseUri(FsIdNumber fsIdNumber) {
        final String year = Year.now().toString();

        return UriWrapper.fromUri(baseFsHostUrl)
                .addChild(STUDENT_TEACHING_PATH)
                .addQueryParameter(DB_IDENTIFIER, "true")
                .addQueryParameter(LIMIT_IDENTIFIER, LIMIT_VALUE)
                .addQueryParameter(FS_PERSON_ID_NUMBER_PATH, fsIdNumber.toString())
                .addQueryParameter(TEACHING_SEMESTER_AR_PATH, year)
                .getUri();
    }

    private URI createSearchRolesToStaffPersonUri(FsIdNumber fsIdNumber) {
        final String year = Year.now().toString();

        return UriWrapper.fromUri(baseFsHostUrl)
                .addChild(PERSON_ROLES)
                .addQueryParameter(FS_PERSON_ID_NUMBER_PATH, fsIdNumber.toString())
                .addQueryParameter(SEMESTER_YEAR_PATH, year)
                .getUri();
    }

    private URI createSearchCourseToRoleUri(FsRoleToStaffPerson role) {
        return role.getUriToRole();
    }

    private URI createSearchCourseForStaffPersonUri(FsUriToCourseActivity courseActivity) {
        return courseActivity.getUri();
    }

    private String getBasicAuthenticationHeader() {
        final String valueToEncode = this.username + ":" + this.password;

        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }
}

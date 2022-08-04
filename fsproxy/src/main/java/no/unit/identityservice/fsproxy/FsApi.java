package no.unit.identityservice.fsproxy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.identityservice.fsproxy.model.course.FsCourse;
import no.unit.identityservice.fsproxy.model.course.FsCourseItemContainingCourseContainer;
import no.unit.identityservice.fsproxy.model.course.FsCoursesSearchResult;
import no.unit.identityservice.fsproxy.model.fagperson.FsRoleToStaffPerson;
import no.unit.identityservice.fsproxy.model.fagperson.FsRolesToPersonSearchResult;
import no.unit.identityservice.fsproxy.model.fagperson.FsUriToCourseActivity;
import no.unit.identityservice.fsproxy.model.fagperson.FsUriToCourseActivityContainer;
import no.unit.identityservice.fsproxy.model.person.FsIdNumber;
import no.unit.identityservice.fsproxy.model.person.FsPersonSearchResponse;
import no.unit.identityservice.fsproxy.model.person.Nin;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.joda.time.DateTime;

@SuppressWarnings("PMD")

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
    
    private static URI readFsHost() {
        var hostUriString = ENVIRONMENT.readEnv("FS_HOST");
        return URI.create(hostUriString);
    }
    
    private final String username = ENVIRONMENT.readEnv("FS_USERNAME");
    private final String password = ENVIRONMENT.readEnv("FS_PASSWORD");
    
    private final HttpClient httpClient;
    private final URI baseFsHostUrl;
    
    public FsApi(HttpClient httpClient, URI baseFsHostUrl) {
        this.httpClient = httpClient;
        this.baseFsHostUrl = baseFsHostUrl;
    }
    
    @JacocoGenerated
    public FsApi() {
        this(HttpClient.newBuilder().build(), FS_HOST);
    }
    
    public FsIdNumber getFsId(Nin nationalIdentityNumber) throws IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder().build();
        var httpRequest = HttpRequest.newBuilder(createSearchPersonUri(nationalIdentityNumber))
            .header("Authorization", getBasicAuthenticationHeader())
            .GET()
            .build();
        
        var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        var responseBody = response.body();
        var fsIdSearchResult = JsonUtils.dtoObjectMapper.readValue(responseBody, FsPersonSearchResponse.class);
        
        if (fsIdSearchResult.getSearchResults().size() == 0) {
            throw new UserPrincipalNotFoundException(nationalIdentityNumber.toString());
        } else {
            
            return fsIdSearchResult.getSearchResults().get(0).getFsPerson().getFsIdNumber();
        }
    }
    
    public List<FsCourseItemContainingCourseContainer> getCoursesToStudent(FsIdNumber fsIdNumber)
        throws IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder().build();
        var httpRequest = HttpRequest.newBuilder(createSearchCourseUri(fsIdNumber))
            .header("Authorization", getBasicAuthenticationHeader())
            .GET()
            .build();
        
        var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        var responseBody = response.body();
        var fsCoursesSearchResult = JsonUtils.dtoObjectMapper
            .readValue(responseBody, FsCoursesSearchResult.class);
        return fsCoursesSearchResult.getItems();
    }
    
    public List<FsCourse> getAllCoursesToStaffPerson(FsIdNumber fsIdNumber)
        throws IOException, InterruptedException {
        var roles = this.getRolesToFagperson(fsIdNumber);
        var coursesUriToRoles = roles.stream().map(role -> {
            try {
                return getCourseUriToGivenRole(role);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        
        var courses = coursesUriToRoles.stream().map(uri -> {
            try {
                return getCourseToFagpersonGivenUriToCourse(uri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).collect(
            Collectors.toList());
        
        return courses;
    }
    
    public Boolean getFsPersonStatus(FsIdNumber fsIdNumber) throws IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder().build();
        var httpRequest = HttpRequest.newBuilder(createSearchRolesToFagpersonUri(fsIdNumber))
            .header("Authorization", getBasicAuthenticationHeader())
            .GET()
            .build();
        
        var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        var responseBody = response.body();
        var fsIdSearchResult = JsonUtils.dtoObjectMapper.readValue(responseBody, FsPersonSearchResponse.class);
        
        if (fsIdSearchResult.getSearchResults().size() == 0) {
            throw new UserPrincipalNotFoundException(fsIdNumber.toString());
        } else {
            return fsIdSearchResult.getSearchResults().get(0).getFsPerson().getFagperson().getActiveStatus();
        }
    }
    
    public List<FsRoleToStaffPerson> getRolesToFagperson(FsIdNumber fsIdNumber)
        throws IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder().build();
        var httpRequest = HttpRequest.newBuilder(createSearchRolesToFagpersonUri(fsIdNumber))
            .header("Authorization", getBasicAuthenticationHeader())
            .GET()
            .build();
        
        var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        var responseBody = response.body();
        var fsRolesSearchResult = JsonUtils.dtoObjectMapper.readValue(responseBody,
            FsRolesToPersonSearchResult.class);
        return fsRolesSearchResult.getItems();
    }
    
    public FsUriToCourseActivity getCourseUriToGivenRole(FsRoleToStaffPerson role)
        throws IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder().build();
        var httpRequest = HttpRequest.newBuilder(createSearchCourseToRoleUri(role.getUriToRole()))
            .header("Authorization", getBasicAuthenticationHeader())
            .GET()
            .build();
        
        var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        var responseBody = response.body();
        var fsCourse = JsonUtils.dtoObjectMapper.readValue(responseBody, FsUriToCourseActivityContainer.class);
        return fsCourse.getCourseUri();
    }
    
    public FsCourse getCourseToFagpersonGivenUriToCourse(FsUriToCourseActivity course)
        throws IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder().build();
        var httpRequest = HttpRequest.newBuilder(createSearchCourseForFagpersonUri(course.getUri()))
            .header("Authorization", getBasicAuthenticationHeader())
            .GET()
            .build();
        
        var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        var responseBody = response.body();
        var fsCourse = JsonUtils.dtoObjectMapper.readValue(responseBody, FsCourse.class);
        
        return fsCourse;
    }
    
    private URI createSearchPersonUri(Nin nin) {
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
    
    private URI createSearchRolesToFagpersonUri(FsIdNumber fsIdNumber) {
        final String year = String.valueOf(new DateTime().getYear());
        return UriWrapper.fromUri(baseFsHostUrl)
            .addChild(PERSONROLLER_PATH)
            .addQueryParameter(PERSON_PERSONLOPENUMMER_PATH, fsIdNumber.toString())
            .addQueryParameter(SEMESTER_AR_PATH, year)
            .getUri();
    }
    
    private URI createSearchCourseToRoleUri(String href) {
        return UriWrapper.fromUri(baseFsHostUrl).addChild(PERSONROLLER_PATH).addChild(href).getUri();
    }
    
    private URI createSearchCourseForFagpersonUri(String href) {
        return UriWrapper.fromUri(baseFsHostUrl).addChild(UNDERVISNINGSAKTIVITETER_PATH).addChild(href).getUri();
    }
    
    private String getBasicAuthenticationHeader() {
        final String valueToEncode = this.username + ":" + this.password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }
}

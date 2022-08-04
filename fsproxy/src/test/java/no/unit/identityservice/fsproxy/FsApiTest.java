package no.unit.identityservice.fsproxy;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.io.IOException;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.stream.Collectors;
import no.unit.identityservice.fsproxy.model.course.FsCourse;
import no.unit.identityservice.fsproxy.model.course.FsSubject;
import no.unit.identityservice.fsproxy.model.person.FsIdNumber;
import no.unit.identityservice.fsproxy.util.CourseToRoleGenerator;
import no.unit.identityservice.fsproxy.util.PersonGenerator;
import no.unit.identityservice.fsproxy.util.RoleUriGenerator;
import no.unit.identityservice.fsproxy.util.StudentGenerator;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FsApiTest {
    
    public static final String FODSELSDATO_IDENTIFIER = "fodselsdato0";
    public static final String PERSONNUMMER_IDENTIFIER = "personnummer0";
    public static final String PERSONROLLER = "/personroller";
    public static final String SEMESTER_AR_PATH = "semester.ar";
    public static final String UNDERVISNINGSAKTIVITETER_PATH = "/undervisningsaktiviteter";
    private static final String PERSON_PATH = "/personer";
    private static final String STUDENTUNDERVISNING_PATH = "/studentundervisning";
    private static final String UNDERVISNING_SEMESTER_AR_PATH = "undervisning.semester.ar";
    private static final String PERSON_PERSONLOPENUMMER_PATH = "person.personlopenummer";
    private static final String DB_IDENTIFIER = "dbId";
    private static final String LIMIT_IDENTIFIER = "limit";
    private static final String LIMIT_VALUE = "0";
    private FsApi fsApi;
    private FsMock fsMock;
    private WireMockServer httpServer;
    
    @AfterEach
    public void tearDown() {
        fsMock.shutDown();
    }
    
    @BeforeEach
    void init() {
        fsMock = new FsMock();
        fsMock.initialize();
        fsApi = new FsApi(fsMock.getHttpClient(), fsMock.getFsHostUri());
    }
    
    @Test
    void shouldReturnFsIdNumberWhenInputIsNin() throws IOException, InterruptedException {
        var somePerson = fsMock.createExistingPersonWithoutEmployment();
        var personEntry = fsMock.getPersonEntry(somePerson);
        var actualIdNumber = fsApi.getFsId(somePerson);
        assertThat(actualIdNumber, is(equalTo(personEntry.getFsIdNumber())));
    }
    
    @Test
    void shouldThrowExceptionWhenPersonIsNotFoundInFs() {
        var personNotInFs = fsMock.createResponseForPersonNotInFs();
        assertThrows(UserPrincipalNotFoundException.class, () -> fsApi.getFsId(personNotInFs));
    }
    
    @Test
    void shouldReturnStudentCoursesOfCurrentYearWhenInputIsNinOfStudent() throws IOException, InterruptedException {
        var student = fsMock.createStudent();
        var fsIdNumber = fsMock.getPersonEntry(student).getFsIdNumber();
        var currentYear = FsMock.CURRENT_YEAR;
        var expectedCoursesCodes = fsMock.getStudentCourses(student)
            .stream().filter(c -> c.getSemester().getYear() == currentYear)
            .map(FsCourse::getSubject)
            .map(FsSubject::getCode)
            .collect(Collectors.toList());
        
        var actualCoursesCodes = fsApi.getCoursesToStudent(fsIdNumber)
            .stream()
            .map(item -> item.getId().getCourse().getSubject().getCode())
            .collect(Collectors.toList());
        
        assertThat(actualCoursesCodes, containsInAnyOrder(expectedCoursesCodes.toArray(String[]::new)));
    }
    
    @Test
    void shouldReturnSameAmountOfCoursesOfCurrentYearWhenInputIsNinOfStudent()
        throws IOException, InterruptedException {
    
    }
    
    @Test
    void shouldReturnCoursesWhenJsonIsDeserialized() throws IOException, InterruptedException {
    
    }
    
    @Test
    void shouldDetectIfPersonIsStaffPerson() throws IOException, InterruptedException {
        var studentGenerator = new StudentGenerator();
        var nin = studentGenerator.generateNin();
        
        var fsIdNumber = new FsIdNumber(Integer.valueOf(
            studentGenerator.getPersonGenerator().getSearchResults().get(0).getFsPerson().getFsIdNumber().toString()));
        
        var expectedFagpersonStatus = studentGenerator.getPersonGenerator()
            .getSearchResults()
            .get(0)
            .getFsPerson()
            .getFagperson()
            .getActiveStatus();
        
        createTestStubForGetRolesToFagperson(fsIdNumber.toString(), studentGenerator.convertToJson());
        var actualFagPersonStatus = fsApi.getFsPersonStatus(fsIdNumber);
        assertThat(actualFagPersonStatus, is(equalTo(expectedFagpersonStatus)));
    }
    
    @Test
    void shouldReturnHrefRolesToPerson() throws IOException, InterruptedException {
        var staffPersonGenerator = new PersonGenerator();
        var fsIdNumber = new FsIdNumber(randomInteger());
        var expectedRoles = staffPersonGenerator.getFsRolesToFagpersonSearchResult().getItems();
        createTestStubForGetRolesToFagperson(String.valueOf(fsIdNumber),
            staffPersonGenerator.convertToJson());
        var actualRoles = fsApi.getRolesToFagperson(fsIdNumber);
        assertThat(actualRoles, containsInAnyOrder(expectedRoles.toArray()));
    }
    
    @Test
    void shouldReturnCourseUriToRole() throws IOException, InterruptedException {
        var UriToRoleGenerator = new RoleUriGenerator();
        var role = UriToRoleGenerator.generateRole();
        var courseToRoleGenerator = new CourseToRoleGenerator();
        createTestStubForGetCourseToRole(role.getUriToRole(), courseToRoleGenerator.convertToJson());
        var expectedCourse = courseToRoleGenerator.getFsCourseToRoleSearchResult().getCourseUri();
        var actualCourses = fsApi.getCourseUriToGivenRole(role);
        assertThat(actualCourses, is(equalTo(expectedCourse)));
    }
    
    @Test
    void shouldReturnCourseToPerson() throws IOException, InterruptedException {
    
    }
    
    @Test
    void shouldReturnAllCoursesToPerson() throws IOException, InterruptedException {
    
    }
    
    private void createTestStubForGetFsPersonId(String birthdate, String personalNumber, String responseBody) {
        httpServer.stubFor(get(urlPathEqualTo(PERSON_PATH)).withQueryParam(DB_IDENTIFIER, WireMock.equalTo("true"))
            .withQueryParam(LIMIT_IDENTIFIER, WireMock.equalTo(LIMIT_VALUE))
            .withQueryParam(FODSELSDATO_IDENTIFIER, WireMock.equalTo(birthdate))
            .withQueryParam(PERSONNUMMER_IDENTIFIER, WireMock.equalTo(personalNumber))
            .willReturn(ok().withHeader("Content-Type", "application/json").withBody(responseBody)));
    }
    
    private void createTestStubForGetCoursesToFsStudent(String lopenummer, String responseBody) {
        String year = String.valueOf(new DateTime().getYear());
        httpServer.stubFor(
            get(urlPathEqualTo(STUDENTUNDERVISNING_PATH)).withQueryParam(DB_IDENTIFIER, WireMock.equalTo("true"))
                .withQueryParam(LIMIT_IDENTIFIER, WireMock.equalTo(LIMIT_VALUE))
                .withQueryParam(PERSON_PERSONLOPENUMMER_PATH, WireMock.equalTo(lopenummer))
                .withQueryParam(UNDERVISNING_SEMESTER_AR_PATH, WireMock.equalTo(year))
                .willReturn(ok().withHeader("Content-Type", "application/json").withBody(responseBody)));
    }
    
    private void createTestStubForGetRolesToFagperson(String lopenummer, String responseBody) {
        String year = String.valueOf(new DateTime().getYear());
        httpServer.stubFor(
            get(urlPathEqualTo(PERSONROLLER)).withQueryParam(PERSON_PERSONLOPENUMMER_PATH,
                    WireMock.equalTo(lopenummer)).withQueryParam(
                    SEMESTER_AR_PATH, WireMock.equalTo(year))
                .willReturn(ok().withHeader("Content-type", "application/json").withBody(responseBody)));
    }
    
    private void createTestStubForGetCourseToRole(String href, String responseBody) {
        httpServer.stubFor(
            get(urlPathEqualTo(PERSONROLLER + "/" + href))
                .willReturn(ok().withHeader("Content-type", "application/json").withBody(responseBody)));
    }
    
    private void createTestStubForGetCourseToFagperson(String href, String responseBody) {
        httpServer.stubFor(
            get(urlPathEqualTo(UNDERVISNINGSAKTIVITETER_PATH + "/" + href))
                .willReturn(ok().withHeader("Content-type", "application/json").withBody(responseBody)));
    }
}

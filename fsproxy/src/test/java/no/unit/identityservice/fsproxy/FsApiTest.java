package no.unit.identityservice.fsproxy;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.identityservice.fsproxy.model.FsCourseData;
import no.unit.identityservice.fsproxy.model.FsIdNumber;
import no.unit.identityservice.fsproxy.util.CourseGenerator;
import no.unit.identityservice.fsproxy.util.PersonGenerator;
import no.unit.nva.stubs.WiremockHttpClient;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FsApiTest {

    public static final String FODSELSDATO_IDENTIFIER = "fodselsdato0";
    public static final String PERSONNUMMER_IDENTIFIER = "personnummer0";
    private static final String PERSON_PATH = "/personer";
    private static final String STUDENTUNDERVISNING_PATH = "/studentundervisning";
    private static final String UNDERVISNING_SEMESTER_AR_PATH = "undervisning.semester.ar";
    private static final String PERSON_PERSONLOPENUMMER_PATH = "person.personlopenummer";
    private static final String DB_IDENTIFIER = "dbId";
    private static final String LIMIT_IDENTIFIER = "limit";
    private static final String LIMIT_VALUE = "0";

    private HttpClient httpClient;
    private WireMockServer httpServer;
    private URI fsUrl;
    private FsApi fsApi;

    @AfterEach
    public void tearDown() {
        httpServer.stop();
    }

    @BeforeEach
    void init() {
        startWiremockServer();
    }

    @Test
    void shouldReturnFsIdNumberWhenInputIsNin() throws IOException, InterruptedException {
        var personGenerator = new PersonGenerator();
        var nin = personGenerator.generateNin();
        var expectedFsIdNumber = new FsIdNumber(Integer.valueOf(
            personGenerator.getPersonGenerator().getSearchResults().get(0).getFsPerson().getFsIdNumber().toString()));
        createTestStubForGetFsPersonId(nin.getBirthDate(), nin.getPersonalNumber(), personGenerator.convertToJson());
        var actualIdNumber = fsApi.getFsId(nin);
        assertThat(actualIdNumber, is(equalTo(expectedFsIdNumber)));
    }

    @Test
    void shouldThrowExceptionWhenFsIdNumberIsNull() {
        var personGenerator = new PersonGenerator(0);
        var nin = personGenerator.generateNin();
        createTestStubForGetFsPersonId(nin.getBirthDate(), nin.getPersonalNumber(), personGenerator.convertToJson());

        Assertions.assertThrows(UserPrincipalNotFoundException.class, () -> fsApi.getFsId(nin));
    }

    @Test
    void shouldReturnCoursesOfCurrentYearWhenInputIsNinOfStudent() throws IOException, InterruptedException {
        var courseGenerator = new CourseGenerator();
        var expectedCoursesEmner = courseGenerator.getFsCoursesSearchResult().getFsCourseData().stream()
                                       .map(item -> item.getFsCourse().getUndervisning().getEmne().getCode())
                                       .collect(Collectors.toList());
        var fsIdNumber = new FsIdNumber(randomInteger());
        createTestStubForGetCoursesToFsStudent(fsIdNumber.toString(), courseGenerator.convertToJson());

        var actualCoursesEmner = fsApi.getCourses(fsIdNumber)
                                     .stream()
                                     .map(item -> item.getFsCourse().getUndervisning().getEmne().getCode())
                                     .collect(Collectors.toList());

        assertTrue(actualCoursesEmner.containsAll(expectedCoursesEmner));
    }

    @Test
    void shouldReturnSameAmountOfCoursesOfCurrentYearWhenInputIsNinOfStudent()
        throws IOException, InterruptedException {
        var courseGenerator = new CourseGenerator();
        List<FsCourseData> expectedCourses = courseGenerator.getFsCoursesSearchResult().getFsCourseData();
        var fsIdNumber = new FsIdNumber(randomInteger());
        createTestStubForGetCoursesToFsStudent(fsIdNumber.toString(), courseGenerator.convertToJson());
        var actualCourses = fsApi.getCourses(fsIdNumber);

        assertEquals(expectedCourses.size(), actualCourses.size());
    }

    @Test
    void shouldReturnUndervisningDataWhenJsonIsDeserialized() throws IOException, InterruptedException {
        var courseGenerator = new CourseGenerator();
        List<FsCourseData> expectedCourses = courseGenerator.getFsCoursesSearchResult().getFsCourseData();
        var fsIdNumber = new FsIdNumber(randomInteger());

        createTestStubForGetCoursesToFsStudent(fsIdNumber.toString(), courseGenerator.convertToJson());
        var actualCourses = fsApi.getCourses(fsIdNumber);

        assertThat(actualCourses, containsInAnyOrder(expectedCourses.toArray()));
    }

    private void startWiremockServer() {
        httpServer = new WireMockServer(options().dynamicPort());
        httpServer.start();
        fsUrl = URI.create(httpServer.baseUrl());
        httpClient = WiremockHttpClient.create();
        fsApi = new FsApi(httpClient, httpServer.baseUrl());
    }

    private void createTestStubForGetFsPersonId(String birthdate, String personalNumber, String responseBody) {
        httpServer.stubFor(
            get(urlPathEqualTo(PERSON_PATH))
                .withQueryParam(DB_IDENTIFIER, WireMock.equalTo("true"))
                .withQueryParam(LIMIT_IDENTIFIER, WireMock.equalTo(LIMIT_VALUE))
                .withQueryParam(FODSELSDATO_IDENTIFIER, WireMock.equalTo(birthdate))
                .withQueryParam(PERSONNUMMER_IDENTIFIER, WireMock.equalTo(personalNumber))
                .willReturn(
                    ok().withHeader("Content-Type", "application/json").withBody(responseBody)));
    }

    private void createTestStubForGetCoursesToFsStudent(String lopenummer, String responseBody) {
        String year = String.valueOf(new DateTime().getYear());
        httpServer.stubFor(
            get(urlPathEqualTo(STUDENTUNDERVISNING_PATH))
                .withQueryParam(DB_IDENTIFIER, WireMock.equalTo("true"))
                .withQueryParam(LIMIT_IDENTIFIER, WireMock.equalTo(LIMIT_VALUE))
                .withQueryParam(PERSON_PERSONLOPENUMMER_PATH, WireMock.equalTo(lopenummer))
                .withQueryParam(UNDERVISNING_SEMESTER_AR_PATH, WireMock.equalTo(year))
                .willReturn(
                    ok().withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }
}

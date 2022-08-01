package no.unit.identityservice.fsproxy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import no.unit.identityservice.fsproxy.model.*;
import no.unit.nva.stubs.WiremockHttpClient;
import nva.commons.core.ioutils.IoUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FsApiTest {

    public static final String FODSELSDATO_IDENTIFIER = "fodselsdato0";
    public static final String PERSONNUMMER_IDENTIFIER = "personnummer0";
    public static final String FS_HOST = "api.fellesstudentsystem.no";
    private static final String PERSON_PATH = "/personer";
    private static final String STUDENTUNDERVISNING_PATH = "studentundervisning";
    private static final String UNDERVISNING_SEMESTER_AR_PATH = "undervisning.semester.ar";
    private static final String PERSON_PERSONLOPENUMMER_PATH = "person.personlopenummer";
    private static final String DB_IDENTIFIER = "dbId";
    private static final String LIMIT_IDENTIFIER = "limit";

    private static final FsCourseData TEST_COURSE_1 = new FsCourseData(
        new FsCourse(
            new FsUndervisning(
                new FsEmne("ABIO6050"), 1, new FsSemester("2022", "VÅR"))));
    private static final FsCourseData TEST_COURSE_2 = new FsCourseData(
        new FsCourse(
            new FsUndervisning(
                new FsEmne("ABIO6100"), 1, new FsSemester("2022", "VÅR"))));
    private static final FsCourseData TEST_COURSE_3 = new FsCourseData(
        new FsCourse(
            new FsUndervisning(
                new FsEmne("OPERA6000"), 1, new FsSemester("2022", "VÅR"))));
    private static final FsCourseData TEST_COURSE_4 = new FsCourseData(
        new FsCourse(
            new FsUndervisning(
                new FsEmne("OPERA6000"), 2, new FsSemester("2022", "HØST"))));
    private static final FsCourseData TEST_COURSE_5 = new FsCourseData(
        new FsCourse(
            new FsUndervisning(
                new FsEmne("ABIO6100"), 2, new FsSemester("2022", "VÅR"))));
    private static final FsCourseData TEST_COURSE_6 = new FsCourseData(
        new FsCourse(
            new FsUndervisning(
                new FsEmne("ABIO6100"), 3, new FsSemester("2022", "HØST"))));
    private static final FsCourseData TEST_COURSE_7 = new FsCourseData(
        new FsCourse(
            new FsUndervisning(
                new FsEmne("OPERAPRA20"), 1, new FsSemester("2022", "VÅR"))));
    private static final FsCourseData TEST_COURSE_8 = new FsCourseData(
        new FsCourse(
            new FsUndervisning(
                new FsEmne("OPERAPRA3"), 1, new FsSemester("2022", "HØST"))));
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
        var expectedFsIdNumber = new FsIdNumber(33637);
        var nin = new FsNin("24027336201");
        String response = IoUtils.stringFromResources(Path.of("FsPersonResponse.json"));
        createTestStubForGetFsPersonId("240273", "36201", response);
        var actualIdNumber = fsApi.getFsId(nin);
        assertThat(actualIdNumber, is(equalTo(expectedFsIdNumber)));
    }

    @Test
    void shouldReturnCoursesOfCurrentYearWhenInputIsNinOfStudent() throws IOException, InterruptedException {
        var fsIdNumber = new FsIdNumber(33637);
        List<FsCourseData> expectedCourses = new ArrayList<>();
        expectedCourses.add(TEST_COURSE_1);
        expectedCourses.add(TEST_COURSE_2);
        expectedCourses.add(TEST_COURSE_3);
        expectedCourses.add(TEST_COURSE_4);
        expectedCourses.add(TEST_COURSE_5);
        expectedCourses.add(TEST_COURSE_6);
        expectedCourses.add(TEST_COURSE_7);
        expectedCourses.add(TEST_COURSE_8);
        var expectedCoursesEmner = expectedCourses.stream()
                                       .map(item -> item.getFsCourse().getUndervisning().getEmne().getCode())
                                       .collect(Collectors.toList());


        String response = IoUtils.stringFromResources(Path.of("CoursesToStudent.json"));
        createTestStubForGetCoursesToFsStudent(fsIdNumber.toString(), response);

        var actualCoursesEmner = fsApi.getCourses(fsIdNumber)
                .stream()
                .map(item -> item.getFsCourse().getUndervisning().getEmne().getCode())
                .collect(Collectors.toList());

        assertTrue(actualCoursesEmner.containsAll(expectedCoursesEmner));
    }

    @Test
    void shouldReturnSameAmountOfCoursesOfCurrentYearWhenInputIsNinOfStudent()
        throws IOException, InterruptedException {
        var fsIdNumber = new FsIdNumber(33637);
        List<FsCourseData> expectedCourses = new ArrayList<>();
        expectedCourses.add(TEST_COURSE_1);
        expectedCourses.add(TEST_COURSE_2);
        expectedCourses.add(TEST_COURSE_3);
        expectedCourses.add(TEST_COURSE_4);
        expectedCourses.add(TEST_COURSE_5);
        expectedCourses.add(TEST_COURSE_6);
        expectedCourses.add(TEST_COURSE_7);
        expectedCourses.add(TEST_COURSE_8);

        String response = IoUtils.stringFromResources(Path.of("CoursesToStudent.json"));
        createTestStubForGetCoursesToFsStudent(fsIdNumber.toString(), response);
        var actualCourses = fsApi.getCourses(fsIdNumber);

        assertEquals(expectedCourses.size(), actualCourses.size());
    }

    @Test
    void shouldReturnUndervisningDataWhenJsonIsDeserialized() throws IOException, InterruptedException {
        var fsIdNumber = new FsIdNumber(33637);
        String expectedFsYear = new FsUndervisning(new FsEmne("ABIO6050"), 1,
                                                   new FsSemester("2022", "VÅR")).getSemester().getYear();
        String expectedFsTermin = new FsUndervisning(new FsEmne("ABIO6050"), 1,
                                                     new FsSemester("2022", "VÅR")).getSemester().getTermin();
        int expectedFsTerminNumber = new FsUndervisning(new FsEmne("ABIO6050"), 1,
                                                        new FsSemester("2022", "VÅR")).getTerminNumber();

        String response = IoUtils.stringFromResources(Path.of("CoursesToStudent.json"));
        createTestStubForGetCoursesToFsStudent(fsIdNumber.toString(), response);
        var courses = fsApi.getCourses(fsIdNumber);

        var actualFsUndervisningYears = courses.stream()
                                            .map(item -> item.getFsCourse().getUndervisning().getSemester().getYear())
                                            .collect(Collectors.toList());
        var actualFsUndervisningTermins = courses.stream()
                                              .map(item -> item.getFsCourse()
                                                               .getUndervisning()
                                                               .getSemester()
                                                               .getTermin())
                                              .collect(Collectors.toList());
        var actualFsUndervisningTerminNumbers = courses.stream()
                                                    .map(item -> item.getFsCourse().getUndervisning().getTerminNumber())
                                                    .collect(Collectors.toList());

        assertTrue(actualFsUndervisningYears.contains(expectedFsYear));
        assertTrue(actualFsUndervisningTermins.contains(expectedFsTermin));
        assertTrue(actualFsUndervisningTerminNumbers.contains(expectedFsTerminNumber));
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
                    .withQueryParam(LIMIT_IDENTIFIER, WireMock.equalTo("0"))
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
                        .withQueryParam(LIMIT_IDENTIFIER, WireMock.equalTo("0"))
                        .withQueryParam(PERSON_PERSONLOPENUMMER_PATH, WireMock.equalTo(lopenummer))
                        .withQueryParam(UNDERVISNING_SEMESTER_AR_PATH, WireMock.equalTo(year))
                        .willReturn(
                                ok().withHeader("Content-Type", "application/json").withBody(responseBody)));
    }
}

package no.unit.identityservice.fsproxy;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.identityservice.fsproxy.model.FsCourse;
import no.unit.identityservice.fsproxy.model.FsCourseData;
import no.unit.identityservice.fsproxy.model.FsEmne;
import no.unit.identityservice.fsproxy.model.FsIdNumber;
import no.unit.identityservice.fsproxy.model.FsNin;
import no.unit.identityservice.fsproxy.model.FsSemester;
import no.unit.identityservice.fsproxy.model.FsUndervisning;
import no.unit.nva.stubs.WiremockHttpClient;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FsApiTest {

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
        var expectedFsIdNumber = new FsIdNumber(78);
        var nin = new FsNin("24027336201");
        String response = IoUtils.stringFromResources(Path.of("CoursesToStudent.json"));
        createTestStubForGetFsPersonId("190477", "47298", response);
        var actualIdNumber = fsApi.getFsId(nin);
        assertThat(actualIdNumber, is(equalTo(expectedFsIdNumber)));
    }

    @Test
    void shouldReturnCoursesOfCurrentYearWhenInputIsNinOfStudent() throws IOException, InterruptedException {
        var nin = new FsNin("19047747298");
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
        var actualCoursesEmner = fsApi.getCourses(nin)
                                     .stream()
                                     .map(item -> item.getFsCourse().getUndervisning().getEmne().getCode())
                                     .collect(Collectors.toList());



        assertTrue(actualCoursesEmner.containsAll(expectedCoursesEmner));
    }

    @Test
    void shouldReturnSameAmountOfCoursesOfCurrentYearWhenInputIsNinOfStudent()
        throws IOException, InterruptedException {
        var nin = new FsNin("19047747298");
        List<FsCourseData> expectedCourses = new ArrayList<>();
        expectedCourses.add(TEST_COURSE_1);
        expectedCourses.add(TEST_COURSE_2);
        expectedCourses.add(TEST_COURSE_3);
        expectedCourses.add(TEST_COURSE_4);
        expectedCourses.add(TEST_COURSE_5);
        expectedCourses.add(TEST_COURSE_6);
        expectedCourses.add(TEST_COURSE_7);
        expectedCourses.add(TEST_COURSE_8);
        var actualCourses = fsApi.getCourses(nin);

        assertEquals(expectedCourses.size(), actualCourses.size());
    }

    @Test
    void shouldReturnUndervisningDataWhenJsonIsDeserialized() throws IOException, InterruptedException {
        var nin = new FsNin("19047747298");
        String expectedFsYear = new FsUndervisning(new FsEmne("ABIO6050"), 1,
                                                   new FsSemester("2022", "VÅR")).getSemester().getYear();
        String expectedFsTermin = new FsUndervisning(new FsEmne("ABIO6050"), 1,
                                                     new FsSemester("2022", "VÅR")).getSemester().getTermin();
        int expectedFsTerminNumber = new FsUndervisning(new FsEmne("ABIO6050"), 1,
                                                        new FsSemester("2022", "VÅR")).getTerminNumber();

        var courses = fsApi.getCourses(nin);
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
        httpServer = new WireMockServer(options().dynamicHttpsPort());
        httpServer.start();
        fsUrl = URI.create(httpServer.baseUrl());
        httpClient = WiremockHttpClient.create();
        fsApi = new FsApi(httpClient, httpServer.baseUrl());
    }

    private void createTestStubForGetFsPersonId(String birthdate, String personalNumber, String responseBody) {
        httpServer.stubFor(
            get(urlEqualTo(httpServer.baseUrl() + "/personer?dbId=true&limit=0&fodselsdato0=" + birthdate +
                           "&personnummer0=" + personalNumber)).willReturn(
                ok().withHeader("Content-Type", "application/json").withBody(responseBody)));
    }
}

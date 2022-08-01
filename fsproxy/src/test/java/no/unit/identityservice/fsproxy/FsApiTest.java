package no.unit.identityservice.fsproxy;

import no.unit.identityservice.fsproxy.model.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


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

    @Test
    void shouldReturnFsIdNumberWhenInputIsNin() throws IOException, InterruptedException {
        var expectedFsIdNumber = new FsIdNumber(78);
        var nin = new FsNin("24027336201");
        FsApi fsApi = new FsApi();
        var actualIdNumber = fsApi.getFsId(nin);

        assertThat(actualIdNumber, is(equalTo(expectedFsIdNumber)));
    }

    @Test
    void shouldReturnCoursesOfCurrentYearWhenInputIsNinOfStudent() throws IOException, InterruptedException {
        var nin = new FsNin("19047747298");
        List<FsCourseData> expectedCourses = new ArrayList<FsCourseData>();
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
        FsApi fsApi = new FsApi();
        var actualCoursesEmner = fsApi.getCourses(nin)
                .stream()
                .map(item -> item.getFsCourse().getUndervisning().getEmne().getCode())
                .collect(Collectors.toList());

        assertTrue(actualCoursesEmner.containsAll(expectedCoursesEmner));
    }

    @Test
    void shouldReturnSameAmountOfCoursesOfCurrentYearWhenInputIsNinOfStudent() throws IOException, InterruptedException {
        var nin = new FsNin("19047747298");
        List<FsCourseData> expectedCourses = new ArrayList<FsCourseData>();
        expectedCourses.add(TEST_COURSE_1);
        expectedCourses.add(TEST_COURSE_2);
        expectedCourses.add(TEST_COURSE_3);
        expectedCourses.add(TEST_COURSE_4);
        expectedCourses.add(TEST_COURSE_5);
        expectedCourses.add(TEST_COURSE_6);
        expectedCourses.add(TEST_COURSE_7);
        expectedCourses.add(TEST_COURSE_8);
        var actualCourses = new FsApi().getCourses(nin);

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

        FsApi fsApi = new FsApi();
        var courses = fsApi.getCourses(nin);
        var actualFsUndervisningYears = courses.stream()
                .map(item -> item.getFsCourse().getUndervisning().getSemester().getYear())
                .collect(Collectors.toList());
        var actualFsUndervisningTermins = courses.stream()
                .map(item -> item.getFsCourse().getUndervisning().getSemester().getTermin())
                .collect(Collectors.toList());
        var actualFsUndervisningTerminNumbers = courses.stream()
                .map(item -> item.getFsCourse().getUndervisning().getTerminNumber())
                .collect(Collectors.toList());

        assertTrue(actualFsUndervisningYears.contains(expectedFsYear));
        assertTrue(actualFsUndervisningTermins.contains(expectedFsTermin));
        assertTrue(actualFsUndervisningTerminNumbers.contains(expectedFsTerminNumber));

    }
}

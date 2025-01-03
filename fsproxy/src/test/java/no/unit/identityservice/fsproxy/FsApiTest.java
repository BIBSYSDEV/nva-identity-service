package no.unit.identityservice.fsproxy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FsApiTest {

    private FsApi fsApi;
    private FsMock fsMock;

    @AfterEach
    public void tearDown() {
        fsMock.shutDown();
    }

    @BeforeEach
    void init() {
        fsMock = new FsMock();
        fsApi = new FsApi(fsMock.getHttpClient(), fsMock.getFsHostUri());
    }

    @Test
    void shouldThrowExceptionWhenPersonIsNotFoundInFs() {
        var personNotInFs = fsMock.createResponseForPersonNotInFs();

        assertThrows(UserPrincipalNotFoundException.class, () -> fsApi.fetchCoursesForPerson(personNotInFs));
    }

    @Test
    void shouldReturnCoursesIfPersonIsStaffPersonOnly() throws IOException, InterruptedException {
        var staffPerson = fsMock.createStaffPerson();
        var expectedCourses = fsMock.getCoursesToStaffPerson();
        var actualCourses = fsApi.fetchCoursesForPerson(staffPerson);
        assertThat(actualCourses, is(equalTo(expectedCourses)));
    }

    @Test
    void shouldReturnCoursesIfPersonIsStudentOnly() throws IOException, InterruptedException {
        var student = fsMock.createStudent();
        var expectedCourses = fsMock.getStudentCourses(student);
        var actualCourses = fsApi.fetchCoursesForPerson(student);
        assertThat(actualCourses, is(equalTo(expectedCourses)));
    }

    @Test
    void shouldReturnEmptyCourseListWhenPersonIsNotSignedUpToAnyCourses() throws IOException, InterruptedException {
        var somePerson = fsMock.createPersonWithoutCourses();
        var coursesIfStudent = fsMock.getStudentCourses(somePerson);
        var coursesIfStaff = fsMock.getCoursesToStaffPerson();
        var expectedCourses = Stream.concat(coursesIfStaff.stream(), coursesIfStudent.stream())
            .collect(Collectors.toList());
        var actualCourses = fsApi.fetchCoursesForPerson(somePerson);

        assertThat(actualCourses, is(equalTo(expectedCourses)));
    }

    @Test
    void shouldReturnAllCoursesToPersonWhenPersonIsBothStudentAndStaffPerson()
        throws IOException, InterruptedException {
        var somePerson = fsMock.createPersonWhichIsStudentAndStaffPerson();
        var coursesIfStudent = fsMock.getStudentCourses(somePerson);
        var coursesIfStaff = fsMock.getCoursesToStaffPerson();
        var expectedCourses = Stream.concat(coursesIfStaff.stream(), coursesIfStudent.stream())
            .collect(Collectors.toList());
        var actualCourses = fsApi.fetchCoursesForPerson(somePerson);

        assertThat(actualCourses, is(equalTo(expectedCourses)));
    }
}

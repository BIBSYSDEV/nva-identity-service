package no.unit.identityservice.fsproxy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.IOException;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        fsMock.initialize();
        fsApi = new FsApi(fsMock.getHttpClient(), fsMock.getFsHostUri());
    }

    @Test
    void shouldThrowExceptionWhenPersonIsNotFoundInFs() {
        var personNotInFs = fsMock.createResponseForPersonNotInFs();

        assertThrows(UserPrincipalNotFoundException.class, () -> fsApi.getCourses(personNotInFs));
    }

    @Test
    void shouldReturnAllCoursesToPerson() throws IOException, InterruptedException {
        var somePerson = fsMock.createRandomPerson();
        var coursesIfStudent = fsMock.getStudentCourses(somePerson);
        var coursesIfStaff = fsMock.getCoursesToStaffPerson();
        var expectedCourses = Stream.concat(coursesIfStaff.stream(), coursesIfStudent.stream())
                                  .collect(Collectors.toList());
        var actualCourses = fsApi.getCourses(somePerson);

        assertThat(actualCourses, is(equalTo(expectedCourses)));
    }
}

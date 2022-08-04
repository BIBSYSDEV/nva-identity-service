package no.unit.identityservice.fsproxy.util;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.identityservice.fsproxy.model.course.FsCourse;

public class CourseListGeneratorForPperson extends CourseGeneratorForPperson {

    private List<FsCourse> courses;

    public CourseListGeneratorForPperson() {
        this.courses = generateRandomCoursesList();
    }

    public List<FsCourse> generateRandomCoursesList() {
        var maxNumberOfCourses = 10;
        return IntStream.range(0, randomInteger(maxNumberOfCourses))
                   .boxed()
                   .map(index -> super.generateFsUndervisning())
                   .collect(Collectors.toList());
    }

}

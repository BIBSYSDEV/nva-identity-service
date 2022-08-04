package no.unit.identityservice.fsproxy.util;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomLocalDate;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.identityservice.fsproxy.model.Course.FsCourseContainer;
import no.unit.identityservice.fsproxy.model.Course.FsCourseItemContainingCourseContainer;
import no.unit.identityservice.fsproxy.model.Course.FsCoursesSearchResult;
import no.unit.identityservice.fsproxy.model.Course.FsSubject;
import no.unit.identityservice.fsproxy.model.Course.FsSemester;
import no.unit.identityservice.fsproxy.model.Course.FsCourse;
import no.unit.nva.commons.json.JsonUtils;

public class CourseGeneratorForStudent {

    private final FsCoursesSearchResult fsCoursesSearchResult;


    public CourseGeneratorForStudent() {
        fsCoursesSearchResult = new FsCoursesSearchResult(generateRandomFsCourseDataList());
    }


    public String convertToJson() {
        try {
            return JsonUtils.dtoObjectMapper.writeValueAsString(fsCoursesSearchResult);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public FsCoursesSearchResult getFsCoursesSearchResult() {
        return fsCoursesSearchResult;
    }

    private List<FsCourseItemContainingCourseContainer> generateRandomFsCourseDataList() {
        var maxNumberOfCourses = 10;
        return IntStream.range(0, randomInteger(maxNumberOfCourses))
                   .boxed()
                   .map(index -> createRandomFsCourseData())
                   .collect(Collectors.toList());
    }

    private FsCourseItemContainingCourseContainer createRandomFsCourseData() {
        return new FsCourseItemContainingCourseContainer(generateRandomFsCourse());
    }

    private FsCourseContainer generateRandomFsCourse() {
        return new FsCourseContainer(generateFsUndervisning());
    }

    public FsCourse generateFsUndervisning() {
        return new FsCourse(createRandomFsEmne(), randomTermin(), randomSemester());
    }

    private FsSemester randomSemester() {
        return new FsSemester(randomYear(), randomTerminString());
    }

    private String randomTerminString() {
        return TERMIN.values()[randomInteger(TERMIN.values().length)].toString();
    }

    private String randomYear() {
        return Integer.toString(randomLocalDate().getYear());
    }

    private int randomTermin() {
        var minTermin = 1;
        var maxTermin = 3;
        return randomInteger(maxTermin) + minTermin;
    }

    private FsSubject createRandomFsEmne() {
        return new FsSubject(randomString());
    }

    @SuppressWarnings("ant:checkstyle")
    public enum TERMIN {
        VIT,
        SOM,
        HØST,
        VÅR
    }
}

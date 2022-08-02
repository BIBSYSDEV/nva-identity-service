package no.unit.identityservice.fsproxy.util;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomLocalDate;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.identityservice.fsproxy.model.FsCourse;
import no.unit.identityservice.fsproxy.model.FsCourseData;
import no.unit.identityservice.fsproxy.model.FsCoursesSearchResult;
import no.unit.identityservice.fsproxy.model.FsEmne;
import no.unit.identityservice.fsproxy.model.FsSemester;
import no.unit.identityservice.fsproxy.model.FsUndervisning;
import no.unit.nva.commons.json.JsonUtils;

public class CourseGenerator {

    private final FsCoursesSearchResult fsCoursesSearchResult;

    public CourseGenerator() {
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

    private List<FsCourseData> generateRandomFsCourseDataList() {
        var maxNumberOfCourses = 10;
        return IntStream.range(0, randomInteger(maxNumberOfCourses))
                   .boxed()
                   .map(index -> createRandomFsCourseData())
                   .collect(Collectors.toList());
    }

    private FsCourseData createRandomFsCourseData() {
        return new FsCourseData(generateRandomFsCourse());
    }

    private FsCourse generateRandomFsCourse() {
        return new FsCourse(generateFsUndervisning());
    }

    private FsUndervisning generateFsUndervisning() {
        return new FsUndervisning(createRandomFsEmne(), randomTermin(), randomSemester());
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

    private FsEmne createRandomFsEmne() {
        return new FsEmne(randomString());
    }

    @SuppressWarnings("ant:checkstyle")
    public enum TERMIN {
        VIT,
        SOM,
        HØST,
        VÅR
    }
}

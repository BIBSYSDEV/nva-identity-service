package no.unit.identityservice.fsproxy.util;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomLocalDate;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.identityservice.fsproxy.model.course.FsSubject;
import no.unit.identityservice.fsproxy.model.course.FsSemester;
import no.unit.identityservice.fsproxy.model.course.FsCourse;
import no.unit.nva.commons.json.JsonUtils;

public class CourseGeneratorForPperson {

    private final FsCourse fsCourse;

    public CourseGeneratorForPperson() {
        this.fsCourse = generateFsUndervisning();
    }

    public FsCourse getCourse() {
        return fsCourse;
    }

    public String convertToJson() {
        try {
            return JsonUtils.dtoObjectMapper.writeValueAsString(fsCourse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public FsCourse generateFsUndervisning() {
        return new FsCourse(createRandomFsEmne(), randomTermin(), randomSemester());
    }

    private FsSemester randomSemester() {
        return new FsSemester(randomYear(), randomTerminString());
    }

    private String randomTerminString() {
        return CourseGeneratorForStudent.TERMIN.values()[randomInteger(
            CourseGeneratorForStudent.TERMIN.values().length)].toString();
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

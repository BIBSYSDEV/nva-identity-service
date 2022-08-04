package no.unit.identityservice.fsproxy.util;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.identityservice.fsproxy.model.Fagperson.FsUriToCourseActivityContainer;
import no.unit.identityservice.fsproxy.model.Fagperson.FsUriToCourseActivity;
import no.unit.nva.commons.json.JsonUtils;

public class CourseToRoleGenerator {

    private FsUriToCourseActivityContainer fsUriToCourseActivityContainer;



    public CourseToRoleGenerator() {
        this.fsUriToCourseActivityContainer = generateRandomCourseToRoleResult();
    }

    public FsUriToCourseActivityContainer getFsCourseToRoleSearchResult() {
        return fsUriToCourseActivityContainer;
    }

    public String convertToJson() {
        try {
            return JsonUtils.dtoObjectMapper.writeValueAsString(fsUriToCourseActivityContainer);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private List<FsUriToCourseActivityContainer> generateRandomFsCourseToRoleSearchResultList() {
        var maxNumberOfCourses = 10;
        return IntStream.range(0, randomInteger(maxNumberOfCourses))
                   .boxed()
                   .map(index -> generateRandomCourseToRoleResult())
                   .collect(Collectors.toList());
    }




    private FsUriToCourseActivityContainer generateRandomCourseToRoleResult() {
        return new FsUriToCourseActivityContainer(generateRandomFsUndervisningsaktivitet());
    }

    private FsUriToCourseActivity generateRandomFsUndervisningsaktivitet() {
        return new FsUriToCourseActivity(randomString());
    }
}

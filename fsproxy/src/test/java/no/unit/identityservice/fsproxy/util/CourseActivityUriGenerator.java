package no.unit.identityservice.fsproxy.util;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import no.unit.identityservice.fsproxy.model.fagperson.FsUriToCourseActivity;

public class CourseActivityUriGenerator {

    private final FsUriToCourseActivity fsUriToCourseActivity;

    public CourseActivityUriGenerator() {
        this.fsUriToCourseActivity = generateCourseActivityUri();
    }

    public FsUriToCourseActivity getCourseActivity() {
        return fsUriToCourseActivity;
    }

    private FsUriToCourseActivity generateCourseActivityUri() {
        return new FsUriToCourseActivity(randomInteger()
                                         + ","
                                         + randomString()
                                         + ","
                                         + randomInteger()
                                         + ","
                                         + randomInteger()
                                         + ","
                                         + randomString()
                                         + ","
                                         + randomInteger());
    }
}

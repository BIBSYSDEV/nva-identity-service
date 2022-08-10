package no.unit.identityservice.fsproxy;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.identityservice.fsproxy.model.course.FsCourse;
import no.unit.identityservice.fsproxy.model.course.FsCourseItemContainingCourseContainer;
import no.unit.identityservice.fsproxy.model.course.FsCoursesSearchResult;
import no.unit.identityservice.fsproxy.model.course.FsSemester;
import no.unit.identityservice.fsproxy.model.course.FsSubject;
import no.unit.identityservice.fsproxy.model.fagperson.FsCourseActivity;
import no.unit.identityservice.fsproxy.model.fagperson.FsRoleToStaffPerson;
import no.unit.identityservice.fsproxy.model.fagperson.FsRolesToPersonSearchResult;
import no.unit.identityservice.fsproxy.model.fagperson.FsUriToCourseActivity;
import no.unit.identityservice.fsproxy.model.fagperson.FsUriToCourseActivityContainer;
import no.unit.identityservice.fsproxy.model.person.FsIdNumber;
import no.unit.identityservice.fsproxy.model.person.FsIdSearchResult;
import no.unit.identityservice.fsproxy.model.person.FsPerson;
import no.unit.identityservice.fsproxy.model.person.FsPersonSearchResponse;
import no.unit.identityservice.fsproxy.model.person.NationalIdentityNumber;
import no.unit.nva.stubs.WiremockHttpClient;
import org.joda.time.LocalDate;

public class FsMock {

    public static final String FODSELSDATO_IDENTIFIER = "fodselsdato0";
    public static final String PERSONNUMMER_IDENTIFIER = "personnummer0";
    public static final String PERSONROLLER = "/personroller";
    public static final String SEMESTER_AR_PATH = "semester.ar";
    public static final String UNDERVISNINGSAKTIVITETER_PATH = "/undervisningsaktiviteter";
    public static final String EMPTY_FAG_PERSON = null;
    public static final Integer CURRENT_YEAR = new LocalDate().getYear();
    public static final Integer NEXT_YEAR = CURRENT_YEAR + 1;
    private static final String PERSON_PATH = "personer";
    private static final String STUDENTUNDERVISNING_PATH = "/studentundervisning";
    private static final String UNDERVISNING_SEMESTER_AR_PATH = "undervisning.semester.ar";
    private static final String PERSON_PERSONLOPENUMMER_PATH = "person.personlopenummer";
    private static final String DB_IDENTIFIER = "dbId";
    private static final String LIMIT_IDENTIFIER = "limit";
    private static final String LIMIT_VALUE = "0";
    private final Map<NationalIdentityNumber, FsPerson> personEntries;
    private final Map<FsIdNumber, List<FsRoleToStaffPerson>> personRoles;
    private final Map<NationalIdentityNumber, List<FsCourse>> coursesForStudents;
    private final Map<FsUriToCourseActivity, FsCourseActivity> courseActivities;
    private final Map<FsRoleToStaffPerson, FsUriToCourseActivity> coursesToRoles;

    private WireMockServer server;
    private URI fsHostUri;
    private HttpClient httpClient;

    public FsMock() {
        personRoles = new ConcurrentHashMap<>();
        this.personEntries = new ConcurrentHashMap<>();
        coursesForStudents = new ConcurrentHashMap<>();
        coursesToRoles = new ConcurrentHashMap<>();
        courseActivities = new ConcurrentHashMap<>();
    }

    public void initialize() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        fsHostUri = URI.create(server.baseUrl());
        httpClient = WiremockHttpClient.create();
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public URI getFsHostUri() {
        return fsHostUri;
    }

    public FsPerson getPersonEntry(NationalIdentityNumber nin) {
        return personEntries.get(nin);
    }

    public void shutDown() {
        server.stop();
    }

    public NationalIdentityNumber createPerson() {
        var nin = randomNin();
        var person = new FsPerson(randomFsIdNumber(), EMPTY_FAG_PERSON, randomString(), randomString());
        personEntries.put(nin, person);
        List<FsCourse> courses = createCourses();
        coursesForStudents.put(nin, courses);

        addResponseWhenSearchingByNin(nin);
        addResponseForGettingCoursesStudentByFsIdNumber(nin, CURRENT_YEAR);
        addResponseForGettingCoursesStudentByFsIdNumber(nin, NEXT_YEAR);
        return nin;
    }

    public NationalIdentityNumber createRandomPerson() {
        var nin = randomNin();
        var person = new FsPerson(randomFsIdNumber(), randomString(), randomString(), randomString());
        personEntries.put(nin, person);

        List<FsCourse> coursesToStudent = createCourses();
        coursesForStudents.put(nin, coursesToStudent);

        List<FsRoleToStaffPerson> roles = createRoles();
        personRoles.put(person.getFsIdNumber(), roles);

        List<FsUriToCourseActivity> uriToCourseActivities = roles.stream()
                                                                .map(this::createUriToCourseActivity).toList();
        for (FsRoleToStaffPerson role : roles) {
            for (FsUriToCourseActivity uriToCourseActivity : uriToCourseActivities) {
                coursesToRoles.put(role, uriToCourseActivity);
            }
        }

        List<FsCourseActivity> coursesToStaffPerson = uriToCourseActivities.stream()
                                                          .map(this::createCourseActivity).toList();
        for (FsUriToCourseActivity uriToCourseActivity : uriToCourseActivities) {
            for (FsCourseActivity toStaffPerson : coursesToStaffPerson) {
                courseActivities.put(uriToCourseActivity, toStaffPerson);
            }
        }

        addPersonToFsInstance(nin);
        coursesToStudent.forEach(course -> addResponseForGettingCoursesStudentByFsIdNumber(nin, CURRENT_YEAR));
        coursesToStudent.forEach(course -> addResponseForGettingCoursesStudentByFsIdNumber(nin, NEXT_YEAR));
        addResponseForGettingRolesToStaffPerson(person.getFsIdNumber(), LocalDate.now().getYear());
        roles.forEach(this::addResponseForGettingCourseUriToRole);
        uriToCourseActivities.forEach(this::addResponseForGettingCourseToStaffPerson);

        return nin;
    }

    public List<FsCourse> getCoursesToStaffPerson() {
        return courseActivities.values().stream().map(FsCourseActivity::getCourse).collect(Collectors.toList());
    }

    public NationalIdentityNumber createExistingPersonWithoutEmployment() {
        var person = randomNin();
        var personEntry = new FsPerson(randomFsIdNumber(), EMPTY_FAG_PERSON, person.getBirthDate(),
                                       person.getPersonalNumber());
        personEntries.put(person, personEntry);
        addPersonToFsInstance(person);
        return person;
    }

    public NationalIdentityNumber createExistingPersonWithEmployment() {
        var person = randomNin();
        var personEntry = new FsPerson(randomFsIdNumber(), randomString(), person.getBirthDate(),
                                       person.getPersonalNumber());
        personEntries.put(person, personEntry);
        addPersonToFsInstance(person);
        return person;
    }

    public FsIdNumber createPersonWithRoles() {
        var person = new FsPerson(randomFsIdNumber(), randomString(), randomString(), randomString());

        var rolesEntry = IntStream.range(0, smallNumber())
                             .boxed()
                             .map(index -> createRole())
                             .collect(Collectors.toList());

        personRoles.put(person.getFsIdNumber(), rolesEntry);
        addResponseForGettingRolesToStaffPerson(person.getFsIdNumber(), LocalDate.now().getYear());
        return person.getFsIdNumber();
    }

    public List<FsRoleToStaffPerson> getRoles(FsIdNumber fsIdNumber) {
        return personRoles.get(fsIdNumber);
    }

    public NationalIdentityNumber createResponseForPersonNotInFs() {
        var nonExistingPerson = new NationalIdentityNumber(randomString());
        addResponseWhenForPersonNotRegisteredInFs(nonExistingPerson);
        return nonExistingPerson;
    }

    public List<FsCourse> getStudentCourses(NationalIdentityNumber nin) {
        return this.coursesForStudents.get(nin)
                   .stream()
                   .filter(c -> c.getSemester().getYear() == LocalDate.now().getYear())
                   .collect(Collectors.toList());
    }

    public List<FsCourse> createCourses() {
        return Stream.of(createCoursesForYear(CURRENT_YEAR), createCoursesForYear(NEXT_YEAR))
                   .flatMap(Function.identity())
                   .collect(Collectors.toList());
    }

    public List<FsRoleToStaffPerson> createRoles() {
        return IntStream.range(0, smallNumber()).boxed().map(index -> createRole()).collect(Collectors.toList());
    }

    public FsRoleToStaffPerson createRole() {
        return new FsRoleToStaffPerson(randomString());
    }

    public FsUriToCourseActivity createUriToCourseActivity(FsRoleToStaffPerson role) {
        var uriToCourse = new FsUriToCourseActivity(randomString());
        coursesToRoles.put(role, uriToCourse);
        addResponseForGettingCourseUriToRole(role);
        return uriToCourse;
    }

    public FsUriToCourseActivity getUriToCourseActivity(FsRoleToStaffPerson role) {
        return coursesToRoles.get(role);
    }

    public FsCourseActivity createCourseActivity(FsUriToCourseActivity uri) {
        var courseActivity = new FsCourseActivity(randomCourse());
        courseActivities.put(uri, courseActivity);
        addResponseForGettingCourseToStaffPerson(uri);
        return courseActivity;
    }

    private String createRolesResponseBody(FsIdNumber fsIdNumber) {
        return new FsRolesToPersonSearchResult(personRoles.get(fsIdNumber)).toString();
    }

    private String createUriToCourseActivityResponseBody(FsRoleToStaffPerson role) {
        return new FsUriToCourseActivityContainer(getUriToCourseActivity(role)).toString();
    }

    private FsCourseActivity getCourseActivity(FsUriToCourseActivity uri) {
        return courseActivities.get(uri);
    }

    private String createCourseActivityResponseBody(FsUriToCourseActivity uri) {
        return getCourseActivity(uri).toString();
    }

    private int smallNumber() {
        return 2 + randomInteger(10);
    }

    private int randomSemesterNumber() {
        return 1 + randomInteger(4);
    }

    private NationalIdentityNumber randomNin() {
        return new NationalIdentityNumber(randomString());
    }

    private FsIdNumber randomFsIdNumber() {
        return new FsIdNumber(randomInteger());
    }

    private String createCoursesResponseBody(NationalIdentityNumber nin, int year) {
        var coursesForYear = studentCoursesForYear(nin, year);
        var courses = FsCourseItemContainingCourseContainer.fromCourseList(coursesForYear);
        return new FsCoursesSearchResult(courses).toString();
    }

    private FsCourse randomCourse() {
        return new FsCourse(new FsSubject(randomString()), randomSemesterNumber(),
                            new FsSemester(randomInteger(), randomString()));
    }

    private FsCourse randomCourse(int year) {
        return new FsCourse(new FsSubject(randomString()), randomSemesterNumber(),
                            new FsSemester(year, randomString()));
    }

    private Stream<FsCourse> createCoursesForYear(Integer nextYear) {
        return IntStream.range(0, smallNumber()).boxed().map(index -> randomCourse(nextYear));
    }

    private List<FsCourse> studentCoursesForYear(NationalIdentityNumber nin, int year) {
        return coursesForStudents.get(nin)
                   .stream()
                   .filter(course -> course.getSemester().getYear() == year)
                   .collect(Collectors.toList());
    }

    private String fsPersonNotFoundResponse() {
        return new FsPersonSearchResponse(Collections.emptyList()).toString();
    }

    private void addResponseWhenSearchingByNin(NationalIdentityNumber nin) {
        var fsPerson = personEntries.get(nin);
        server.stubFor(get(urlPathEqualTo("/" + PERSON_PATH)).withQueryParam(DB_IDENTIFIER, equalTo("true"))
                           .withQueryParam(LIMIT_IDENTIFIER, equalTo(LIMIT_VALUE))
                           .withQueryParam(FODSELSDATO_IDENTIFIER, equalTo(nin.getBirthDate()))
                           .withQueryParam(PERSONNUMMER_IDENTIFIER, equalTo(nin.getPersonalNumber()))
                           .willReturn(ok().withHeader("Content-Type", "application/json")
                                           .withBody(fsPersonSearchResponse(fsPerson))));
    }

    private void addPersonToFsInstance(NationalIdentityNumber person) {
        var fsPerson = personEntries.get(person);
        addResponseWhenSearchingByNin(person);
        addResponseWhenSearchingById(fsPerson);
    }

    private void addResponseWhenSearchingById(FsPerson fsPerson) {
        server.stubFor(get(urlPathEqualTo(PERSON_PATH + "/" + fsPerson.getFsIdNumber().toString())).willReturn(
            ok().withHeader("Content-Type", "application/json").withBody(fsPerson.toString())));
    }

    private void addResponseWhenForPersonNotRegisteredInFs(NationalIdentityNumber nin) {
        server.stubFor(get(urlPathEqualTo("/" + PERSON_PATH)).withQueryParam(DB_IDENTIFIER, equalTo("true"))
                           .withQueryParam(LIMIT_IDENTIFIER, equalTo(LIMIT_VALUE))
                           .withQueryParam(FODSELSDATO_IDENTIFIER, equalTo(nin.getBirthDate()))
                           .withQueryParam(PERSONNUMMER_IDENTIFIER, equalTo(nin.getPersonalNumber()))
                           .willReturn(ok().withHeader("Content-Type", "application/json")
                                           .withBody(fsPersonNotFoundResponse())));
    }

    private void addResponseForGettingCoursesStudentByFsIdNumber(NationalIdentityNumber nin, Integer year) {
        server.stubFor(get(urlPathEqualTo(STUDENTUNDERVISNING_PATH)).withQueryParam(DB_IDENTIFIER, equalTo("true"))
                           .withQueryParam(LIMIT_IDENTIFIER, equalTo(LIMIT_VALUE))
                           .withQueryParam(PERSON_PERSONLOPENUMMER_PATH,
                                           equalTo(personEntries.get(nin).getFsIdNumber().toString()))
                           .withQueryParam(UNDERVISNING_SEMESTER_AR_PATH, equalTo(year.toString()))
                           .willReturn(ok().withHeader("Content-Type", "application/json")
                                           .withBody(createCoursesResponseBody(nin, year))));
    }

    private void addResponseForGettingRolesToStaffPerson(FsIdNumber fsIdNumber, Integer year) {
        server.stubFor(get(urlPathEqualTo(PERSONROLLER)).withQueryParam(PERSON_PERSONLOPENUMMER_PATH,
                                                                        WireMock.equalTo(fsIdNumber.toString()))
                           .withQueryParam(SEMESTER_AR_PATH, WireMock.equalTo(year.toString()))
                           .willReturn(ok().withHeader("Content-type", "application/json")
                                           .withBody(createRolesResponseBody(fsIdNumber))));
    }

    private void addResponseForGettingCourseUriToRole(FsRoleToStaffPerson uri) {
        server.stubFor(get(urlPathEqualTo(PERSONROLLER + "/" + uri.getUriToRole())).willReturn(
            ok().withHeader("Content-type", "application/json").withBody(createUriToCourseActivityResponseBody(uri))));
    }

    private void addResponseForGettingCourseToStaffPerson(FsUriToCourseActivity uri) {
        server.stubFor(get(urlPathEqualTo(UNDERVISNINGSAKTIVITETER_PATH + "/" + uri.getUri())).willReturn(
            ok().withHeader("Content-type", "application/json").withBody(createCourseActivityResponseBody(uri))));
    }

    private String fsPersonSearchResponse(FsPerson fsPerson) {
        FsIdSearchResult searchResult = new FsIdSearchResult(fsPerson);
        return new FsPersonSearchResponse(List.of(searchResult)).toString();
    }
}

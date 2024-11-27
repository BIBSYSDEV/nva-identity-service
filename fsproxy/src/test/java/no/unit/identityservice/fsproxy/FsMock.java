package no.unit.identityservice.fsproxy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import no.unit.identityservice.fsproxy.model.course.FsCourse;
import no.unit.identityservice.fsproxy.model.course.FsCourseItemContainingCourseContainer;
import no.unit.identityservice.fsproxy.model.course.FsCoursesSearchResult;
import no.unit.identityservice.fsproxy.model.course.FsSemester;
import no.unit.identityservice.fsproxy.model.course.FsSubject;
import no.unit.identityservice.fsproxy.model.person.FsIdNumber;
import no.unit.identityservice.fsproxy.model.person.FsIdSearchResult;
import no.unit.identityservice.fsproxy.model.person.FsPerson;
import no.unit.identityservice.fsproxy.model.person.FsPersonSearchResponse;
import no.unit.identityservice.fsproxy.model.person.NationalIdentityNumber;
import no.unit.identityservice.fsproxy.model.staffperson.FsCourseActivity;
import no.unit.identityservice.fsproxy.model.staffperson.FsRoleToStaffPerson;
import no.unit.identityservice.fsproxy.model.staffperson.FsRolesToPersonSearchResult;
import no.unit.identityservice.fsproxy.model.staffperson.FsUriToCourseActivity;
import no.unit.identityservice.fsproxy.model.staffperson.FsUriToCourseActivityContainer;
import no.unit.nva.stubs.WiremockHttpClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;

public class FsMock {

    public static final String BIRTHDATE_IDENTIFIER = "fodselsdato0";
    public static final String PERSONAL_NUMBER_IDENTIFIER = "personnummer0";
    public static final String PERSON_ROLES = "/personroller";
    public static final String SEMESTER_YEAR_PATH = "semester.ar";
    public static final Integer CURRENT_YEAR = Year.now().getValue();
    public static final Integer NEXT_YEAR = CURRENT_YEAR + 1;
    private static final String PERSON_PATH = "/personer";
    private static final String STUDENT_TEACHING_PATH = "/studentundervisning";
    private static final String TEACHING_SEMESTER_AR_PATH = "undervisning.semester.ar";
    private static final String FS_PERSON_ID_NUMBER_PATH = "person.personlopenummer";
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
        this.initialize();
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

    public void shutDown() {
        server.stop();
    }

    public NationalIdentityNumber createPersonWhichIsStudentAndStaffPerson() {
        var nin = randomNin();
        var person = new FsPerson(randomFsIdNumber(), randomString(), randomString(), randomString());
        personEntries.put(nin, person);

        List<FsCourse> coursesToStudent = createCourses();
        coursesForStudents.put(nin, coursesToStudent);

        List<FsRoleToStaffPerson> roles = createRoles();
        personRoles.put(person.getFsIdNumber(), roles);

        List<FsUriToCourseActivity> uriToCourseActivities = roles.stream()
            .map(this::createUriToCourseActivity)
            .collect(Collectors.toList());
        mapRolesToUri(roles, uriToCourseActivities);

        List<FsCourseActivity> coursesToStaffPerson = uriToCourseActivities.stream()
            .map(this::createCourseActivity)
            .collect(Collectors.toList());
        mapCoursesToUri(uriToCourseActivities, coursesToStaffPerson);

        addAllResponsesToCreatePerson(nin, person, roles, uriToCourseActivities);

        return nin;
    }

    public NationalIdentityNumber createPersonWithoutCourses() {
        var nin = randomNin();
        var person = new FsPerson(randomFsIdNumber(), randomString(), randomString(), randomString());
        personEntries.put(nin, person);

        List<FsCourse> coursesToStudent = new ArrayList<>();
        coursesForStudents.put(nin, coursesToStudent);

        List<FsRoleToStaffPerson> roles = new ArrayList<>();
        personRoles.put(person.getFsIdNumber(), roles);

        List<FsUriToCourseActivity> uriToCourseActivities = roles.stream()
            .map(this::createUriToCourseActivity)
            .collect(Collectors.toList());
        mapRolesToUri(roles, uriToCourseActivities);

        List<FsCourseActivity> coursesToStaffPerson = uriToCourseActivities.stream()
            .map(this::createCourseActivity)
            .collect(Collectors.toList());
        mapCoursesToUri(uriToCourseActivities, coursesToStaffPerson);

        addAllResponsesToCreatePerson(nin, person, roles, uriToCourseActivities);

        return nin;
    }

    public NationalIdentityNumber createStudent() {
        var nin = randomNin();
        var student = new FsPerson(randomFsIdNumber(), randomString(), randomString(), randomString());
        personEntries.put(nin, student);

        List<FsCourse> coursesToStudent = createCourses();
        coursesForStudents.put(nin, coursesToStudent);

        List<FsRoleToStaffPerson> roles = new ArrayList<>();
        personRoles.put(student.getFsIdNumber(), roles);

        List<FsUriToCourseActivity> uriToCourseActivities = roles.stream()
            .map(this::createUriToCourseActivity)
            .collect(Collectors.toList());
        mapRolesToUri(roles, uriToCourseActivities);

        List<FsCourseActivity> coursesToStaffPerson = uriToCourseActivities.stream()
            .map(this::createCourseActivity)
            .collect(Collectors.toList());
        mapCoursesToUri(uriToCourseActivities, coursesToStaffPerson);

        addAllResponsesToCreatePerson(nin, student, roles, uriToCourseActivities);

        return nin;
    }

    public NationalIdentityNumber createStaffPerson() {
        var nin = randomNin();
        var staffPerson = new FsPerson(randomFsIdNumber(), randomString(), randomString(), randomString());
        personEntries.put(nin, staffPerson);

        List<FsCourse> coursesToStudent = new ArrayList<>();
        coursesForStudents.put(nin, coursesToStudent);

        List<FsRoleToStaffPerson> roles = createRoles();
        personRoles.put(staffPerson.getFsIdNumber(), roles);

        List<FsUriToCourseActivity> uriToCourseActivities = roles.stream()
            .map(this::createUriToCourseActivity)
            .collect(Collectors.toList());
        mapRolesToUri(roles, uriToCourseActivities);

        List<FsCourseActivity> coursesToStaffPerson = uriToCourseActivities.stream()
            .map(this::createCourseActivity)
            .collect(Collectors.toList());
        mapCoursesToUri(uriToCourseActivities, coursesToStaffPerson);

        addAllResponsesToCreatePerson(nin, staffPerson, roles, uriToCourseActivities);

        return nin;
    }

    public List<FsCourse> getCoursesToStaffPerson() {
        return courseActivities.values().stream().map(FsCourseActivity::getCourse).collect(Collectors.toList());
    }

    public NationalIdentityNumber createResponseForPersonNotInFs() {
        var nonExistingPerson = new NationalIdentityNumber(randomString());
        addResponseWhenForPersonNotRegisteredInFs(nonExistingPerson);
        return nonExistingPerson;
    }

    private void addResponseWhenForPersonNotRegisteredInFs(NationalIdentityNumber nin) {
        server.stubFor(get(urlPathEqualTo(PERSON_PATH)).withQueryParam(DB_IDENTIFIER, equalTo("true"))
            .withQueryParam(LIMIT_IDENTIFIER, equalTo(LIMIT_VALUE))
            .withQueryParam(BIRTHDATE_IDENTIFIER, equalTo(nin.getBirthDate()))
            .withQueryParam(PERSONAL_NUMBER_IDENTIFIER, equalTo(nin.getPersonalNumber()))
            .willReturn(ok().withHeader("Content-Type", "application/json")
                .withBody(fsPersonNotFoundResponse())));
    }

    private String fsPersonNotFoundResponse() {
        return new FsPersonSearchResponse(Collections.emptyList()).toString();
    }

    public List<FsCourse> getStudentCourses(NationalIdentityNumber nin) {
        return this.coursesForStudents.get(nin)
            .stream()
            .filter(c -> c.getSemester().getYear() == Year.now().getValue())
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
        return new FsRoleToStaffPerson(randomUri());
    }

    public FsUriToCourseActivity createUriToCourseActivity(FsRoleToStaffPerson role) {
        var uriToCourse = new FsUriToCourseActivity(randomUri());
        coursesToRoles.put(role, uriToCourse);
        addResponseForGettingCourseActivityToRoleUri(role);
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

    private void mapCoursesToUri(List<FsUriToCourseActivity> uriToCourseActivities,
                                 List<FsCourseActivity> coursesToStaffPerson) {
        for (FsUriToCourseActivity uriToCourseActivity : uriToCourseActivities) {
            for (FsCourseActivity toStaffPerson : coursesToStaffPerson) {
                courseActivities.put(uriToCourseActivity, toStaffPerson);
            }
        }
    }

    private void mapRolesToUri(List<FsRoleToStaffPerson> roles, List<FsUriToCourseActivity> uriToCourseActivities) {
        for (FsRoleToStaffPerson role : roles) {
            for (FsUriToCourseActivity uriToCourseActivity : uriToCourseActivities) {
                coursesToRoles.put(role, uriToCourseActivity);
            }
        }
    }

    private void addAllResponsesToCreatePerson(NationalIdentityNumber nin,
                                               FsPerson student,
                                               List<FsRoleToStaffPerson> roles,
                                               List<FsUriToCourseActivity> uriToCourseActivities) {
        addPersonToFsInstance(nin);
        addResponseForGettingStudentCoursesByFsIdNumber(nin, CURRENT_YEAR);
        addResponseForGettingStudentCoursesByFsIdNumber(nin, NEXT_YEAR);
        addResponseForGettingRolesToStaffPerson(student.getFsIdNumber(), Year.now().getValue());
        roles.forEach(this::addResponseForGettingCourseActivityToRoleUri);
        uriToCourseActivities.forEach(this::addResponseForGettingCourseToStaffPerson);
    }

    private URI randomUri() {
        return URI.create(server.baseUrl() + "/" + randomString());
    }

    private String createUriToCourseActivityResponseBody(FsRoleToStaffPerson role) {
        return new FsUriToCourseActivityContainer(getUriToCourseActivity(role)).toString();
    }

    private String createRolesResponseBody(FsIdNumber fsIdNumber) {
        return new FsRolesToPersonSearchResult(personRoles.get(fsIdNumber)).toString();
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

    private NationalIdentityNumber randomNin() {
        return new NationalIdentityNumber(randomString());
    }

    private FsIdNumber randomFsIdNumber() {
        return new FsIdNumber(randomInteger());
    }

    private String createCoursesResponseBody(NationalIdentityNumber nin, int year) {
        var coursesForYear = studentCoursesForYear(nin, year);
        var courses
            = FsCourseItemContainingCourseContainer.fromCourseList(coursesForYear);
        return new FsCoursesSearchResult(courses).toString();
    }

    private FsCourse randomCourse() {
        return new FsCourse(new FsSubject(randomString()), new FsSemester(randomInteger(), randomString()));
    }

    private FsCourse randomCourse(int year) {
        return new FsCourse(new FsSubject(randomString()), new FsSemester(year, randomString()));
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

    private String fsPersonSearchResponse(FsPerson fsPerson) {
        FsIdSearchResult searchResult = new FsIdSearchResult(fsPerson);
        return new FsPersonSearchResponse(List.of(searchResult)).toString();
    }

    private void addResponseWhenSearchingByNin(NationalIdentityNumber nin) {
        var fsPerson = personEntries.get(nin);
        server.stubFor(get(urlPathEqualTo(PERSON_PATH)).withQueryParam(DB_IDENTIFIER, equalTo("true"))
            .withQueryParam(LIMIT_IDENTIFIER, equalTo(LIMIT_VALUE))
            .withQueryParam(BIRTHDATE_IDENTIFIER, equalTo(nin.getBirthDate()))
            .withQueryParam(PERSONAL_NUMBER_IDENTIFIER, equalTo(nin.getPersonalNumber()))
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

    private void addResponseForGettingStudentCoursesByFsIdNumber(NationalIdentityNumber nin, Integer year) {
        server.stubFor(get(urlPathEqualTo(STUDENT_TEACHING_PATH)).withQueryParam(DB_IDENTIFIER, equalTo("true"))
            .withQueryParam(LIMIT_IDENTIFIER, equalTo(LIMIT_VALUE))
            .withQueryParam(FS_PERSON_ID_NUMBER_PATH,
                equalTo(personEntries.get(nin).getFsIdNumber().toString()))
            .withQueryParam(TEACHING_SEMESTER_AR_PATH, equalTo(year.toString()))
            .willReturn(ok().withHeader("Content-Type", "application/json")
                .withBody(createCoursesResponseBody(nin, year))));
    }

    private void addResponseForGettingRolesToStaffPerson(FsIdNumber fsIdNumber, Integer year) {
        server.stubFor(get(urlPathEqualTo(PERSON_ROLES)).withQueryParam(FS_PERSON_ID_NUMBER_PATH,
                WireMock.equalTo(fsIdNumber.toString()))
            .withQueryParam(SEMESTER_YEAR_PATH, WireMock.equalTo(year.toString()))
            .willReturn(ok().withHeader("Content-type", "application/json")
                .withBody(createRolesResponseBody(fsIdNumber))));
    }

    private void addResponseForGettingCourseActivityToRoleUri(FsRoleToStaffPerson role) {
        server.stubFor(get(urlPathEqualTo(role.getUriToRole().getPath())).willReturn(
            ok().withHeader("Content-type", "application/json")
                .withBody(createUriToCourseActivityResponseBody(role))));
    }

    private void addResponseForGettingCourseToStaffPerson(FsUriToCourseActivity courseActivity) {
        server.stubFor(get(urlPathEqualTo(courseActivity.getUri().getPath())).willReturn(
            ok().withHeader("Content-type", "application/json")
                .withBody(createCourseActivityResponseBody(courseActivity))));
    }
}

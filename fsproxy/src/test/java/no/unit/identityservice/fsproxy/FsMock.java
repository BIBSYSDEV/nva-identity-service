package no.unit.identityservice.fsproxy;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static no.unit.identityservice.fsproxy.FsApi.DB_IDENTIFIER;
import static no.unit.identityservice.fsproxy.FsApi.FODSELSDATO_IDENTIFIER;
import static no.unit.identityservice.fsproxy.FsApi.LIMIT_IDENTIFIER;
import static no.unit.identityservice.fsproxy.FsApi.LIMIT_VALUE;
import static no.unit.identityservice.fsproxy.FsApi.PERSONNUMMER_IDENTIFIER;
import static no.unit.identityservice.fsproxy.FsApi.PERSON_PATH;
import static no.unit.identityservice.fsproxy.FsApi.PERSON_PERSONLOPENUMMER_PATH;
import static no.unit.identityservice.fsproxy.FsApi.STUDENTUNDERVISNING_PATH;
import static no.unit.identityservice.fsproxy.FsApi.UNDERVISNING_SEMESTER_AR_PATH;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import com.github.tomakehurst.wiremock.WireMockServer;
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
import no.unit.identityservice.fsproxy.model.fagperson.FsPossibleStaffPerson;
import no.unit.identityservice.fsproxy.model.person.FsIdNumber;
import no.unit.identityservice.fsproxy.model.person.FsIdSearchResult;
import no.unit.identityservice.fsproxy.model.person.FsPerson;
import no.unit.identityservice.fsproxy.model.person.FsPersonSearchResponse;
import no.unit.identityservice.fsproxy.model.person.Nin;
import no.unit.nva.stubs.WiremockHttpClient;
import org.joda.time.LocalDate;

public class FsMock {
    
    public static final FsPossibleStaffPerson EMPTY_FAG_PERSON = null;
    private final Map<Nin, FsPerson> personEntries;
    private WireMockServer server;
    private URI fsHostUri;
    private HttpClient httpClient;
    public static final Integer CURRENT_YEAR = new LocalDate().getYear();
    public static final Integer NEXT_YEAR = CURRENT_YEAR + 1;
    private final Map<Nin, List<FsCourse>> coursesForStudents;
    
    public FsMock() {
        this.personEntries = new ConcurrentHashMap<>();
        coursesForStudents = new ConcurrentHashMap<>();
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
    
    public Nin createExistingPersonWithoutEmployment() {
        var person = randomNin();
        var personEntry = new FsPerson(randomFsIsNumber(), EMPTY_FAG_PERSON, person.getBirthDate(),
            person.getPersonalNumber());
        personEntries.put(person, personEntry);
        addPersonToFsInstance(person);
        return person;
    }
    
    public FsPerson getPersonEntry(Nin nin) {
        return personEntries.get(nin);
    }
    
    public void shutDown() {
        server.stop();
    }
    
    public Nin createResponseForPersonNotInFs() {
        var nonExistingPerson = new Nin(randomString());
        addResponseWhenForPersonNotRegisteredInFs(nonExistingPerson);
        return nonExistingPerson;
    }
    
    public List<FsCourse> createCourses() {
        return Stream.of(createCoursesForYear(CURRENT_YEAR), createCoursesForYear(NEXT_YEAR))
            .flatMap(Function.identity())
            .collect(Collectors.toList());
    }
    
    public List<FsCourse> getStudentCourses(Nin nin) {
        return this.coursesForStudents.get(nin);
    }
    
    public Nin createStudent() {
        var nin = randomNin();
        var person = new FsPerson(randomFsIsNumber(), EMPTY_FAG_PERSON, randomString(), randomString());
        personEntries.put(nin, person);
        List<FsCourse> courses = createCourses();
        coursesForStudents.put(nin, courses);
        addResponseWhenSearchingByNin(nin);
        addResponseForGettingCoursesStudentByFsIdNumber(nin, CURRENT_YEAR);
        addResponseForGettingCoursesStudentByFsIdNumber(nin, NEXT_YEAR);
        return nin;
    }
    
    private Stream<FsCourse> createCoursesForYear(Integer nextYear) {
        return IntStream.range(0, smallNumber())
            .boxed()
            .map(index -> randomCourse(nextYear));
    }
    
    private int smallNumber() {
        return 2 + randomInteger(10);
    }
    
    private FsCourse randomCourse(int year) {
        return new FsCourse(new FsSubject(randomString()), randomSemesterNumber(),
            new FsSemester(year, randomString()));
    }
    
    private int randomSemesterNumber() {
        return 1 + randomInteger(4);
    }
    
    private void addResponseForGettingCoursesStudentByFsIdNumber(Nin nin, Integer year) {
        server.stubFor(
            get(urlPathEqualTo("/" + STUDENTUNDERVISNING_PATH))
                .withQueryParam(DB_IDENTIFIER, equalTo("true"))
                .withQueryParam(LIMIT_IDENTIFIER, equalTo(LIMIT_VALUE))
                .withQueryParam(PERSON_PERSONLOPENUMMER_PATH,
                    equalTo(personEntries.get(nin).getFsIdNumber().toString()))
                .withQueryParam(UNDERVISNING_SEMESTER_AR_PATH, equalTo(year.toString()))
                .willReturn(ok().withHeader("Content-Type", "application/json")
                    .withBody(createCoursesResponseBody(nin, year))));
    }
    
    private String createCoursesResponseBody(Nin nin, int year) {
        var coursesForYear = studentCoursesForYear(nin, year);
        var courses = FsCourseItemContainingCourseContainer.fromCourseList(coursesForYear);
        var foo = new FsCoursesSearchResult(courses).toString();
        return foo;
    }
    
    private List<FsCourse> studentCoursesForYear(Nin nin, int year) {
        return coursesForStudents.get(nin).stream()
            .filter(course -> course.getSemester().getYear() == year)
            .collect(Collectors.toList());
    }
    
    private void addPersonToFsInstance(Nin person) {
        var fsPerson = personEntries.get(person);
        addResponseWhenSearchingByNin(person);
        addResponseWhenGettingById(fsPerson);
    }
    
    private void addResponseWhenGettingById(FsPerson fsPerson) {
        server.stubFor(get(urlPathEqualTo("/" + PERSON_PATH + "/" + fsPerson.getFsIdNumber().toString())).willReturn(
            ok().withHeader("Content-Type", "application/json").withBody(fsPerson.toString())));
    }
    
    private void addResponseWhenForPersonNotRegisteredInFs(Nin nin) {
        server.stubFor(get(urlPathEqualTo("/" + PERSON_PATH)).withQueryParam(DB_IDENTIFIER, equalTo("true"))
            .withQueryParam(LIMIT_IDENTIFIER, equalTo(LIMIT_VALUE))
            .withQueryParam(FODSELSDATO_IDENTIFIER, equalTo(nin.getBirthDate()))
            .withQueryParam(PERSONNUMMER_IDENTIFIER, equalTo(nin.getPersonalNumber()))
            .willReturn(ok().withHeader("Content-Type", "application/json")
                .withBody(fsPersonNotFoundResponse())));
    }
    
    private String fsPersonNotFoundResponse() {
        return new FsPersonSearchResponse(Collections.emptyList()).toString();
    }
    
    private void addResponseWhenSearchingByNin(Nin nin) {
        var fsPerson = personEntries.get(nin);
        server.stubFor(get(urlPathEqualTo("/" + PERSON_PATH)).withQueryParam(DB_IDENTIFIER, equalTo("true"))
            .withQueryParam(LIMIT_IDENTIFIER, equalTo(LIMIT_VALUE))
            .withQueryParam(FODSELSDATO_IDENTIFIER, equalTo(nin.getBirthDate()))
            .withQueryParam(PERSONNUMMER_IDENTIFIER, equalTo(nin.getPersonalNumber()))
            .willReturn(ok().withHeader("Content-Type", "application/json")
                .withBody(fsPersonSearchResponse(fsPerson))));
    }
    
    private String fsPersonSearchResponse(FsPerson fsPerson) {
        FsIdSearchResult searchResult = new FsIdSearchResult(fsPerson);
        return new FsPersonSearchResponse(List.of(searchResult)).toString();
    }
    
    private Nin randomNin() {
        return new Nin(randomString());
    }
    
    private FsIdNumber randomFsIsNumber() {
        return new FsIdNumber(randomInteger());
    }
}

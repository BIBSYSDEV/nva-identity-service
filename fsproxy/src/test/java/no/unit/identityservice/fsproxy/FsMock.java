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
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.identityservice.fsproxy.model.course.FsCourse;
import no.unit.identityservice.fsproxy.model.course.FsSemester;
import no.unit.identityservice.fsproxy.model.course.FsSubject;
import no.unit.identityservice.fsproxy.model.fagperson.FsPossibleStaffPerson;
import no.unit.identityservice.fsproxy.model.person.FsIdNumber;
import no.unit.identityservice.fsproxy.model.person.FsIdSearchResult;
import no.unit.identityservice.fsproxy.model.person.FsPerson;
import no.unit.identityservice.fsproxy.model.person.FsPersonSearchResponse;
import no.unit.identityservice.fsproxy.model.person.Nin;
import no.unit.nva.stubs.WiremockHttpClient;

public class FsMock {

    public static final FsPossibleStaffPerson EMPTY_FAG_PERSON = null;
    private final Map<Nin, FsPerson> personEntries;
    private WireMockServer server;
    private URI fsHostUri;
    private HttpClient httpClient;

    public FsMock() {
        this.personEntries = new ConcurrentHashMap<>();
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

    public List<FsCourse> createResponseForCourses() {
        var maxNumberOfCourses = 5;
        var courses = IntStream.range(0, randomInteger(maxNumberOfCourses))
                          .boxed()
                          .map(index -> new FsCourse(new FsSubject(randomString()), 1,
                                                     new FsSemester(randomString(), randomString())))
                          .collect(Collectors.toList());
        return courses;
    }

    public

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
        var x = new FsPersonSearchResponse(Collections.emptyList()).toString();
        return x;
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

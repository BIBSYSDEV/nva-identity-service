package no.unit.identityservice.fsproxy.util;

import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.identityservice.fsproxy.model.Fagperson.FsPossibleStaffPerson;
import no.unit.identityservice.fsproxy.model.Person.FsIdNumber;
import no.unit.identityservice.fsproxy.model.Person.FsIdSearchResult;
import no.unit.identityservice.fsproxy.model.Person.FsNin;
import no.unit.identityservice.fsproxy.model.Person.FsPerson;
import no.unit.identityservice.fsproxy.model.Person.FsPersonSearchResponse;
import no.unit.nva.commons.json.JsonUtils;

public class StudentGenerator {

    private final FsPersonSearchResponse fsPersonSearchResponse;
    private final int numberOfPersons;

    public StudentGenerator() {
        numberOfPersons = 1;
        fsPersonSearchResponse = generateRandomFsPersonSearchResponse();
    }

    public StudentGenerator(int specifiedNumberOfPersons) {
        numberOfPersons = specifiedNumberOfPersons;
        fsPersonSearchResponse = generateRandomFsPersonSearchResponse();
    }

    public String convertToJson() {
        try {
            return JsonUtils.dtoObjectMapper.writeValueAsString(fsPersonSearchResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public FsPersonSearchResponse getPersonGenerator() {
        return fsPersonSearchResponse;
    }

    public FsNin generateNin() {
        return new FsNin(randomInteger().toString());
    }

    private FsPersonSearchResponse generateRandomFsPersonSearchResponse() {
        return numberOfPersons > 0
                   ?
                   new FsPersonSearchResponse(IntStream.range(0, 1)
                                                  .boxed()
                                                  .map(index -> generateRandomFsIdSearchResult())
                                                  .collect(
                                                      Collectors.toList())) :
                                                                                new FsPersonSearchResponse(List.of());
    }

    private FsIdSearchResult generateRandomFsIdSearchResult() {
        return new FsIdSearchResult(generateRandomFsPerson());
    }

    private FsPerson generateRandomFsPerson() {
        return new FsPerson(generateRandomFsIdNumber(), generateRandomFagperson());
    }

    private FsIdNumber generateRandomFsIdNumber() {
        return new FsIdNumber(randomInteger());
    }

    private FsPossibleStaffPerson generateRandomFagperson() {
        return new FsPossibleStaffPerson(randomBoolean());
    }



}

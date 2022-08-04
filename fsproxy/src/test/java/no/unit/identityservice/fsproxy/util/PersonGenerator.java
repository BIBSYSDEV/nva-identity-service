package no.unit.identityservice.fsproxy.util;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.identityservice.fsproxy.model.Fagperson.FsRoleToStaffPerson;
import no.unit.identityservice.fsproxy.model.Fagperson.FsRolesToPersonSearchResult;
import no.unit.identityservice.fsproxy.model.Person.FsIdNumber;
import no.unit.nva.commons.json.JsonUtils;

public class PersonGenerator {

    private final FsRolesToPersonSearchResult fsRolesToPersonSearchResult;

    public PersonGenerator() {
        fsRolesToPersonSearchResult = generateRandomFagperson();
    }

    public FsRolesToPersonSearchResult getFsRolesToFagpersonSearchResult() {
        return fsRolesToPersonSearchResult;
    }

    public String convertToJson() {
        try {
            return JsonUtils.dtoObjectMapper.writeValueAsString(fsRolesToPersonSearchResult);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private FsRolesToPersonSearchResult generateRandomFagperson() {
        return new FsRolesToPersonSearchResult(generateRandomRolesList());
    }

    private List<FsRoleToStaffPerson> generateRandomRolesList() {
        var maxNumberOfRoles = 10;
        return IntStream.range(0, randomInteger(maxNumberOfRoles))
                   .boxed()
                   .map(index -> generateRandomRole())
                   .collect(Collectors.toList());
    }
    private FsRoleToStaffPerson generateRandomRole() {
        return new FsRoleToStaffPerson(randomString());
    }

    public FsIdNumber generateIdNumber() {
        return new FsIdNumber(randomInteger());
    }

}

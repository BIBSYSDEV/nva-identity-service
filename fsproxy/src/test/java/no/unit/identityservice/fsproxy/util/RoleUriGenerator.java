package no.unit.identityservice.fsproxy.util;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.identityservice.fsproxy.model.Fagperson.FsRoleToStaffPerson;
import no.unit.nva.commons.json.JsonUtils;

public class RoleUriGenerator {

    private final FsRoleToStaffPerson fsRole;

    public RoleUriGenerator() {
        this.fsRole = generateRole();
    }

    public FsRoleToStaffPerson getFsRole() {
        return fsRole;
    }

    public FsRoleToStaffPerson generateRole() {
        return new FsRoleToStaffPerson(randomInteger() + "," + randomInteger());
    }

    public String convertToJson() {
        try {
            return JsonUtils.dtoObjectMapper.writeValueAsString(fsRole);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

package no.unit.nva.handlers;

import java.util.List;
import no.unit.nva.useraccessservice.model.RoleDto;

public interface RoleSource {
    List<RoleDto> roles();
}

package no.unit.nva.handlers;

import no.unit.nva.useraccessservice.model.RoleDto;

import java.util.List;

public interface RoleSource {
    List<RoleDto> roles();
}

package no.unit.nva.handlers;

import java.util.List;
import no.unit.nva.useraccessservice.model.RoleDto;

@FunctionalInterface
public interface RoleSource {
  List<RoleDto> roles();
}

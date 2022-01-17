package no.unit.nva.database;

import static no.unit.nva.database.RoleService.ROLE_NOT_FOUND_MESSAGE;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.useraccessmanagement.dao.UserDao;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class DatabaseServiceImplTest extends DatabaseAccessor {


    public static final String SOME_USERNAME = "someUsername";


    private DatabaseServiceImpl databaseService;





}
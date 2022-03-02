package no.unit.nva.useraccessmanagement.model;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.useraccessmanagement.model.ViewingScope.defaultViewingScope;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.jr.ob.JSON;
import java.io.IOException;
import java.util.Set;
import nva.commons.apigatewayv2.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

public class ViewingScopeTest {

    @Test
    void viewingScopeIsSerializedWithType() throws BadRequestException, IOException {
        ViewingScope viewingScope = new ViewingScope(Set.of(randomCristinOrgId()), Set.of(randomCristinOrgId()));
        var jsonString = JSON.std.asString(viewingScope);
        var jsonMap = JSON.std.mapFrom(jsonString);
        assertThat(jsonMap, hasEntry("type","ViewingScope"));
    }


    @Test
    void defaultViewingScopeReturnsViewingScope() {
        ViewingScope viewingScope = defaultViewingScope(randomCristinOrgId());
        assertThat(viewingScope.getIncludedUnits().size(), is(equalTo(1)));
    }




}
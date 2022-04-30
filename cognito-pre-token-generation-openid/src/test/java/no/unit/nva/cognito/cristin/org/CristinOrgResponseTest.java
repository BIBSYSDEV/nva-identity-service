package no.unit.nva.cognito.cristin.org;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.io.IOException;
import java.nio.file.Path;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.usercreation.cristin.org.CristinOrgResponse;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

class CristinOrgResponseTest {

    @Test
    void shouldReturnOwnIdAsTopLevelOrgWhenIsTopLevelOrg() throws IOException {
        var input = IoUtils.stringFromResources(Path.of("cristin", "org", "top_level_org.json"));
        var org = JsonConfig.beanFrom(CristinOrgResponse.class, input);
        assertThat(org.extractTopOrgUri().toString(), is(equalTo(org.getOrgId())));
    }

    @Test
    void shouldReturnFirstLevelPartOfIdAsTopLevelOrgWhenOrgIsExactlyUnderTopLevel() throws IOException {
        var input = IoUtils.stringFromResources(Path.of("cristin", "org", "one_level_under_top_level.json"));
        var org = JsonConfig.beanFrom(CristinOrgResponse.class, input);
        String topLevelOrgId = org.getPartOf().get(0).getOrgId();
        assertThat(org.extractTopOrgUri().toString(), is(equalTo(topLevelOrgId)));
    }

    @Test
    void shouldReturnSecondLevelPartOfIdAsTopLevelOrgWhenOrgIsTwoLevelsUnderTopLevel() throws IOException {
        var input = IoUtils.stringFromResources(Path.of("cristin", "org", "two_levels_under_top_level.json"));
        var org = JsonConfig.beanFrom(CristinOrgResponse.class, input);
        String topLevelOrgId = org.getPartOf().get(0).getPartOf().get(0).getOrgId();
        assertThat(org.extractTopOrgUri().toString(), is(equalTo(topLevelOrgId)));
    }
}
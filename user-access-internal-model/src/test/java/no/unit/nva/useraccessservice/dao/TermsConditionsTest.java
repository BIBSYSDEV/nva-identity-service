package no.unit.nva.useraccessservice.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import org.junit.jupiter.api.Test;

import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class TermsConditionsTest {
    @Test
    void shouldMakeRoundTripWithoutLossOfInformation() throws JsonProcessingException {
        var randomLicenseInfo = randomLicenseInfo();
        var objectAsString = JsonUtils.dtoObjectMapper.writeValueAsString(randomLicenseInfo);
        var regeneratedObject = JsonUtils.dtoObjectMapper.readValue(objectAsString, TermsConditionsResponse.class);
        assertThat(randomLicenseInfo, is(equalTo(regeneratedObject)));
    }

    @Test
    void shouldMakeRoundTripWithoutLossOfInformationWhenLicenseInfoIsCreatedFromDao() throws JsonProcessingException {
        var randomLicenseInfoDao = randomLicenseInfoDao();
        var objectAsString = JsonUtils.dtoObjectMapper.writeValueAsString(randomLicenseInfoDao);
        var regeneratedObject = JsonUtils.dtoObjectMapper.readValue(objectAsString, TermsConditions.class);
        assertThat(randomLicenseInfoDao, is(equalTo(regeneratedObject)));
    }

    @Test
    void shouldMakeRoundTripWithoutLossOfInformationWhenLicenseInfoIsCreatedFromDaoAndBack()
            throws JsonProcessingException {
        var randomLicenseInfoDao = randomLicenseInfoDao();
        var licenseInfo =  new TermsConditionsResponse.Builder()
                .withTermsConditionsUri(randomLicenseInfoDao.termsConditionsUri())
                .build();
        var objectAsString = JsonUtils.dtoObjectMapper.writeValueAsString(licenseInfo);
        var regeneratedObject = JsonUtils.dtoObjectMapper.readValue(objectAsString, TermsConditionsResponse.class);
        assertThat(licenseInfo, is(equalTo(regeneratedObject)));
    }

    private TermsConditionsResponse randomLicenseInfo() {
        return new TermsConditionsResponse.Builder()
                .withTermsConditionsUri(randomUri())
                .build();
    }

    private TermsConditions randomLicenseInfoDao() {
        var lostInstant = randomInstant();
        return TermsConditions.builder()
                .id(randomUri())
                .type("TermsConditions")
                .created(lostInstant)
                .modified(lostInstant)
                .termsConditionsUri(randomUri())
                .build();
    }
}
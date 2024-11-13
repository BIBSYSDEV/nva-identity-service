package no.unit.nva.useraccessservice.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.database.interfaces.DataAccessService;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TermsConditionsTest {


    private static final DataAccessService<TermsConditions> mockedService = mock(DataAccessService.class);

    @BeforeAll
    public static void init() throws NotFoundException {
        when(mockedService.fetch(any())).thenReturn(randomTermsConditions().build());

    }


    @Test
    void shouldMakeRoundTripWithoutLossOfInformation() throws JsonProcessingException {
        var randomLicenseInfo = randomTermsConditionsResponse();
        var objectAsString = JsonUtils.dtoObjectMapper.writeValueAsString(randomLicenseInfo);
        var regeneratedObject = JsonUtils.dtoObjectMapper.readValue(objectAsString, TermsConditionsResponse.class);
        assertThat(randomLicenseInfo, is(equalTo(regeneratedObject)));
    }

    @Test
    void shouldMakeRoundTripWithoutLossOfInformationWhenLicenseInfoIsCreatedFromDao() throws JsonProcessingException {
        var randomLicenseInfoDao = randomTermsConditions().build();
        var objectAsString = JsonUtils.dtoObjectMapper.writeValueAsString(randomLicenseInfoDao);
        var regeneratedObject = JsonUtils.dtoObjectMapper.readValue(objectAsString, TermsConditions.class);
        assertThat(randomLicenseInfoDao, is(equalTo(regeneratedObject)));
    }

    @Test
    void shouldMakeRoundTripWithoutLossOfInformationWhenLicenseInfoIsCreatedFromDaoAndBack()
            throws JsonProcessingException {
        var randomLicenseInfoDao = randomTermsConditions().build();
        var licenseInfo =  new TermsConditionsResponse.Builder()
                .withTermsConditionsUri(randomLicenseInfoDao.termsConditionsUri())
                .build();
        var objectAsString = JsonUtils.dtoObjectMapper.writeValueAsString(licenseInfo);
        var regeneratedObject = JsonUtils.dtoObjectMapper.readValue(objectAsString, TermsConditionsResponse.class);
        assertThat(licenseInfo, is(equalTo(regeneratedObject)));
    }

    @Test
    void shouldMergeTermsConditionsSuccessfully() {
        var firstTermsConditions = randomTermsConditions().build();
        var secondTermsConditions = randomTermsConditions().build();
        var mergedTermsConditions = firstTermsConditions.merge(secondTermsConditions);

        assertThat(mergedTermsConditions.id(), is(equalTo(firstTermsConditions.id())));
        assertThat(mergedTermsConditions.type(), is(equalTo(firstTermsConditions.type())));
        assertThat(mergedTermsConditions.created(), is(equalTo(firstTermsConditions.created())));
        assertThat(mergedTermsConditions.owner(), is(equalTo(firstTermsConditions.owner())));
        assertThat(mergedTermsConditions.modifiedBy(), is(equalTo(secondTermsConditions.modifiedBy())));
        assertThat(mergedTermsConditions.termsConditionsUri(), is(equalTo(secondTermsConditions.termsConditionsUri())));
    }

    @Test
    void shouldFetchTermsConditions() throws NotFoundException {
        var firstTerms = randomTermsConditions().build();
        when(mockedService.fetch(any()))
                .thenReturn(firstTerms);
        var fetchedTermsConditions = firstTerms.fetch(mockedService);

        assertThat(fetchedTermsConditions, is(equalTo(firstTerms)));
    }

    @Test
    void shouldPersistTermsConditions() throws NotFoundException {
        var firstTerms = randomTermsConditions().build();
        when(mockedService.fetch(any()))
                .thenReturn(firstTerms);
        var fetchedTermsConditions =firstTerms.upsert(mockedService);

        assertThat(fetchedTermsConditions, is(equalTo(firstTerms)));

    }


    @Test
    void shouldReturnEmptyOptionalWhenFetchingTermsConditionsThrowsNotFoundException() throws NotFoundException {
        when(mockedService.fetch(any()))
                .thenThrow(new NotFoundException(""));
        var fetchedTermsConditions = Optional.ofNullable(new TermsConditions.Builder().build())
                .flatMap(termsConditions -> {
                    try {
                        return Optional.of(termsConditions.fetch(mockedService));
                    } catch (NotFoundException e) {
                        return Optional.empty();
                    }
                });

        assertThat(fetchedTermsConditions, is(equalTo(Optional.empty())));
    }

    private TermsConditionsResponse randomTermsConditionsResponse() {
        return new TermsConditionsResponse.Builder()
                .withTermsConditionsUri(randomUri())
                .build();
    }

    private static TermsConditions.Builder randomTermsConditions() {
        var lostInstant = randomInstant();
        return TermsConditions.builder()
                .id(randomUri())
                .type("TermsConditions")
                .created(lostInstant)
                .modified(lostInstant)
                .modifiedBy(randomUri())
                .termsConditionsUri(randomUri());
    }
}
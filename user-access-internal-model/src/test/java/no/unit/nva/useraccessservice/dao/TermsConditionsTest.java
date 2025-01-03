package no.unit.nva.useraccessservice.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.database.interfaces.DataAccessService;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TermsConditionsTest {


	private static final DataAccessService<TermsConditions> mockedService =
		mock(DataAccessService.class);

	@BeforeAll
	public static void init() throws NotFoundException {
		when(mockedService.fetch(any())).thenReturn(randomTermsConditions().build());

	}

	private static TermsConditions.Builder randomTermsConditions() {
		var lostInstant = randomInstant();
		return TermsConditions.builder()
			.id(randomUri().toString())
			.created(lostInstant)
			.modified(lostInstant)
			.modifiedBy(randomUri().toString())
			.termsConditionsUri(randomUri());
	}

	@Test
	void shouldMakeRoundTripWithoutLossOfInformation() throws JsonProcessingException {
		var randomLicenseInfo = randomTermsConditionsResponse();
		var objectAsString = dtoObjectMapper.writeValueAsString(randomLicenseInfo);
		var regeneratedObject = dtoObjectMapper.readValue(objectAsString, TermsConditionsResponse.class);
		assertThat(randomLicenseInfo, is(equalTo(regeneratedObject)));
	}

	private TermsConditionsResponse randomTermsConditionsResponse() {
		return new TermsConditionsResponse.Builder()
			.withTermsConditionsUri(randomUri())
			.build();
	}

	@Test
	void shouldMakeRoundTripWithoutLossOfInformationWhenLicenseInfoIsCreatedFromDao() throws JsonProcessingException {
		var randomLicenseInfoDao = randomTermsConditions().build();
		var objectAsString = dtoObjectMapper.writeValueAsString(randomLicenseInfoDao);
		var regeneratedObject = dtoObjectMapper.readValue(objectAsString, TermsConditions.class);
		assertThat(randomLicenseInfoDao, is(equalTo(regeneratedObject)));
	}

	@Test
	void shouldMakeRoundTripWithoutLossOfInformationWhenLicenseInfoIsCreatedFromDaoAndBack()
		throws JsonProcessingException {
		var randomLicenseInfoDao = randomTermsConditions().build();
		var licenseInfo = new TermsConditionsResponse.Builder()
			.withTermsConditionsUri(randomLicenseInfoDao.termsConditionsUri())
			.build();
		var objectAsString = dtoObjectMapper.writeValueAsString(licenseInfo);
		var regeneratedObject = dtoObjectMapper.readValue(objectAsString, TermsConditionsResponse.class);
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
		assertThat(mergedTermsConditions.createdBy(), is(equalTo(firstTermsConditions.createdBy())));
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
		var fetchedTermsConditions = firstTerms.upsert(mockedService);

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
}
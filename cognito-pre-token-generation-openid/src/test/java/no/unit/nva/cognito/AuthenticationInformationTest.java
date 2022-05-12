package no.unit.nva.cognito;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.useraccessservice.usercreation.PersonInformation;
import no.unit.nva.useraccessservice.usercreation.cristin.PersonAffiliation;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;
import org.junit.jupiter.api.Test;

class AuthenticationInformationTest {

    @Test
    void overridenMethodsCallDecoratedImplObject() throws InvocationTargetException, IllegalAccessException {
        var personInfo = new PersonInformationSpy();
        var authInfo = new AuthenticationInformation(personInfo);
        var personInfoMethods = collectMethodsDeclaredByTheInterfaceExcludingDefaultMethods();
        for (var method : personInfoMethods) {
            var parameters = method.getParameterTypes();
            var arguments = Arrays.stream(parameters).map(this::getMock).toArray();
            method.invoke(authInfo, arguments);
        }

        assertThat(personInfo.getActivations(), is(equalTo(personInfoMethods.size())));
    }

    private List<Method> collectMethodsDeclaredByTheInterfaceExcludingDefaultMethods() {
        return Arrays.stream(PersonInformation.class.getDeclaredMethods())
            .collect(Collectors.toList());
    }

    private Object getMock(Class<?> type) {
        return typeCanBeMocked(type) ? mock(type) : realExampleValue();
    }

    private URI realExampleValue() {
        return randomUri();
    }

    private boolean typeCanBeMocked(Class<?> type) {
        return !type.equals(URI.class);
    }

    private static class PersonInformationSpy implements PersonInformation {

        private final AtomicInteger activations = new AtomicInteger(0);

        @Override
        public Set<CustomerDto> getActiveCustomers() {
            registerActivation();
            return null;
        }

        @Override
        public void setActiveCustomers(Set<CustomerDto> activeCustomers) {
            registerActivation();
        }

        @Override
        public String getOrgFeideDomain() {
            registerActivation();
            return null;
        }

        @Override
        public String getPersonFeideIdentifier() {
            registerActivation();
            return null;
        }

        @Override
        public Optional<CristinPersonResponse> getCristinPersonResponse() {
            registerActivation();
            return Optional.empty();
        }

        @Override
        public void setCristinPersonResponse(CristinPersonResponse cristinResponse) {
            registerActivation();
        }

        @Override
        public URI getOrganizationAffiliation(URI parentInstitution) {
            registerActivation();
            return null;
        }

        @Override
        public List<PersonAffiliation> getPersonAffiliations() {
            registerActivation();
            return null;
        }

        @Override
        public void setPersonAffiliations(List<PersonAffiliation> affiliationInformation) {
            registerActivation();
        }

        @Override
        public Optional<URI> getPersonRegistryId() {
            registerActivation();
            return Optional.empty();
        }

        public int getActivations() {
            return activations.get();
        }

        private void registerActivation() {
            activations.incrementAndGet();
        }
    }
}
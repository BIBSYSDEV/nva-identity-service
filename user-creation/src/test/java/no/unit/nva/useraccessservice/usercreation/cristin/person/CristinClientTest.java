package no.unit.nva.useraccessservice.usercreation.cristin.person;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.Test;

class CristinClientTest {
    
    public static final int NO_INVOCATIONS = 0;
    public static final int TWO_ATTEMPTS_TO_READ_SECRET_ID_AND_SECRET_KEY = 4; //We read two secret values two times.
    
    private final SecretsReader secretsReader = new FakeSecretsReader();
    private final AtomicInteger numberOfInvocations = new AtomicInteger(NO_INVOCATIONS);
    
    @Test
    void shouldReadLatestCognitoCredentialsOnEveryOperation()
        throws IOException, BadGatewayException, InterruptedException {
        var client = CristinClient.defaultClient(secretsReader);
        startCountingInvocationsAfterTheInitializationOfTheClientThatWillHappenOnTheHotInstance();
        performPossiblyFailingActionDueToNotBeingConnectedToRealServer(client);
        performPossiblyFailingActionDueToNotBeingConnectedToRealServer(client);
        assertThat(numberOfInvocations.get(), is(equalTo(TWO_ATTEMPTS_TO_READ_SECRET_ID_AND_SECRET_KEY)));
    }
    
    private void startCountingInvocationsAfterTheInitializationOfTheClientThatWillHappenOnTheHotInstance() {
        numberOfInvocations.set(NO_INVOCATIONS);
    }
    
    private void performPossiblyFailingActionDueToNotBeingConnectedToRealServer(CristinClient client) {
        attempt(() -> client.sendRequestToCristin(new NationalIdentityNumber(randomString())));
    }
    
    private class FakeSecretsReader extends SecretsReader {
        
        @Override
        public String fetchSecret(String secretName, String secretKey) {
            numberOfInvocations.incrementAndGet();
            return randomString();
        }
    }
}
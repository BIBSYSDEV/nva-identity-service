package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static no.unit.nva.handlers.HandlerWithEventualConsistency.FAILED_TO_FETCH_OBJECT;
import static no.unit.nva.handlers.HandlerWithEventualConsistency.MAX_EFFORTS_FOR_FETCHING_OBJECT;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class HandlerWithEventualConsistencyTest {

    public static final HandlerWithEventualConsistency<String, String> handler = getHandlerWithEventualConsistency();

    private static HandlerWithEventualConsistency<String, String> getHandlerWithEventualConsistency() {
        return new HandlerWithEventualConsistency<>(String.class) {

            @Override
            protected void validateRequest(String s, RequestInfo requestInfo, Context context)
                throws ApiGatewayException {
                //Do nothing
            }

            @Override
            protected String processInput(String input, RequestInfo requestInfo, Context context) {
                return input;
            }

            @Override
            protected Integer getSuccessStatusCode(String input, String output) {
                return 200;
            }
        };
    }

    @Test
    void handlerShouldEventuallyReturnValue() {
        var expectedValue = randomString();
        AtomicInteger counter = new AtomicInteger();
        Callable callable = () -> {
            counter.getAndIncrement();
            if (counter.get() < 3) {
                throw new RuntimeException("Not yet");
            }
            return expectedValue;
        };

        var result = handler.getEventuallyConsistent(callable);
        assertThat(result, is(equalTo(Optional.of(expectedValue))));
    }

    @Test
    void errorsShouldBeLogged() {
        var testingAppender = LogUtils.getTestingAppenderForRootLogger();

        var counter = new AtomicInteger();
        Callable callable = () -> {
            counter.getAndIncrement();
            if (counter.get() < 3) {
                throw new RuntimeException("Not yet");
            }
            return randomString();
        };

        handler.getEventuallyConsistent(callable);

        var expectedLogValue = String.format(FAILED_TO_FETCH_OBJECT, 0, MAX_EFFORTS_FOR_FETCHING_OBJECT);

        assertThat(testingAppender.getMessages(), containsString(expectedLogValue));
    }

}
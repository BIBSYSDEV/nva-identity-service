package no.unit.nva.database.interfaces;

import nva.commons.core.Environment;

import java.util.Map;
import java.util.Optional;

public interface WithEnvironment {

    String DEFAULT_ENV_VALUE = "*";

    /**
     * mock environment. Returns "*" for
     *
     * @return an Environment that returns "*" for all env variables.
     */
    default Environment mockEnvironment() {
        return mockEnvironment(DEFAULT_ENV_VALUE);
    }

    /**
     * Mock environment.
     *
     * @param returnValueForAllEnvVariables the value to be returned for every env variable.
     * @return return the parameter for every env variable.
     */
    default Environment mockEnvironment(String returnValueForAllEnvVariables) {
        return new Environment() {
            @Override
            public Optional<String> readEnvOpt(String variableName) {
                return Optional.of(returnValueForAllEnvVariables);
            }

            @Override
            public String readEnv(String variableName) {
                return returnValueForAllEnvVariables;
            }
        };
    }

    /**
     * Mock environment.
     *
     * @param returnValueForAllEnvVariables the value to be returned for every env variable.
     * @return return the parameter for every env variable.
     */
    default Environment mockEnvironment(Map<String, String> envValues, String returnValueForAllEnvVariables) {
        return new Environment() {
            @Override
            public Optional<String> readEnvOpt(String variableName) {
                return Optional.ofNullable(readEnv(variableName));
            }

            @Override
            public String readEnv(String variableName) {
                if (envValues.containsKey(variableName)) {
                    return envValues.get(variableName);
                } else {
                    return returnValueForAllEnvVariables;
                }
            }
        };
    }
}

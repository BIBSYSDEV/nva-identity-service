package no.unit.nva.customer.model.interfaces;

public interface DoiAgent {

    String getName();

    String getPrefix();

    static DoiAgent randomDoiAgent(String randomString) {
        return new DoiAgent() {

            @Override
            public String getName() {
                return "agency-name-" + randomString;
            }

            @Override
            public String getPrefix() {
                return "10.000";
            }
        };
    }
}
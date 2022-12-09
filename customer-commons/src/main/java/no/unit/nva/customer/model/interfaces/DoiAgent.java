package no.unit.nva.customer.model.interfaces;

public interface DoiAgent {

    String getPrefix();

    String getUrl();

    String getName();


    static DoiAgent randomDoiAgent(String randomString) {
        return new DoiAgent() {

            @Override
            public String getPrefix() {
                return "10.000";
            }

            @Override
            public String getUrl() {
                return "mds." + randomString + ".datacite.org";
            }

            @Override
            public String getName() {
                return "agency-name-" + randomString;
            }
        };
    }
}
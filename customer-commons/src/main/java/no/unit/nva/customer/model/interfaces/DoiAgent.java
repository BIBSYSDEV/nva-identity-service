package no.unit.nva.customer.model.interfaces;

public interface DoiAgent {
    String DOI_AGENT = "doiagent";

    String getPrefix();

    String getUrl();

    String getUsername();

}
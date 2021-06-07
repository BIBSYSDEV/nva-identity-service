package no.unit.nva.customer.model.interfaces;

import java.net.URI;

public interface JsonLdSupport {

    URI getId();

    void setId(URI id);

    URI getContext();

    void setContext(URI context);
}

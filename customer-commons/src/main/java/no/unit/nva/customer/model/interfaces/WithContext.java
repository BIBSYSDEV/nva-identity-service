package no.unit.nva.customer.model.interfaces;

import java.net.URI;

public interface WithContext {

    URI getContext();

    void setContext(URI context);

}

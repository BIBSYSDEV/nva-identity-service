package no.unit.nva.customer.model.interfaces;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.customer.model.VocabularyStatus;

import java.net.URI;

@JsonTypeName("VocabularySetting")
public interface VocabularySetting {

    String getName();

    void setName(String name);

    URI getId();

    void setId(URI id);

    VocabularyStatus getStatus();

    void setStatus(VocabularyStatus status);

}

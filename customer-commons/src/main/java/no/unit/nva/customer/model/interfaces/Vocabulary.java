package no.unit.nva.customer.model.interfaces;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import no.unit.nva.customer.model.VocabularyStatus;

@SuppressWarnings("PMD.ExcessivePublicCount")
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = As.PROPERTY,
    property = "type")
@JsonTypeName("Vocabulary")
public interface Vocabulary {

    String getName();

    void setName(String name);

    URI getId();

    void setId(URI id);

    VocabularyStatus getStatus();

    void setStatus(VocabularyStatus status);
}

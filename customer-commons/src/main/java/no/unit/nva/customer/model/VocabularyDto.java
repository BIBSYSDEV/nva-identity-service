package no.unit.nva.customer.model;

import java.net.URI;
import java.util.Objects;
import no.unit.nva.customer.model.interfaces.Vocabulary;
import nva.commons.core.JacocoGenerated;

public class VocabularyDto implements Vocabulary {

    private String name;
    private URI id;
    private VocabularyStatus status;

    public VocabularyDto() {
    }

    @JacocoGenerated
    public VocabularyDto(String name, URI id, VocabularyStatus status) {
        this();
        this.name = name;
        this.id = id;
        this.status = status;
    }

    @JacocoGenerated
    @Override
    public String getName() {
        return name;
    }

    @JacocoGenerated
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @JacocoGenerated
    @Override
    public URI getId() {
        return id;
    }

    @JacocoGenerated
    @Override
    public void setId(URI id) {
        this.id = id;
    }

    @JacocoGenerated
    @Override
    public VocabularyStatus getStatus() {
        return status;
    }

    @JacocoGenerated
    @Override
    public void setStatus(VocabularyStatus status) {
        this.status = status;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getId(), getStatus());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VocabularyDto)) {
            return false;
        }
        VocabularyDto that = (VocabularyDto) o;
        return Objects.equals(getName(), that.getName())
               && Objects.equals(getId(), that.getId())
               && getStatus() == that.getStatus();
    }
}

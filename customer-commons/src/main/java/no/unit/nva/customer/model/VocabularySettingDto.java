package no.unit.nva.customer.model;

import no.unit.nva.customer.model.interfaces.VocabularySetting;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Objects;

public class VocabularySettingDto implements VocabularySetting {

    private String name;
    private URI id;
    private VocabularyStatus status;

    public VocabularySettingDto() {
    }

    @JacocoGenerated
    public VocabularySettingDto(String name, URI id, VocabularyStatus status) {
        this();
        this.name = name;
        this.id = id;
        this.status = status;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public URI getId() {
        return id;
    }

    @Override
    public void setId(URI id) {
        this.id = id;
    }

    @Override
    public VocabularyStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(VocabularyStatus status) {
        this.status = status;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VocabularySettingDto that = (VocabularySettingDto) o;
        return Objects.equals(name, that.name)
                && Objects.equals(id, that.id)
                && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, status);
    }
}

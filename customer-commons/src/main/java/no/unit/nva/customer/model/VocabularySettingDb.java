package no.unit.nva.customer.model;

import no.unit.nva.customer.model.interfaces.VocabularySetting;

import java.net.URI;
import java.util.Objects;

public class VocabularySettingDb implements VocabularySetting {

    private String name;
    private URI id;
    private VocabularyStatus status;

    public VocabularySettingDb() {
    }

    public VocabularySettingDb(String name, URI id, VocabularyStatus status) {
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VocabularySettingDb that = (VocabularySettingDb) o;
        return Objects.equals(name, that.name)
                && Objects.equals(id, that.id)
                && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, status);
    }
}

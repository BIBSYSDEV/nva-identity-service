package no.unit.nva.customer.model;

import java.net.URI;
import java.util.Objects;
import no.unit.nva.customer.model.interfaces.Vocabulary;


public class VocabularyDao implements Vocabulary {

    private String name;
    private URI id;
    private VocabularyStatus status;

    public VocabularyDao() {
    }

    public VocabularyDao(String name, URI id, VocabularyStatus status) {
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
    public int hashCode() {
        return Objects.hash(name, id, status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VocabularyDao that = (VocabularyDao) o;
        return Objects.equals(name, that.name)
               && Objects.equals(id, that.id)
               && status == that.status;
    }

    public VocabularyDto toVocabularySettingsDto() {
        return new VocabularyDto(this.getName(), this.getId(), this.getStatus());
    }

    public static VocabularyDao fromVocabularySettingsDto(VocabularyDto vocabularySettingDto) {
        return new VocabularyDao(vocabularySettingDto.getName(),
                                 vocabularySettingDto.getId(),
                                 vocabularySettingDto.getStatus());
    }
}

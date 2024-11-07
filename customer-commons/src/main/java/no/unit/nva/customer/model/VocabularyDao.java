package no.unit.nva.customer.model;

import no.unit.nva.customer.model.dynamo.converters.VocabularyConverterProvider;
import no.unit.nva.customer.model.dynamo.converters.VocabularySetConverter;
import no.unit.nva.customer.model.interfaces.Vocabulary;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.net.URI;
import java.util.Objects;
import java.util.Set;

@DynamoDbBean(converterProviders = {VocabularyConverterProvider.class, DefaultAttributeConverterProvider.class})
public class VocabularyDao implements Vocabulary {

    public static final AttributeConverter<Set<VocabularyDao>> SET_CONVERTER = new VocabularySetConverter();
    public static final TableSchema<VocabularyDao> SCHEMA = TableSchema.fromClass(VocabularyDao.class);
    public static final String STATUS_FIELD = "status";
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

    public static VocabularyDao fromVocabularySettingsDto(VocabularyDto vocabularySettingDto) {
        return new VocabularyDao(vocabularySettingDto.getName(),
                vocabularySettingDto.getId(),
                vocabularySettingDto.getStatus());
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
    @DynamoDbAttribute(STATUS_FIELD)
    public VocabularyStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(VocabularyStatus status) {
        this.status = status;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getName(), getId(), getStatus());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VocabularyDao)) {
            return false;
        }
        VocabularyDao that = (VocabularyDao) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getId(), that.getId())
                && getStatus() == that.getStatus();
    }

    public VocabularyDto toVocabularySettingsDto() {
        return new VocabularyDto(this.getName(), this.getId(), this.getStatus());
    }
}

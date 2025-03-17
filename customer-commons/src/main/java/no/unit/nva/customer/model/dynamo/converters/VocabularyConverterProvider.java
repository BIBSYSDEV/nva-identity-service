package no.unit.nva.customer.model.dynamo.converters;

import no.unit.nva.customer.model.VocabularyDao;
import no.unit.nva.customer.model.VocabularyStatus;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

import java.util.Set;

public class VocabularyConverterProvider implements AttributeConverterProvider {

    @Override
    public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
        if (isVocabularySet(enhancedType)) {
            return (AttributeConverter<T>) VocabularyDao.SET_CONVERTER;
        }
        if (isVocabularyStatus(enhancedType)) {
            return (AttributeConverter<T>) VocabularyStatus.VOCABULARY_STATUS_CONVERTER;
        }
        return null;
    }

    private <T> boolean isVocabularyStatus(EnhancedType<T> enhancedType) {
        return VocabularyStatus.class.equals(enhancedType.rawClass());
    }

    private <T> boolean isVocabularySet(EnhancedType<T> enhancedType) {
        return Set.class.equals(enhancedType.rawClass())
            && enhancedType.rawClassParameters().size() == 1
            && EnhancedType.of(VocabularyDao.class).equals(enhancedType.rawClassParameters().get(0));
    }
}

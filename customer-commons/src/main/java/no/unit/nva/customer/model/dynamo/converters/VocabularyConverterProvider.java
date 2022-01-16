package no.unit.nva.customer.model.dynamo.converters;

import java.util.Set;
import no.unit.nva.customer.model.VocabularyDao;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

public class VocabularyConverterProvider implements AttributeConverterProvider {

    @Override
    public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
        if (isVocabularySet(enhancedType)) {
            return (AttributeConverter<T>) VocabularyDao.SET_CONVERTER;
        }
        return null;
    }

    private <T> boolean isVocabularySet(EnhancedType<T> enhancedType) {
        return Set.class.equals(enhancedType.rawClass())
               && enhancedType.rawClassParameters().size() == 1
               && EnhancedType.of(VocabularyDao.class).equals(enhancedType.rawClassParameters().get(0));
    }
}

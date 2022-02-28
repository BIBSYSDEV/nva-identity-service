package no.unit.nva.customer.model.interfaces;

import java.util.Set;

public interface WithVocabulary<T extends Vocabulary> {

    Set<T> getVocabularies();

    void setVocabularies(Set<T> vocabularies);

}

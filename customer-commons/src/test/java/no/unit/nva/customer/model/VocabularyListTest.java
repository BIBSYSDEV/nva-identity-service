package no.unit.nva.customer.model;

import org.junit.jupiter.api.Test;

import static no.unit.nva.customer.testing.CustomerDataGenerator.randomVocabularies;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VocabularyListTest {

    public static VocabularyDto randomVocabulary() {
        return new VocabularyDto(randomString(), randomUri(), randomElement(VocabularyStatus.values()));
    }

    @Test
    void shouldSerializeAndDeserialize() {
        var vlist = randomVocabularyList();
        assertThat(vlist, doesNotHaveEmptyValues());
        var json = vlist.toString();
        var deserialized = VocabularyList.fromJson(json);
        assertThat(deserialized, is(equalTo(vlist)));
    }

    private VocabularyList randomVocabularyList() {
        return new VocabularyList(randomUri(), randomVocabularies());
    }

    @Test
    void shouldThrowExceptionWhenFailingToParse() {
        var illegalString = randomString();
        assertThrows(Exception.class, () -> VocabularyList.fromJson(illegalString));
    }


}
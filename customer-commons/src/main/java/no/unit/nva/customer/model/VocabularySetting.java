package no.unit.nva.customer.model;

import java.net.URI;
import java.util.Objects;

public class VocabularySetting {

    private String name;
    private URI id;
    private VocabularyStatus status;

    public VocabularySetting() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    public VocabularyStatus getStatus() {
        return status;
    }

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
        VocabularySetting that = (VocabularySetting) o;
        return Objects.equals(name, that.name)
                && Objects.equals(id, that.id)
                && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, status);
    }

    public static final class Builder {

        private String name;
        private URI id;
        private VocabularyStatus status;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withId(URI id) {
            this.id = id;
            return this;
        }

        public Builder withStatus(VocabularyStatus status) {
            this.status = status;
            return this;
        }

        public VocabularySetting build() {
            VocabularySetting vocabularySetting = new VocabularySetting();
            vocabularySetting.setName(name);
            vocabularySetting.setId(id);
            vocabularySetting.setStatus(status);
            return vocabularySetting;
        }
    }
}

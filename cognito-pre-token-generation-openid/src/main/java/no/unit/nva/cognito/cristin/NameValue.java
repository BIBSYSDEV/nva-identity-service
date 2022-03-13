package no.unit.nva.cognito.cristin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class NameValue {

    public static final String LAST_NAME_TYPE = "LastName";
    public static final String FIRST_NAME_TYPE = "FirstName";
    private String type;
    private String value;

    public NameValue() {

    }

    public static NameValue firstName(String value) {
        NameValue nameValue = new NameValue();
        nameValue.setType(FIRST_NAME_TYPE);
        nameValue.setValue(value);
        return nameValue;
    }

    public static NameValue lastName(String value) {
        NameValue nameValue = new NameValue();
        nameValue.setType(LAST_NAME_TYPE);
        nameValue.setValue(value);
        return nameValue;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getType(), getValue());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NameValue)) {
            return false;
        }
        NameValue nameValue = (NameValue) o;
        return Objects.equals(getType(), nameValue.getType()) && Objects.equals(getValue(),
                                                                                nameValue.getValue());
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @JsonIgnore
    public boolean isFirstName() {
        return FIRST_NAME_TYPE.equals(getType());
    }

    @JsonIgnore
    public boolean isLastName() {
        return LAST_NAME_TYPE.equals(getType());
    }
}

package no.unit.nva.customer.model;

import com.google.common.base.Objects;
import nva.commons.core.JacocoGenerated;

public class LoginMethods {

    private boolean feide;
    private boolean minId;
    private boolean helseId;

    public boolean isFeide() {
        return feide;
    }

    public void setFeide(boolean feide) {
        this.feide = feide;
    }

    public boolean isMinId() {
        return minId;
    }

    public void setMinId(boolean minId) {
        this.minId = minId;
    }

    public boolean isHelseId() {
        return helseId;
    }

    public void setHelseId(boolean helseId) {
        this.helseId = helseId;
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
        LoginMethods that = (LoginMethods) o;
        return isFeide() == that.isFeide()
                && isMinId() == that.isMinId()
                && isHelseId() == that.isHelseId();
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hashCode(isFeide(), isMinId(), isHelseId());
    }
}

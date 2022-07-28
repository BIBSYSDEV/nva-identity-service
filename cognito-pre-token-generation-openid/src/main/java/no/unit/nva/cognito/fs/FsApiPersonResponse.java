package no.unit.nva.cognito.fs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class FsApiPersonResponse {

    @JsonProperty("items")
    private List<Item> items;

    @JsonCreator
    public FsApiPersonResponse(@JsonProperty("items") List<Item> items) {
        this.items = items;
    }

    public List<Item> getItems() {
        return items;
    }

    public static class Item {

        @JsonProperty("id")
        private Id id;

        @JsonCreator
        public Item(@JsonProperty("id") Id id) {
            this.id = id;
        }

        public Id getId() {
            return id;
        }
    }


    public static class Id {

        @JsonProperty("personlopenummer")
        private Integer personlopenummer;

        @JsonCreator
        public Id(@JsonProperty("personlopenummer") Integer personlopenummer) {
            this.personlopenummer = personlopenummer;
        }

        public Integer getPersonlopenummer() {
            return personlopenummer;
        }

        public String getPersonlopenummerAsString() {
            return String.valueOf(personlopenummer);
        }
    }
}


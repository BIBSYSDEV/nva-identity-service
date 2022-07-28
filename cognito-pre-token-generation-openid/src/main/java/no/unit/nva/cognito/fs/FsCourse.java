package no.unit.nva.cognito.fs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class FsCourse {
    @JsonProperty("items")
    private final List<Items> courses;

    @JsonCreator
    public FsCourse(@JsonProperty("items") final List<Items> courses) {
        this.courses = courses;
    }


    public List<Items> getCourses() {
        return courses;
    }

    public static class Items {

        @JsonProperty("items")
        private final List<Undervisning> items;
        @JsonCreator
        public Items(@JsonProperty("items") List<Undervisning> items) {
            this.items = items;
        }

        public List<Undervisning> getItems() {
            return items;
        }
    }

    public static class Undervisning {

        private final Emne emne;

        private final Semester semester;

        @JsonProperty("terminnummer")
        private final Integer terminnummer;
        @JsonCreator
        public Undervisning(Emne emne, Semester semester, Integer terminnummer) {
            this.emne = emne;
            this.semester = semester;
            this.terminnummer = terminnummer;
        }

        public Emne getEmne() {
            return emne;
        }

        public Semester getSemester() {
            return semester;
        }

        public Integer getTerminnummer() {
            return terminnummer;
        }


        public static class Emne {

            @JsonProperty("kode")
            private final String courseCode;

            public Emne(String courseCode) {
                this.courseCode = courseCode;
            }

            public String getCourseCode() {
                return courseCode;
            }
        }

        public static class Semester {

            @JsonProperty("ar")
            private final Integer year;


            public Semester(Integer year) {
                this.year = year;
            }

            public Integer getYear() {
                return year;
            }
        }


    }
}

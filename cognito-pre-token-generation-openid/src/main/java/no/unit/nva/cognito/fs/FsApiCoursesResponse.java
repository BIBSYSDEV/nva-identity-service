package no.unit.nva.cognito.fs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class FsApiCoursesResponse {

    @JsonProperty("items")
    private List<Item> items;

    @JsonCreator
    public FsApiCoursesResponse(@JsonProperty("items") final List<Item> items) {
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

        @JsonProperty("undervisning")
        private Undervisning undervisning;

        @JsonCreator
        public Id(@JsonProperty("undervisning") Undervisning undervisning) {
            this.undervisning = undervisning;
        }

        public Undervisning getUndervisning() {
            return undervisning;
        }
    }

    public static class Undervisning {

        @JsonProperty("emne")
        private Course course;

        @JsonProperty("semester")
        private Semester semester;

        @JsonCreator
        public Undervisning(@JsonProperty("emne") Course course, @JsonProperty("semester") Semester semester) {
            this.course = course;
            this.semester = semester;
        }

        private Course getCourse() {
            return course;
        }
    }

    public static class Course{

        @JsonProperty("kode")
        private String courseCode;

        @JsonCreator
        public Course(@JsonProperty("kode") String courseCode) {
            this.courseCode = courseCode;
        }

        private String getCourseCode() {
            return courseCode;
        }
    }

    public static class Semester{

        @JsonProperty("ar")
        private Integer year;

        @JsonProperty("termin")
        private String termin;

        @JsonCreator
        public Semester(@JsonProperty("ar") Integer year, @JsonProperty("termin") String termin) {
            this.year = year;
            this.termin = termin;
        }

        private Integer getYear() {
            return year;
        }

        private String getTermin() {
            return termin;
        }
    }
}

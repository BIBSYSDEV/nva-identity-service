package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PublicationInstanceTypes {
    ACADEMIC_ARTICLE("Vitenskapelig artikkel"),
    ACADEMIC_CHAPTER("Vitenskapelig kapittel"),
    ACADEMIC_LITERATURE_REVIEW("Vitenskapelig oversiktsartikkel"),
    ACADEMIC_MONOGRAPH("Vitenskapelig monografi"),
    ANTHOLOGY("Del av bok eller rapport"),
    ARCHITECTURE("Arkitektur"),
    ARTISTIC("Kunstnerisk resultat"),
    ARTISTIC_DESIGN("Design"),
    BOOK("Bokpublikasjon"),
    BOOK_ANTHOLOGY("Antologi"),
    CASE_REPORT("Kasuistikk"),
    CHAPTER_CONFERENCE_ABSTRACT("Konferanseabstrakt"),
    CHAPTER_IN_REPORT("Kapittel i rapport"),
    CONFERENCE_ABSTRACT("Konferanseabstrakt"),
    CONFERENCE_LECTURE("Konferanseforedrag"),
    CONFERENCE_POSTER("Konferanseposter"),
    CONFERENCE_REPORT("Konferanserapport"),
    DATA_MANAGEMENT_PLAN("Datahåndteringsplan (DMP)"),
    DATA_SET("Datasett"),
    DEGREE("Studentoppgave eller avhandling"),
    DEGREE_BACHELOR("Bacheloroppgave"),
    DEGREE_LICENTIATE("Lisensiatavhandling"),
    DEGREE_MASTER("Masteroppgave"),
    DEGREE_PHD("Doktoravhandling"),
    ENCYCLOPEDIA("Oppslagsverk"),
    ENCYCLOPEDIA_CHAPTER("Kapittel i oppslagsverk"),
    EVENT("Presentasjon"),
    EXHIBITION_CATALOG("Utstillingskatalog"),
    EXHIBITION_CATALOG_CHAPTER("Kapittel i utstillingskatalog"),
    EXHIBITION_CONTENT("Utstilling"),
    EXHIBITION_PRODUCTION("Utstillingsproduksjon"),
    GEOGRAPHICAL_CONTENT("Annen publikasjon"),
    INTRODUCTION("Innledning i antologi"),
    JOURNAL("Tidsskriftspublikasjon"),
    JOURNAL_CORRIGENDUM("Korrigendum"),
    JOURNAL_ISSUE("Hefte i tidsskrift"),
    JOURNAL_LEADER("Leder"),
    JOURNAL_LETTER("Kommentar"),
    JOURNAL_REVIEW("Bokanmeldelse"),
    LECTURE("Foredrag"),
    LITERARY_ARTS("Skrivekunst"),
    MAP("Kart"),
    MEDIA_BLOG_POST("Bloggpost"),
    MEDIA_CONTRIBUTION("Mediebidrag"),
    MEDIA_FEATURE_ARTICLE("Kronikk"),
    MEDIA_INTERVIEW("Intervju"),
    MEDIA_PARTICIPATION_IN_RADIO_OR_TV("Deltagelse i radio eller TV"),
    MEDIA_PODCAST("Podkast"),
    MEDIA_READER_OPINION("Leserinnlegg"),
    MOVING_PICTURE("Film"),
    MUSIC_PERFORMANCE("Musikk"),
    NON_FICTION_CHAPTER("Faglig kapittel"),
    NON_FICTION_MONOGRAPH("Faglig monografi"),
    OTHER_PRESENTATION("Annen presentasjon"),
    OTHER_STUDENT_WORK("Annen studentoppgave"),
    PERFORMING_ARTS("Scenekunst"),
    POPULAR_SCIENCE_ARTICLE("Populærvitenskapelig artikkel"),
    POPULAR_SCIENCE_CHAPTER("Populærvitenskapelig kapittel"),
    POPULAR_SCIENCE_MONOGRAPH("Populærvitenskapelig monografi"),
    PROFESSIONAL_ARTICLE("Fagartikkel"),
    REPORT("Rapport"),
    REPORT_BASIC("Annen rapport"),
    REPORT_BOOK_OF_ABSTRACT("Abstraktsamling"),
    REPORT_POLICY("Policyrapport"),
    REPORT_RESEARCH("Forskningsrapport"),
    REPORT_WORKING_PAPER("Arbeidsnotat"),
    RESEARCH_DATA("Forskningsdata"),
    STUDY_PROTOCOL("Studieprotokoll"),
    TEXTBOOK("Lærebok"),
    TEXTBOOK_CHAPTER("Kapittel i lærebok"),
    VISUAL_ARTS("Visuell kunst");

    @JsonValue
    private final String displayValue;

    PublicationInstanceTypes(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}

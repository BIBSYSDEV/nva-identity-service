package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PublicationInstanceTypes {
    ACADEMIC_ARTICLE("AcademicArticle"),
    ACADEMIC_CHAPTER("AcademicChapter"),
    ACADEMIC_LITERATURE_REVIEW("AcademicLiteratureReview"),
    ACADEMIC_MONOGRAPH("AcademicMonograph"),
    ANTHOLOGY("Anthology"),
    ARCHITECTURE("Architecture"),
    ARTISTIC("Artistic"),
    ARTISTIC_DESIGN("ArtisticDesign"),
    BOOK("Book"),
    BOOK_ANTHOLOGY("BookAnthology"),
    CASE_REPORT("CaseReport"),
    CHAPTER_CONFERENCE_ABSTRACT("ChapterConferenceAbstract"),
    CHAPTER_IN_REPORT("ChapterInReport"),
    CONFERENCE_ABSTRACT("ConferenceAbstract"),
    CONFERENCE_LECTURE("ConferenceLecture"),
    CONFERENCE_POSTER("ConferencePoster"),
    CONFERENCE_REPORT("ConferenceReport"),
    DATA_MANAGEMENT_PLAN("DataManagementPlan"),
    DATA_SET("DataSet"),
    DEGREE("Degree"),
    DEGREE_BACHELOR("DegreeBachelor"),
    DEGREE_LICENTIATE("DegreeLicentiate"),
    DEGREE_MASTER("DegreeMaster"),
    DEGREE_PHD("DegreePhd"),
    ENCYCLOPEDIA("Encyclopedia"),
    ENCYCLOPEDIA_CHAPTER("EncyclopediaChapter"),
    EVENT("Event"),
    EXHIBITION_CATALOG("ExhibitionCatalog"),
    EXHIBITION_CATALOG_CHAPTER("ExhibitionCatalogChapter"),
    EXHIBITION_CONTENT("ExhibitionContent"),
    EXHIBITION_PRODUCTION("ExhibitionProduction"),
    GEOGRAPHICAL_CONTENT("GeographicalContent"),
    INTRODUCTION("Introduction"),
    JOURNAL("Journal"),
    JOURNAL_CORRIGENDUM("JournalCorrigendum"),
    JOURNAL_ISSUE("JournalIssue"),
    JOURNAL_LEADER("JournalLeader"),
    JOURNAL_LETTER("JournalLetter"),
    JOURNAL_REVIEW("JournalReview"),
    LECTURE("Lecture"),
    LITERARY_ARTS("LiteraryArts"),
    MAP("Map"),
    MEDIA_BLOG_POST("MediaBlogPost"),
    MEDIA_CONTRIBUTION("MediaContribution"),
    MEDIA_FEATURE_ARTICLE("MediaFeatureArticle"),
    MEDIA_INTERVIEW("MediaInterview"),
    MEDIA_PARTICIPATION_IN_RADIO_OR_TV("MediaParticipationInRadioOrTv"),
    MEDIA_PODCAST("MediaPodcast"),
    MEDIA_READER_OPINION("MediaReaderOpinion"),
    MOVING_PICTURE("MovingPicture"),
    MUSIC_PERFORMANCE("MusicPerformance"),
    NON_FICTION_CHAPTER("NonFictionChapter"),
    NON_FICTION_MONOGRAPH("NonFictionMonograph"),
    OTHER_PRESENTATION("OtherPresentation"),
    OTHER_STUDENT_WORK("OtherStudentWork"),
    PERFORMING_ARTS("PerformingArts"),
    POPULAR_SCIENCE_ARTICLE("PopularScienceArticle"),
    POPULAR_SCIENCE_CHAPTER("PopularScienceChapter"),
    POPULAR_SCIENCE_MONOGRAPH("PopularScienceMonograph"),
    PROFESSIONAL_ARTICLE("ProfessionalArticle"),
    REPORT("Report"),
    REPORT_BASIC("ReportBasic"),
    REPORT_BOOK_OF_ABSTRACT("ReportBookOfAbstract"),
    REPORT_POLICY("ReportPolicy"),
    REPORT_RESEARCH("ReportResearch"),
    REPORT_WORKING_PAPER("ReportWorkingPaper"),
    RESEARCH_DATA("ResearchData"),
    STUDY_PROTOCOL("StudyProtocol"),
    TEXTBOOK("Textbook"),
    TEXTBOOK_CHAPTER("TextbookChapter"),
    VISUAL_ARTS("VisualArts");

    @JsonValue
    private final String displayValue;

    PublicationInstanceTypes(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}

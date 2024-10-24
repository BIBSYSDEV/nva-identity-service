package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonValue;

/*
    NOTE: Changes here must be aligned with frontend and data model/publication api.
 */
public enum PublicationInstanceTypes {
    ARCHITECTURE("Architecture"),
    ARTISTIC_DESIGN("ArtisticDesign"),
    MOVING_PICTURE("MovingPicture"),
    PERFORMING_ARTS("PerformingArts"),
    ACADEMIC_ARTICLE("AcademicArticle"),
    ACADEMIC_LITERATURE_REVIEW("AcademicLiteratureReview"),
    CASE_REPORT("CaseReport"),
    STUDY_PROTOCOL("StudyProtocol"),
    PROFESSIONAL_ARTICLE("ProfessionalArticle"),
    POPULAR_SCIENCE_ARTICLE("PopularScienceArticle"),
    JOURNAL_CORRIGENDUM("JournalCorrigendum"),
    JOURNAL_LETTER("JournalLetter"),
    JOURNAL_LEADER("JournalLeader"),
    JOURNAL_REVIEW("JournalReview"),
    ACADEMIC_MONOGRAPH("AcademicMonograph"),
    POPULAR_SCIENCE_MONOGRAPH("PopularScienceMonograph"),
    ENCYCLOPEDIA("Encyclopedia"),
    EXHIBITION_CATALOG("ExhibitionCatalog"),
    NON_FICTION_MONOGRAPH("NonFictionMonograph"),
    TEXTBOOK("Textbook"),
    BOOK_ANTHOLOGY("BookAnthology"),
    DEGREE_BACHELOR("DegreeBachelor"),
    DEGREE_MASTER("DegreeMaster"),
    DEGREE_PHD("DegreePhd"),
    DEGREE_LICENTIATE("DegreeLicentiate"),
    REPORT_BASIC("ReportBasic"),
    REPOST_POLICY("ReportPolicy"),
    REPORT_RESEARCH("ReportResearch"),
    REPORT_WORKING_PAPER("ReportWorkingPaper"),
    CONFERENCE_REPORT("ConferenceReport"),
    REPORT_BOOK_OF_ABSTRACT("ReportBookOfAbstract"),
    ACADEMIC_CHAPTER("AcademicChapter"),
    ENCYCLOPEDIA_CHAPTER("EncyclopediaChapter"),
    EXHIBITION_CATALOG_CHAPTER("ExhibitionCatalogChapter"),
    INTRODUCTION("Introduction"),
    NON_FICTION_CHAPTER("NonFictionChapter"),
    POPULAR_SCIENCE_CHAPTER("PopularScienceChapter"),
    TEXTBOOK_CHAPTER("TextbookChapter"),
    CHAPTER_CONFERENCE_ABSTRACT("ChapterConferenceAbstract"),
    CHAPTER_IN_REPORT("ChapterInReport"),
    OTHER_STUDENT_WORK("OtherStudentWork"),
    CONFERENCE_LECTURE("ConferenceLecture"),
    CONFERENCE_POSTER("ConferencePoster"),
    LECTURE("Lecture"),
    OTHER_PRESENTATION("OtherPresentation"),
    JOURNAL_ISSUE("JournalIssue"),
    CONFERENCE_ABSTRACT("ConferenceAbstract"),
    MEDIA_FEATURE_ARTICLE("MediaFeatureArticle"),
    MEDIA_BLOG_POST("MediaBlogPost"),
    MEDIA_INTERVIEW("MediaInterview"),
    MEDIA_PARTICIPATION_IN_RADIO_OR_TV("MediaParticipationInRadioOrTv"),
    MEDIA_PODCAST("MediaPodcast"),
    MEDIA_READER_OPINION("MediaReaderOpinion"),
    MUSIC_PERFORMANCE("MusicPerformance"),
    DATA_MANAGEMENT_PLAN("DataManagementPlan"),
    DATA_SET("DataSet"),
    VISUAL_ARTS("VisualArts"),
    MAP("Map"),
    LITERARY_ARTS("LiteraryArts"),
    EXHIBITION_PRODUCTION("ExhibitionProduction"),
    ACADEMIC_COMMENTARY("AcademicCommentary");

    @JsonValue
    private final String displayValue;

    PublicationInstanceTypes(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}

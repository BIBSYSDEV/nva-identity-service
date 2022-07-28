package no.unit.nva.cognito.fs;


public class FsApiClientT {

//    private static final String EXAMPLE_FODSELSNUMMER = "04127034678";
//    private static final String EXAMPLE_URI = "http://localhost:3000";
//    private static final String PERSON_PATH = "personer";
//    private static final String LIMIT_IDENTIFIER = "limit";
//    private static final String FIELDS_IDENTIFIER = "*";
//    private static final String DB_IDENTIFIER = "dbId";
//    private static final String FODSELSDATO_IDENTIFIER = "fodselsdato0=";
//    private static final String PERSONNUMMER_IDENTIFIER = "personnummer0=";
//    private static final String SUCCESS_AUTHORIZATION_HEADER_VALUE = "Basic ZHVtbXlVc2VybmFtZTpkdW1teVBhc3N3b3Jk";
//    private static final String APPLICATION_JSON_CONTENT_TYPE_VALUE = "application/json";
//
//
//    private InputStream createRequest(String nin)
//            throws com.fasterxml.jackson.core.JsonProcessingException {
//        final String fodselsdato = nin.substring(0,5);
//        final String personnummer = nin.substring(6,-1);
//        final URI topLevelCristinOrgId =
//                UriWrapper.fromUri(EXAMPLE_URI).addChild(PERSON_PATH)
//                .addQueryParameter(DB_IDENTIFIER, "true")
//                .addQueryParameter(LIMIT_IDENTIFIER, Integer.toString(0))
//                .addQueryParameter(FIELDS_IDENTIFIER, "*")
//                .addQueryParameter(FODSELSDATO_IDENTIFIER, fodselsdato)
//                .addQueryParameter(PERSONNUMMER_IDENTIFIER, personnummer)
//                .getUri();
//        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
//                .withTopLevelCristinOrgId(topLevelCristinOrgId)
//                .build();
//    }
//
//    private void stubRequestForPerson(final int year, final String responseBody) {
//        stubFor(get(urlPathEqualTo(PERSON_PATH))
//                .withHeader(AUTHORIZATION, equalTo(SUCCESS_AUTHORIZATION_HEADER_VALUE))
//                .withQueryParam(DB_IDENTIFIER, equalTo("true"))
//                .withQueryParam(LIMIT_IDENTIFIER, equalTo("0"))
//                .withQueryParam(FIELDS_IDENTIFIER, equalTo("*"))
//                .withQueryParam(FODSELSDATO_IDENTIFIER, equalTo("fodselsdato0="))
//                .withQueryParam(PERSONNUMMER_IDENTIFIER, equalTo("personnummer0="))
//                .willReturn(WireMock.ok()
//                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_CONTENT_TYPE_VALUE)
//                        .withBody(responseBody)));
//    }

    public static void main(String[] args) throws Exception {
        String username = args[0];
        String password = args[1];
        FsApi apiClient = new FsApi("https://api.fellesstudentsystem.no", username, password);
        var lopenummer = apiClient.getLopenummerToPersonFromFs("19047747298").getItems().get(0).getId().getPersonlopenummer();
        var courses = apiClient.getCoursesToPersonFromFs("19047747298");

   }


}

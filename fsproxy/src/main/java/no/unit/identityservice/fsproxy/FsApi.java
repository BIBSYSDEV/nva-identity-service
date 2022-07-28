package no.unit.identityservice.fsproxy;

import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

public class FsApi {

    public static final String PERSON_PATH = "personer";
    public static final String FS_HOST = "api.fellesstudentsystem.no";
    private final String username = new Environment().readEnv("FS_USERNAME");
    private final String password = new Environment().readEnv("FS_PASSWORD");;

    public FsIdNumber getFsId(NationalIdentityNumber nationalIdentityNumber) throws IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder().build();
        var httpRequest = HttpRequest.newBuilder(createSearchUri(nationalIdentityNumber))
                .header("Authorization", getBasicAuthenticationHeader())
                .GET().build();

        var response=httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        var responseBody = response.body();
        var fsIdSearchResult= JsonUtils.dtoObjectMapper.readValue(responseBody,FsPersonSearchResponse.class);

        return fsIdSearchResult.getSearchResults().get(0).getFsPerson().getFsIdNumber();
    }

    private URI createSearchUri(NationalIdentityNumber nin) {
        return UriWrapper.fromHost(FS_HOST)
                .addChild(PERSON_PATH)
                .addQueryParameter("dbId","true")
                .addQueryParameter("limit","0")
                .addQueryParameter("fodselsdato0", nin.getBirthDate())
                .addQueryParameter("personnummer0",nin.getPersonalNumber())
                .getUri();

    }

    private String getBasicAuthenticationHeader() {
        final String valueToEncode = this.username + ":" + this.password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }


}

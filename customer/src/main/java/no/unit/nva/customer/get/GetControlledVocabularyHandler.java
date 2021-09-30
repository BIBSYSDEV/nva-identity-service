package no.unit.nva.customer.get;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Set;
import no.unit.nva.customer.model.VocabularySettingDto;
import no.unit.nva.customer.model.VocabularyStatus;
import no.unit.nva.customer.model.interfaces.VocabularySettingsList;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;

public class GetControlledVocabularyHandler extends ApiGatewayHandler<Void, VocabularySettingsList> {

    public static final VocabularySettingDto HARDCODED_RESPONSE = new VocabularySettingDto(
        "sampleVocabulary",
        URI.create("https://www.example.com"),
        VocabularyStatus.ALLOWED);

    public GetControlledVocabularyHandler() {
        super(Void.class);
    }

    @Override
    protected VocabularySettingsList processInput(Void input, RequestInfo requestInfo, Context context) {
        return new VocabularySettingsList(Set.of(HARDCODED_RESPONSE));
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, VocabularySettingsList output) {
        return HttpURLConnection.HTTP_OK;
    }
}

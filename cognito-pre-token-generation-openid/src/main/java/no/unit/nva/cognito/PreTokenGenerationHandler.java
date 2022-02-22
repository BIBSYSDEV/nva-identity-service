package no.unit.nva.cognito;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.jr.ob.JSON;
import java.io.IOException;
import java.util.Map;
import nva.commons.core.JacocoGenerated;

public class PreTokenGenerationHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    @JacocoGenerated
    public PreTokenGenerationHandler() {

    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try{
            var json = JSON.std.asString(input);
            context.getLogger().log(json);
            return input;
        }
        catch (IOException e){
            throw new RuntimeException(e);
        }

    }
}

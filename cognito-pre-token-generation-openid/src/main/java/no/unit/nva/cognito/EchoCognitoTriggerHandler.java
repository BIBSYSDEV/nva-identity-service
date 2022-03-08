package no.unit.nva.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Optional;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;

@JacocoGenerated
public class EchoCognitoTriggerHandler implements RequestStreamHandler {

    @JacocoGenerated
    public EchoCognitoTriggerHandler() {

    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        String inputString = Optional.ofNullable(input)
            .map(IoUtils::streamToString)
            .orElse("");
        context.getLogger().log(inputString);
        writeOutput(output,inputString);
    }

    private void writeOutput(OutputStream output, String outputString) {
        try (OutputStreamWriter writer = new OutputStreamWriter(output)) {
            writer.write(outputString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

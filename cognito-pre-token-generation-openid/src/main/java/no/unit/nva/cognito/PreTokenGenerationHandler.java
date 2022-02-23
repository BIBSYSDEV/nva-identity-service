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

public class PreTokenGenerationHandler implements RequestStreamHandler {

    @JacocoGenerated
    public PreTokenGenerationHandler() {

    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        String inputString = Optional.ofNullable(input)
            .map(IoUtils::streamToString)
            .orElse("Input was empty");
        context.getLogger().log(inputString);
        writeOutput(output);
    }

    private void writeOutput(OutputStream output) {
        try (OutputStreamWriter writer = new OutputStreamWriter(output)) {
            writer.write("Not important output");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package no.unit.nva.cognito;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Every permission that lets Cognito invoke a trigger names the user pool it is for. A new trigger
 * added without it would be caught here.
 */
class TriggerPermissionsTest {

  /** Gradle runs the tests from the module directory; the template is one level up. */
  private static final Path TEMPLATE = Path.of("..", "template.yaml");

  private static final String START_OF_RESOURCE = "(?m)^(?= {2}\\w+:$)";
  private static final String COGNITO = "Principal: cognito-idp.amazonaws.com";

  @Test
  void shouldNameTheUserPoolInEveryCognitoTriggerPermission() throws IOException {
    var permissions = cognitoTriggerPermissions();

    assertThat(permissions).hasSize(3);
    assertThat(permissions)
        .allSatisfy(
            permission -> assertThat(permission).containsPattern("SourceArn: !GetAtt \\w+\\.Arn"));
  }

  private static List<String> cognitoTriggerPermissions() throws IOException {
    return Arrays.stream(Files.readString(TEMPLATE).split(START_OF_RESOURCE))
        .filter(resource -> resource.contains("Type: AWS::Lambda::Permission"))
        .filter(resource -> resource.contains(COGNITO))
        .toList();
  }
}

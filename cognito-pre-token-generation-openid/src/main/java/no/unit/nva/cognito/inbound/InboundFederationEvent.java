package no.unit.nva.cognito.inbound;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Event for the Cognito inbound federation trigger ({@code InboundFederation_ExternalProvider}).
 * aws-lambda-java-events has no class for this trigger, so this one mirrors the documented JSON.
 * The Lambda runtime deserializes it with getters and setters and ignores unknown fields.
 *
 * @see <a
 *     href="https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-lambda-inbound-federation.html">AWS
 *     documentation</a>
 */
public class InboundFederationEvent {

  private String version;
  private String triggerSource;
  private String region;
  private String userPoolId;
  private String userName;
  private CallerContext callerContext;
  private Request request = new Request();
  private Response response = new Response();

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getTriggerSource() {
    return triggerSource;
  }

  public void setTriggerSource(String triggerSource) {
    this.triggerSource = triggerSource;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getUserPoolId() {
    return userPoolId;
  }

  public void setUserPoolId(String userPoolId) {
    this.userPoolId = userPoolId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public CallerContext getCallerContext() {
    return callerContext;
  }

  public void setCallerContext(CallerContext callerContext) {
    this.callerContext = callerContext;
  }

  public Request getRequest() {
    return request;
  }

  /** A JSON null keeps the empty default, so the handler never has to null check. */
  public void setRequest(Request request) {
    this.request = Objects.requireNonNullElseGet(request, Request::new);
  }

  public Response getResponse() {
    return response;
  }

  public void setResponse(Response response) {
    this.response = Objects.requireNonNullElseGet(response, Response::new);
  }

  public static class CallerContext {

    private String awsSdkVersion;
    private String clientId;

    public String getAwsSdkVersion() {
      return awsSdkVersion;
    }

    public void setAwsSdkVersion(String awsSdkVersion) {
      this.awsSdkVersion = awsSdkVersion;
    }

    public String getClientId() {
      return clientId;
    }

    public void setClientId(String clientId) {
      this.clientId = clientId;
    }
  }

  public static class Request {

    private String providerName;
    private String providerType;
    private Attributes attributes = new Attributes();

    public String getProviderName() {
      return providerName;
    }

    public void setProviderName(String providerName) {
      this.providerName = providerName;
    }

    public String getProviderType() {
      return providerType;
    }

    public void setProviderType(String providerType) {
      this.providerType = providerType;
    }

    public Attributes getAttributes() {
      return attributes;
    }

    public void setAttributes(Attributes attributes) {
      this.attributes = Objects.requireNonNullElseGet(attributes, Attributes::new);
    }
  }

  /**
   * Raw attributes from the identity provider. Values are kept as {@link Object} because claims
   * such as {@code exp} are numbers and some providers return nested structures.
   */
  public static class Attributes {

    private Map<String, Object> tokenResponse = new HashMap<>();
    private Map<String, Object> idToken = new HashMap<>();
    private Map<String, Object> userInfo = new HashMap<>();
    private Map<String, Object> samlResponse = new HashMap<>();

    public Map<String, Object> getTokenResponse() {
      return tokenResponse;
    }

    public void setTokenResponse(Map<String, Object> tokenResponse) {
      this.tokenResponse = Objects.requireNonNullElseGet(tokenResponse, HashMap::new);
    }

    public Map<String, Object> getIdToken() {
      return idToken;
    }

    public void setIdToken(Map<String, Object> idToken) {
      this.idToken = Objects.requireNonNullElseGet(idToken, HashMap::new);
    }

    public Map<String, Object> getUserInfo() {
      return userInfo;
    }

    public void setUserInfo(Map<String, Object> userInfo) {
      this.userInfo = Objects.requireNonNullElseGet(userInfo, HashMap::new);
    }

    public Map<String, Object> getSamlResponse() {
      return samlResponse;
    }

    public void setSamlResponse(Map<String, Object> samlResponse) {
      this.samlResponse = Objects.requireNonNullElseGet(samlResponse, HashMap::new);
    }
  }

  public static class Response {

    private Map<String, String> userAttributesToMap = new HashMap<>();

    public Map<String, String> getUserAttributesToMap() {
      return userAttributesToMap;
    }

    public void setUserAttributesToMap(Map<String, String> userAttributesToMap) {
      this.userAttributesToMap = Objects.requireNonNullElseGet(userAttributesToMap, HashMap::new);
    }
  }
}

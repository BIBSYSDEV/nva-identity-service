package no.unit.nva.customer.model.interfaces;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeName;

@SuppressWarnings("PMD.ExcessivePublicCount")
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = As.PROPERTY,
    property = "type")
@JsonTypeName("Customer")
public interface WithDataFields {

    String getName();

    void setName(String name);

    String getDisplayName();

    void setDisplayName(String displayName);

    String getShortName();

    void setShortName(String shortName);

    String getArchiveName();

    void setArchiveName(String archiveName);

    String getCname();

    void setCname(String cname);

    String getInstitutionDns();

    void setInstitutionDns(String institutionDns);

    String getFeideOrganizationId();

    void setFeideOrganizationId(String feideOrganizationId);

    String getCristinId();

    void setCristinId(String cristinId);

}

package no.unit.nva.customer.model.interfaces;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;


public interface Customer {

    UUID getIdentifier();

    void setIdentifier(UUID identifier);

    Instant getCreatedDate();

    void setCreatedDate(Instant createdDate);

    Instant getModifiedDate();

    void setModifiedDate(Instant modifiedDate);

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

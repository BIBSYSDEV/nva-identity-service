package no.unit.nva.useraccessmanagement.interfaces;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

public interface WithType {

    String TYPE_FIELD = "type";

    @DynamoDbAttribute(TYPE_FIELD)
    String getType();

    void setType(String ignored);

}

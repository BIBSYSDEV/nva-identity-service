package no.unit.nva.cognito;

import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public final class CognitoClaims {

    public static final String CURRENT_CUSTOMER_CLAIM = "custom:customerId";
    public static final String ALLOWED_CUSTOMERS_CLAIM = "custom:allowedCustomers";
    public static final String PERSON_ID_CLAIM = "custom:cristinId";
    public static final String NVA_USERNAME_CLAIM = "custom:nvaUsername";
    public static final String TOP_ORG_CRISTIN_ID = "custom:topOrgCristinId";
    public static final String PERSON_CRISTIN_ID_CLAIM = "custom:cristinId";
    public static final String EMPTY_CLAIM = "null";
    public static final String ROLES_CLAIM = "custom:roles";
    public static final String ACCESS_RIGHTS_CLAIM = "custom:accessRights";
    public static final String PERSON_AFFILIATION_CLAIM = "custom:personAffiliation";
    public static final String LAST_NAME_CLAIM = "custom:lastName";
    public static final String FIRST_NAME_CLAIM = "custom:firstName";
    public static final String IMPERSONATING_CLAIM = "custom:impersonating";
    public static final String IMPERSONATED_BY_CLAIM = "custom:impersonatedBy";
    public static final String CURRENT_TERMS = "custom:currentTerms";
    public static final String CUSTOMER_ACCEPTED_TERMS = "custom:acceptedTerms";
    public static final String NIN_FOR_FEIDE_USERS = "custom:feideIdNin";
    public static final String NIN_FOR_NON_FEIDE_USERS = "custom:nin";


    static final String[] CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC = {
        NIN_FOR_NON_FEIDE_USERS,
        NIN_FOR_FEIDE_USERS, IMPERSONATING_CLAIM};
    public static final String AT = "@";
    public static final String ELEMENTS_DELIMITER = ",";

    private CognitoClaims() {

    }
}

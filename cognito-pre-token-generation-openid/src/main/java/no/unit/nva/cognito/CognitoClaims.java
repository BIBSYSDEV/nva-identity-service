package no.unit.nva.cognito;

import static no.unit.nva.cognito.UserSelectionUponLoginHandler.NIN_FON_NON_FEIDE_USERS;
import static no.unit.nva.cognito.UserSelectionUponLoginHandler.NIN_FOR_FEIDE_USERS;

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

    public static final String[] CLAIMS_TO_BE_SUPPRESSED_FROM_PUBLIC = {NIN_FON_NON_FEIDE_USERS,
        NIN_FOR_FEIDE_USERS};
    public static final String AT = "@";
    public static final String ELEMENTS_DELIMITER = ",";

    private CognitoClaims() {

    }
}

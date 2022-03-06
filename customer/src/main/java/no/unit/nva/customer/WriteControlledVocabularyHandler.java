package no.unit.nva.customer;

import static no.unit.nva.identityservice.json.JsonConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigatewayv2.exceptions.BadRequestException;

public abstract class WriteControlledVocabularyHandler
    extends ControlledVocabularyHandler<VocabularyList, VocabularyList> {

    public WriteControlledVocabularyHandler(CustomerService customerService) {
        super(customerService);
    }

    protected abstract CustomerDto updateVocabularySettings(VocabularyList input, CustomerDto customer);

    @Override
    protected final VocabularyList processInput(String input,
                                                APIGatewayProxyRequestEvent requestInfo,
                                                Context context) {
        UUID identifier = extractIdentifier(requestInfo);
        CustomerDto customer = customerService.getCustomer(identifier);
        var inputObject = parseInput(input);
        customer = updateVocabularySettings(inputObject, customer);
        CustomerDto updatedCustomer = customerService.updateCustomer(identifier, customer);
        return VocabularyList.fromCustomerDto(updatedCustomer);
    }

    private VocabularyList parseInput(String input) {
        return attempt(() -> objectMapper.beanFrom(VocabularyList.class, input))
            .orElseThrow(fail -> new BadRequestException("Invalid input object",fail.getException()));
    }
}

package com.hedera.node.app.hapi.simplefees.apis.token;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.node.app.hapi.simplefees.AbstractFeeModel;
import com.hedera.node.app.hapi.simplefees.AbstractFeesSchedule;
import com.hedera.node.app.hapi.simplefees.FeeResult;
import com.hedera.node.app.hapi.simplefees.ParameterDefinition;
import com.hedera.node.app.hapi.simplefees.apis.common.AssociateOrDissociate;

import java.util.List;
import java.util.Map;

public class TokenAssociateDissociate extends AbstractFeeModel {
    AssociateOrDissociate associateOrDissociate;

    private final List<ParameterDefinition> params = List.of(
            new ParameterDefinition("numTokenTypes", "number", null, 1, 1, 10, "Number of associated token-types")
    );

    public TokenAssociateDissociate(AssociateOrDissociate associateOrDissociate) {
        this.associateOrDissociate = associateOrDissociate;
    }

    @Override
    public String getService() {
        return "Token";
    }

    @Override
    public String getMethodName() {
        return "TokenAssociateDissociate";
    }

    @Override
    public String getDescription() {
        return (this.associateOrDissociate == AssociateOrDissociate.Associate) ? "Associate tokens-types to accounts" : "Dissociate tokens-types from accounts";
    }

    @Override
    protected List<ParameterDefinition> apiSpecificParams() {
        return params;
    }

    @Override
    protected FeeResult computeApiSpecificFee(Map<String, Object> values, AbstractFeesSchedule feesSchedule) {
        FeeResult fee = new FeeResult();

        long baseFee = ((AssociateOrDissociate)values.get("associateOrDissociate") == AssociateOrDissociate.Associate) ?
                feesSchedule.getServiceBaseFee(HederaFunctionality.TOKEN_ASSOCIATE_TO_ACCOUNT) : feesSchedule.getServiceBaseFee(HederaFunctionality.TOKEN_DISSOCIATE_FROM_ACCOUNT);

        fee.addDetail("Base fee", 1, baseFee);

        int numTokenTypes = (int) values.get("numTokenTypes");
        final int numFreeTokenTypes = 1;
        if (numTokenTypes > numFreeTokenTypes) {
            fee.addDetail("Additional token-types", numTokenTypes - numFreeTokenTypes, (numTokenTypes - numFreeTokenTypes) * baseFee);
        }

        return fee;
    }
}

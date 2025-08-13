package com.hedera.node.app.hapi.simplefees.apis.token;

import com.hedera.node.app.hapi.simplefees.AbstractFeeModel;
import com.hedera.node.app.hapi.simplefees.AbstractFeesSchedule;
import com.hedera.node.app.hapi.simplefees.FeeResult;
import com.hedera.node.app.hapi.simplefees.ParameterDefinition;

import java.util.List;
import java.util.Map;

// Handles tokenClaimAirdrop, tokenCancelAirdrop and tokenReject apis
public class TokenAirdropOperations extends AbstractFeeModel {
    String api;
    String desciption;
    private final List<ParameterDefinition> params = List.of(
            new ParameterDefinition("numTokenTypes", "number", null, 1, 1, 10, "Number of token-types/NFT serials")
    );

    public TokenAirdropOperations(String api, String desciption) {
        this.api = api;
        this.desciption = desciption;
    }

    @Override
    public String getService() {
        return "Token";
    }

    @Override
    public String getMethodName() {
        return this.api;
    }

    @Override
    public String getDescription() {
        return desciption;
    }

    @Override
    protected List<ParameterDefinition> apiSpecificParams() {
        return params;
    }

    @Override
    protected FeeResult computeApiSpecificFee(Map<String, Object> values, AbstractFeesSchedule feesSchedule) {
        FeeResult fee = new FeeResult();

        long baseFee = feesSchedule.getServiceBaseFee(api);
        fee.addDetail("Base fee", 1, baseFee);

        int numTokenTypes = (int) values.get("numTokenTypes");
        final int numFreeTokenTypes = 1;
        if (numTokenTypes > numFreeTokenTypes) {
            fee.addDetail("Additional token-types", numTokenTypes - numFreeTokenTypes, (numTokenTypes - numFreeTokenTypes) * baseFee);
        }

        return fee;
    }
}

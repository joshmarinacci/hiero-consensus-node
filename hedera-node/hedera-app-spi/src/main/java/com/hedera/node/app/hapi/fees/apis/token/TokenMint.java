package com.hedera.node.app.hapi.fees.apis.token;

import com.hedera.node.app.hapi.fees.AbstractFeeModel;
import com.hedera.node.app.hapi.fees.AbstractFeesSchedule;
import com.hedera.node.app.hapi.fees.FeeResult;
import com.hedera.node.app.hapi.fees.ParameterDefinition;
import com.hedera.node.app.hapi.fees.apis.common.FTOrNFT;
import com.hedera.node.app.hapi.fees.apis.common.FeeConstants.Extras;

import java.util.List;
import java.util.Map;

public class TokenMint extends AbstractFeeModel {
    private final List<ParameterDefinition> params = List.of(
            new ParameterDefinition("fungibleOrNonFungible", "list", new Object[] {FTOrNFT.Fungible, FTOrNFT.NonFungible}, FTOrNFT.Fungible, 0, 0, "Fungible or Non-fungible token"),
            new ParameterDefinition("numTokens", "number", null, 1, 1, 10, "Number of tokens minted")
    );

    @Override
    public String getService() {
        return "Token";
    }

    @Override
    public String getMethodName() {
        return "TokenMint";
    }

    @Override
    public String getDescription() {
        return "Mint tokens";
    }

    @Override
    protected List<ParameterDefinition> apiSpecificParams() {
        return params;
    }

    @Override
    protected FeeResult computeApiSpecificFee(Map<String, Object> values, AbstractFeesSchedule feesSchedule) {
        FeeResult fee = new FeeResult();
        fee.addDetail("Base fee for TokenMint", 1, feesSchedule.getServiceBaseFee("TokenMint"));
        long numTokens = (long) values.get(Extras.StandardFungibleTokens.name());
        var numFreeTokens = feesSchedule.getServiceExtraIncludedCount("TokenMint", Extras.StandardFungibleTokens.name());
        if (numTokens > numFreeTokens) {
            fee.addDetail("Additional FTs", numTokens - numFreeTokens, (numTokens - numFreeTokens) * feesSchedule.getExtrasFee(Extras.StandardFungibleTokens.name()));
        }

        return fee;
    }
}

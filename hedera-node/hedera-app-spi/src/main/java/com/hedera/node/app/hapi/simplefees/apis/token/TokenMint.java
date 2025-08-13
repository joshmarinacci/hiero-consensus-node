package com.hedera.node.app.hapi.simplefees.apis.token;

import com.hedera.node.app.hapi.simplefees.AbstractFeeModel;
import com.hedera.node.app.hapi.simplefees.AbstractFeesSchedule;
import com.hedera.node.app.hapi.simplefees.FeeResult;
import com.hedera.node.app.hapi.simplefees.ParameterDefinition;
import com.hedera.node.app.hapi.simplefees.apis.common.FTOrNFT;
import org.hiero.hapi.support.fees.Extra;

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
        long numTokens = (long) values.get(Extra.STANDARD_FUNGIBLE_TOKENS);
        var numFreeTokens = feesSchedule.getServiceExtraIncludedCount("TokenMint", Extra.STANDARD_FUNGIBLE_TOKENS);
        if (numTokens > numFreeTokens) {
            fee.addDetail("Additional FTs", numTokens - numFreeTokens, (numTokens - numFreeTokens) * feesSchedule.getExtrasFee(Extra.STANDARD_FUNGIBLE_TOKENS));
        }

        return fee;
    }
}

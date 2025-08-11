package com.hedera.node.app.hapi.fees.apis.token;

import com.hedera.node.app.hapi.fees.AbstractFeeModel;
import com.hedera.node.app.hapi.fees.AbstractFeesSchedule;
import com.hedera.node.app.hapi.fees.FeeResult;
import com.hedera.node.app.hapi.fees.ParameterDefinition;
import com.hedera.node.app.hapi.fees.apis.common.FTOrNFT;

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

        final FTOrNFT fungibleOrNonFungible = (FTOrNFT) values.get("fungibleOrNonFungible");

        long baseFeeForMint = fungibleOrNonFungible == FTOrNFT.Fungible ? feesSchedule.getServiceBaseFee("TokenMintFungible"): feesSchedule.getServiceBaseFee("TokenMintNonFungible");
        fee.addDetail("Base fee", 1, baseFeeForMint);

        long numTokens = (long) values.get("numTokens");
        final long numFreeTokens = 1;
        if (fungibleOrNonFungible == FTOrNFT.NonFungible && numTokens > numFreeTokens) {
            fee.addDetail("Additional NFTs", numTokens - numFreeTokens, (numTokens - numFreeTokens) * baseFeeForMint);
        }

        return fee;
    }
}

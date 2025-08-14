package com.hedera.node.app.hapi.simplefees.apis.token;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.node.app.hapi.simplefees.AbstractFeeModel;
import com.hedera.node.app.hapi.simplefees.AbstractFeesSchedule;
import com.hedera.node.app.hapi.simplefees.FeeResult;
import com.hedera.node.app.hapi.simplefees.ParameterDefinition;
import com.hedera.node.app.hapi.simplefees.apis.common.FTOrNFT;

import java.util.List;
import java.util.Map;

import static com.hedera.node.app.hapi.simplefees.apis.common.FTOrNFT.NonFungible;

public class TokenBurn extends AbstractFeeModel {
    private final List<ParameterDefinition> params = List.of(
            new ParameterDefinition("fungibleOrNonFungible", "list", new Object[] {FTOrNFT.Fungible, NonFungible}, FTOrNFT.Fungible, 0, 0, "Fungible or Non-fungible token"),
            new ParameterDefinition("numTokens", "number", null, 1, 1, 10, "Number of NFT serials burned")
    );

    @Override
    public String getService() {
        return "Token";
    }

    @Override
    public String getMethodName() {
        return "TokenBurn";
    }

    @Override
    public String getDescription() {
        return "Burn tokens";
    }

    @Override
    protected List<ParameterDefinition> apiSpecificParams() {
        return params;
    }

    @Override
    protected FeeResult computeApiSpecificFee(Map<String, Object> values, AbstractFeesSchedule feesSchedule) {
        FeeResult fee = new FeeResult();

        fee.addDetail("Base fee", 1, feesSchedule.getServiceBaseFee(HederaFunctionality.TOKEN_BURN));

        final FTOrNFT fungibleOrNonFungible = (FTOrNFT) values.get("fungibleOrNonFungible");
        if (fungibleOrNonFungible == NonFungible) {
            int numTokens = (int) values.get("numTokens");
            final int numFreeTokens = 1;
            if (numTokens > numFreeTokens) {
                fee.addDetail("Additional NFTs", numTokens - numFreeTokens, (numTokens - numFreeTokens) * feesSchedule.getServiceBaseFee(HederaFunctionality.TOKEN_BURN));
            }
        }

        return fee;
    }
}

package com.hedera.node.app.hapi.fees.apis.crypto;

import com.hedera.node.app.hapi.fees.*;
import com.hedera.node.app.hapi.fees.apis.common.FeeConstants.Extras;

import java.util.List;
import java.util.Map;


public class CryptoTransfer extends AbstractFeeModel {
    String service;
    String api;

    private final List<ParameterDefinition> params = List.of(
            new ParameterDefinition(Extras.Accounts.name(), "number", null, 2, 0, 20, "Number of Accounts involved"),
            new ParameterDefinition(Extras.StandardFungibleTokens.name(), "number", null, 0, 0, 10, "Fungible token entries without custom fee"),
            new ParameterDefinition(Extras.StandardNonFungibleTokens.name(), "number", null, 0, 0, 10, "NFT entries without custom fee"),
            new ParameterDefinition(Extras.CustomFeeFungibleTokens.name(), "number", null, 0, 0, 10, "Fungible token entries with custom fee"),
            new ParameterDefinition(Extras.CustomFeeNonFungibleTokens.name(), "number", null, 0, 0, 10, "NFT entries with custom fee"),
            new ParameterDefinition(Extras.CreatedAutoAssociations.name(), "number", null, 0, 0, 10, "Auto-created token associations"),
            new ParameterDefinition(Extras.CreatedAccounts.name(), "number", null, 0, 0, 20, "Auto-created accounts")
    );

    public CryptoTransfer(String service, String api) {
        this.service = service;
        this.api = api;
    }

    @Override
    public String getMethodName() {
        return "CryptoTransfer";
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public String getDescription() {
        return "Transfers a combination of hbars and tokens.";
    }

    @Override
    protected List<ParameterDefinition> apiSpecificParams() {
        return params;
    }

    @Override
    public FeeCheckResult checkParameters(Map<String, Object> values) {
        FeeCheckResult base = super.checkParameters(values);
        if (!base.result) return base;

//        if ( (long) values.get(Extras.Accounts.name()) < 2 ||
//                ((long) values.get("numFTNoCustomFeeEntries") < 2 &&
//                (long) values.get("numNFTNoCustomFeeEntries") < 2 &&
//                (long) values.get("numNFTWithCustomFeeEntries") < 2)) {
//            return FeeCheckResult.failure("There must be at least 2 entries of hbar or token transfers.");
//        }
//
        return FeeCheckResult.success();
    }

    private long getLong(Object value) {
        return (value instanceof Long) ? (Long) value : 0;
    }

    /*
     This API is complex. This same code can be called for

     1. CryptoTransfer: just hbar transfers. The base fee here includes 1
        hbar transfer entry between 2 accounts
     2. TokenTransfer: fungible and/or non-fungible tokens and/or hbars.
        These tokens could have custom fee enabled. The base fee here
        includes 1 FT/NFT token transfer without custom fee
     3. TokenAirdrop: fungible and/or non-fungible tokens. These tokens
        could have custom fee enabled. The base fee here includes 1 FT/NFT
        token transfer without custom fee
     */
    protected FeeResult computeApiSpecificFee(Map<String, Object> values, AbstractFeesSchedule feesSchedule) {
        FeeResult fee = new FeeResult();

        // Extract values
        long standardFT = getLong(values.get(Extras.StandardFungibleTokens.name()));
        long standardNFT = getLong(values.get(Extras.StandardNonFungibleTokens.name()));
        long customFT = getLong(values.get(Extras.CustomFeeFungibleTokens.name()));
        long customNFT = getLong(values.get(Extras.CustomFeeNonFungibleTokens.name()));
        long customTokens = customFT + customNFT;
        boolean tokenTransfersPresent = (standardFT + standardNFT + customFT + customNFT) > 0;

        String effectiveApi = api;

        // If no token transfers are present, then treat it as CryptoTransfer (hbar transfer). Otherwise, treat it as a TokenTransfer
        if (tokenTransfersPresent) {
            if (api.equals("CryptoTransfer")) {
                effectiveApi = "TokenTransfer";
            }
        } else {
            effectiveApi = "CryptoTransfer";
        }

        long numFreeTokens = feesSchedule.getServiceExtraIncludedCount(effectiveApi, Extras.StandardFungibleTokens.name());
        // If tokens with custom fees are used, then use the higher base prices
        if (customTokens > 0) {
            if (effectiveApi.equals("TokenTransfer")) {
                fee.addDetail("Base fee for " + effectiveApi + " (with Custom fee)", 1,  feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee"));
            } else if (effectiveApi.equals("TokenAirdrop")) {
                fee.addDetail("Base fee for " + effectiveApi + " (with Custom fee)", 1,  feesSchedule.getServiceBaseFee("TokenAirdropWithCustomFee"));
            }
        } else {
            fee.addDetail("Base fee for " + effectiveApi, 1, feesSchedule.getServiceBaseFee(effectiveApi));
        }

        // Overage for the number of accounts that we need to update for handling this transaction
        long numFreeAccounts = feesSchedule.getServiceExtraIncludedCount(effectiveApi, Extras.Accounts.name());
        var numAccounts = (long)values.get(Extras.Accounts.name());
        if (numAccounts > numFreeAccounts) {
            var excess = numAccounts - numFreeAccounts;
            fee.addDetail("Accounts involved", excess, excess * feesSchedule.getExtrasFee(Extras.Accounts.name()));
        }

        // Overage for the number of token-types that we need to fetch for handling this transaction
        // Process the tokens with Custom Fee first since we have already increased the base price to accommodate the presence of custom-fee tokens, and the included free token should count against the token with custom fee
        if (customFT > 0) {
            if ((customFT - numFreeTokens) > 0) {
                fee.addDetail("FT with custom fee", (customFT - numFreeTokens), (customFT - numFreeTokens) * feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee"));
            }
            numFreeTokens = 0;
        }
        if (customNFT > 0) {
            if ((customNFT - numFreeTokens) > 0) {
                fee.addDetail("NFT with custom fee", (customNFT - numFreeTokens), (customNFT - numFreeTokens) * feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee"));
            }
            numFreeTokens = 0;
        }
        if (standardFT > 0) {
            if ((standardFT - numFreeTokens) > 0) {
                fee.addDetail("FT no custom fee", (standardFT - numFreeTokens), (standardFT - numFreeTokens) * feesSchedule.getServiceBaseFee("TokenTransfer"));
            }
            numFreeTokens = 0;
        }
        if (standardNFT > 0) {
            if ((standardNFT - numFreeTokens) > 0) {
                fee.addDetail("NFT no custom fee", (standardNFT - numFreeTokens), (standardNFT - numFreeTokens) * feesSchedule.getServiceBaseFee("TokenTransfer"));
            }
        }

        // Overage for the number of entities created automatically (associations/accounts) during handling this transaction
        if (values.get(Extras.CreatedAutoAssociations.name()) instanceof Integer num && num > 0)
            fee.addDetail("Auto token associations", num, num * feesSchedule.getServiceBaseFee("TokenAssociateToAccount"));
        if (values.get(Extras.CreatedAccounts.name()) instanceof Integer num && num > 0)
            fee.addDetail("Auto account creations", num, num * feesSchedule.getServiceBaseFee("CryptoCreate"));

        return fee;
    }
}

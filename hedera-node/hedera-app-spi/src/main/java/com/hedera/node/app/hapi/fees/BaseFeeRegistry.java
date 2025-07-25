package com.hedera.node.app.hapi.fees;

import com.hedera.hapi.node.consensus.SimpleFeesSchedule;
import com.hedera.pbj.runtime.io.stream.ReadableStreamingData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class BaseFeeRegistry {

    private static final Map<String, Double> BASE_FEES;
    private static final AbstractSimpleFeesSchedule SIMPLE_FEES_SCHEDULE;

    static {
        Map<String, Double> fees = new HashMap<>();
        // Addons
        fees.put("PerSignature", 0.0001);
        fees.put("PerKey", 0.01);
        fees.put("PerHCSByte", 0.000_011);
        fees.put("PerFileByte", 0.000_011);
        fees.put("PerCryptoTransferAccount", 0.000_01);
        fees.put("PerGas", 0.0000000852);

        // Crypto service
        fees.put("CryptoCreate", 0.050_00);
        fees.put("CryptoUpdate", 0.000_22);
        fees.put("CryptoTransfer", 0.000_10);
        fees.put("CryptoDelete", 0.005_00);
        fees.put("CryptoGetAccountRecords", 0.000_10);
        fees.put("CryptoGetAccountBalance", 0.000_00);
        fees.put("CryptoGetInfo", 0.000_10);
        fees.put("CryptoGetStakers", 0.000_10);
        fees.put("CryptoApproveAllowance", 0.050_00);
        fees.put("CryptoDeleteAllowance", 0.050_00);

        // HCS
//        fees.put("ConsensusCreateTopic",                0.020_00);
//        fees.put("ConsensusCreateTopicWithCustomFee",   2.000_00);
//        fees.put("ConsensusUpdateTopic",                0.000_22);
//        fees.put("ConsensusDeleteTopic", 0.005_00);
//        fees.put("ConsensusSubmitMessage",              0.000_10);
//        fees.put("ConsensusSubmitMessageWithCustomFee", 0.050_00);
        fees.put("ConsensusGetTopicInfo", 0.000_20);

        // HTS
        fees.put("TokenCreate", 1.000_00);
        fees.put("TokenCreateWithCustomFee", 2.000_00);
        fees.put("TokenDelete", 0.001_00);
        fees.put("TokenUpdate", 0.001_00);
        fees.put("TokenMintFungible", 0.001_00);
        fees.put("TokenMintNonFungible", 0.020_00);
        fees.put("TokenBurn", 0.00100);
        fees.put("TokenPause", 0.00100);
        fees.put("TokenUnpause", 0.00100);
        fees.put("TokenGetInfo", 0.00010);
        fees.put("TokenGrantKycToAccount", 0.00100);
        fees.put("TokenRevokeKycFromAccount", 0.00100);
        fees.put("TokenFreezeAccount", 0.00100);
        fees.put("TokenUnfreezeAccount", 0.00100);
        fees.put("TokenAccountWipe", 0.00100);
        fees.put("TokenAssociateToAccount", 0.05000);
        fees.put("TokenDissociateFromAccount", 0.050_00);
        fees.put("TokenTransfer", 0.001_00);
        fees.put("TokenTransferWithCustomFee", 0.002_00);
        fees.put("TokenAirdrop", 0.10000);
        fees.put("TokenAirdropWithCustomFee", 0.10100);
        fees.put("TokenClaimAirdrop", 0.00100);
        fees.put("TokenCancelAirdrop", 0.00100);
        fees.put("TokenReject", 0.00100);
        fees.put("TokenFeeScheduleUpdate", 0.00100);
        fees.put("GetAccountNftInfo", 0.00010);
        fees.put("GetTokenNftInfo", 0.00010);
        fees.put("GetTokenNftInfos", 0.00010);

        // Scheduled Transactions
        fees.put("ScheduleCreate", 0.01000);
        fees.put("ScheduleSign", 0.00100);
        fees.put("ScheduleDelete", 0.00100);
        fees.put("ScheduleGetInfo", 0.00010);

        // Smart Contracts

        fees.put("ContractCreate", 1.00000);
        fees.put("ContractUpdate", 0.02600);
        fees.put("ContractDelete", 0.00700);
        fees.put("ContractCall", 0.00000);
        fees.put("ContractCallLocal", 0.00100);
        fees.put("ContractGetBytecode", 0.05000);
        fees.put("GetBySolidityID", 0.00010);
        fees.put("ContractGetInfo", 0.00010);
        fees.put("ContractGetRecords", 0.00010);
        fees.put("EthereumTransactionSuccess", 0.00000);
        fees.put("EthereumTransactionFail", 0.00010);
        // Files
        fees.put("FileCreate", 0.050_00);
        fees.put("FileUpdate", 0.050_00);
        fees.put("FileDelete", 0.007_00);
        fees.put("FileAppend", 0.050_00);
        fees.put("FileGetContents", 0.000_66);
        fees.put("FileGetInfo", 0.000_66);

        // Misc
        fees.put("GetVersionInfo", 0.00010);
        fees.put("TransactionGetReceipt", 0.00000);
        fees.put("TransactionGetRecord", 0.00010);
        fees.put("SystemDelete", 0.00000);
        fees.put("SystemUndelete", 0.00000);
        fees.put("CreateNode", 0.00100);
        fees.put("DeleteNode", 0.00100);
        fees.put("UpdateNode", 0.00100);
        fees.put("PrngTransaction", 0.00100);
        fees.put("BatchTransaction", 0.00100);

        BASE_FEES = Collections.unmodifiableMap(fees);
        SIMPLE_FEES_SCHEDULE = JsonSimpleFeesSchedule.fromJson();
    }

    private BaseFeeRegistry() {
        // prevent instantiation
    }

    public static double getBaseFee(String api) {
        System.out.println("BASE FEE: " + api);
        if (api.equals("PerKey")) return SIMPLE_FEES_SCHEDULE.getExtrasFee("Keys");
        if (api.equals("PerFileByte")) return SIMPLE_FEES_SCHEDULE.getExtrasFee("Bytes");
        if (api.equals("PerHCSByte")) return SIMPLE_FEES_SCHEDULE.getExtrasFee("Bytes");
        if (api.equals("ConsensusCreateTopic")
                || api.equals("ConsensusCreateTopicWithCustomFee")
                || api.equals("ConsensusUpdateTopic")
                || api.equals("ConsensusSubmitMessage")
                || api.equals("ConsensusSubmitMessageWithCustomFee")
                || api.equals("ConsensusDeleteTopic")
        ) {
            return SIMPLE_FEES_SCHEDULE.getBaseFee(api);
        }
        return BASE_FEES.getOrDefault(api, 0.0);
    }

    public static AbstractSimpleFeesSchedule getFeeSchedule() {
        return SIMPLE_FEES_SCHEDULE;
    }

}

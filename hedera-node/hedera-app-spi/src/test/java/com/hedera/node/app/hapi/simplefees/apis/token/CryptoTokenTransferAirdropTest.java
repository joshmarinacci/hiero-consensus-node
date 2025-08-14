package com.hedera.node.app.hapi.simplefees.apis.token;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.node.app.hapi.simplefees.MockFeesSchedule;
import com.hedera.node.app.hapi.simplefees.apis.MockExchangeRate;
import com.hedera.node.app.hapi.simplefees.apis.crypto.CryptoTransfer;
import com.hedera.node.app.spi.fees.Fees;
import org.hiero.hapi.support.fees.Extra;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hedera.hapi.node.base.HederaFunctionality.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CryptoTokenTransferAirdropTest {
    static MockFeesSchedule feesSchedule;

    @BeforeAll
    static void setup() {
        feesSchedule = new MockFeesSchedule();
        feesSchedule.setExtrasFee(Extra.KEYS,1L);
        feesSchedule.setExtrasFee(Extra.SIGNATURES,2L);


        feesSchedule.setServiceBaseFee(CRYPTO_CREATE,22L);
        feesSchedule.setServiceExtraIncludedCount(CRYPTO_CREATE, Extra.KEYS,2L);

        feesSchedule.setServiceBaseFee(CRYPTO_TRANSFER,10L);
        feesSchedule.setServiceExtraIncludedCount(CRYPTO_TRANSFER, Extra.KEYS,1L);

        feesSchedule.setServiceBaseFee(TOKEN_CREATE,33L);
        feesSchedule.setServiceExtraIncludedCount(TOKEN_CREATE, Extra.KEYS,7L);

//        feesSchedule.setServiceBaseFee("TokenTransfer",33L);
//        feesSchedule.setServiceExtraIncludedCount("TokenTransfer", Extra.KEYS,2L);
//        feesSchedule.setServiceBaseFee("TokenTransferWithCustomFee",33L);
//        feesSchedule.setServiceExtraIncludedCount("TokenTransferWithCustomFee", Extra.KEYS,2L);
//
//        feesSchedule.setServiceBaseFee(TOKEN_AIRDROP,33L);
//        feesSchedule.setServiceExtraIncludedCount(TOKEN_AIRDROP, Extra.KEYS,7L);
//        feesSchedule.setServiceBaseFee("TokenAirdropWithCustomFee",33L);
//        feesSchedule.setServiceExtraIncludedCount("TokenAirdropWithCustomFee", Extra.KEYS,7L);
//
//        feesSchedule.setServiceBaseFee("TokenCreateWithCustomFee",38L);
//        feesSchedule.setServiceExtraIncludedCount("TokenCreateWithCustomFee", Extra.KEYS,7L);

        feesSchedule.setServiceBaseFee(CONSENSUS_CREATE_TOPIC,15L);
        feesSchedule.setServiceExtraIncludedCount(CONSENSUS_CREATE_TOPIC, Extra.KEYS,1L);

        feesSchedule.setServiceBaseFee(CONTRACT_CREATE,15L);
        feesSchedule.setServiceExtraIncludedCount(CONTRACT_CREATE, Extra.KEYS,1L);

        feesSchedule.setServiceBaseFee(SCHEDULE_CREATE,15L);
        feesSchedule.setServiceExtraIncludedCount(SCHEDULE_CREATE, Extra.KEYS,1L);
    }
    List<TransferTestScenario> scenarios = List.of(
            // Either Crypto or TokenTransfer with no tokens should default to CryptoTransfer price
//            new TransferTestScenario("CryptoTransfer", 1, 2, 0, 0, 0, 0, 0, 0, feesSchedule.getServiceBaseFee(CRYPTO_TRANSFER)),
//            new TransferTestScenario("TokenTransfer", 1, 2, 0, 0, 0, 0, 0, 0, feesSchedule.getServiceBaseFee(CRYPTO_TRANSFER)),

            // Either Crypto or TokenTransfer with tokens should default to TokenTransfer price
//            new TransferTestScenario("CryptoTransfer", 1, 2, 1, 0, 0, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenTransfer")),
//            new TransferTestScenario("TokenTransfer", 1, 2, 1, 0, 0, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenTransfer")),

            // Either Crypto or TokenTransfer with tokens without custom fees should charge for those while giving one token transfer free
//            new TransferTestScenario("CryptoTransfer", 1, 2, 2, 0, 0, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenTransfer") + feesSchedule.getServiceBaseFee("TokenTransfer")),
//            new TransferTestScenario("CryptoTransfer", 1, 2, 0, 2, 0, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenTransfer")+ feesSchedule.getServiceBaseFee("TokenTransfer")),

//            new TransferTestScenario("TokenTransfer", 1, 2, 10, 0, 0, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenTransfer")+ 9 * feesSchedule.getServiceBaseFee("TokenTransfer")),
//            new TransferTestScenario("TokenTransfer", 1, 2, 0, 10, 0, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenTransfer")+ 9 * feesSchedule.getServiceBaseFee("TokenTransfer")),
//            new TransferTestScenario("TokenAirdrop", 1, 2, 0, 10, 0, 0, 0, 0, feesSchedule.getServiceBaseFee(TOKEN_AIRDROP)+ 9 * feesSchedule.getServiceBaseFee("TokenTransfer")),

            // Any API involving one token with custom fees should include that in the base price
//            new TransferTestScenario("CryptoTransfer", 1, 2, 0, 0, 1, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee")),
//            new TransferTestScenario("TokenTransfer", 1, 2, 0, 0, 1, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee")),
//            new TransferTestScenario("TokenAirdrop", 1, 2, 0, 0, 1, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenAirdropWithCustomFee")),
//
//            // Any API involving more than one token with custom fees should include the custom fee token in the base price, and include other tokens
//            new TransferTestScenario("CryptoTransfer", 1, 2, 1, 1, 2, 4, 0, 0, feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee") + 2 * feesSchedule.getServiceBaseFee("TokenTransfer") + 5 * feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee")),
//            new TransferTestScenario("TokenTransfer", 1, 2, 1, 1, 2, 4, 0, 0, feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee") + 2 * feesSchedule.getServiceBaseFee("TokenTransfer") + 5 * feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee")),
//            new TransferTestScenario("TokenAirdrop", 1, 2, 1, 1, 2, 4, 0, 0, feesSchedule.getServiceBaseFee("TokenAirdropWithCustomFee") + 2 * feesSchedule.getServiceBaseFee("TokenTransfer") + 5 * feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee")),
//
//            // Every API should charge overages for signatures, accounts, auto-associations, and auto-account-creations
//            new TransferTestScenario("CryptoTransfer", 5, 6, 1, 0, 0, 0, 3, 4, feesSchedule.getServiceBaseFee("TokenTransfer")  + 4 * feesSchedule.getExtrasFee(Extra.SIGNATURES) + 4 * feesSchedule.getServiceBaseFee("PerCryptoTransferAccount") + 3 * feesSchedule.getServiceBaseFee("TokenAssociateToAccount") + 4 * feesSchedule.getServiceBaseFee("CryptoCreate")),
//            new TransferTestScenario("TokenTransfer", 5, 6, 1, 0, 0, 0, 3, 4, feesSchedule.getServiceBaseFee("TokenTransfer")  + 4 * feesSchedule.getServiceBaseFee("PerSignature") + 4 * feesSchedule.getServiceBaseFee("PerCryptoTransferAccount") + 3 * feesSchedule.getServiceBaseFee("TokenAssociateToAccount") + 4 * feesSchedule.getServiceBaseFee("CryptoCreate")),
//            new TransferTestScenario("TokenAirdrop", 5, 6, 1, 0, 0, 0, 3, 4, feesSchedule.getServiceBaseFee("TokenAirdrop") + 4 * feesSchedule.getServiceBaseFee("PerSignature") + 4 * feesSchedule.getServiceBaseFee("PerCryptoTransferAccount") + 3 * feesSchedule.getServiceBaseFee("TokenAssociateToAccount") + 4 * feesSchedule.getServiceBaseFee("CryptoCreate"))

    );

    @Test
    void testPredefinedScenarios() {
        for (var scenario: scenarios) {
            CryptoTransfer transfer = switch (scenario.api) {
                case "CryptoTransfer" -> new CryptoTransfer("Crypto", "CryptoTransfer");
                case "TokenTransfer" -> new CryptoTransfer("Token", "TokenTransfer");
                case "TokenAirdrop" -> new CryptoTransfer("Crypto", "TokenAirdrop");
                default -> throw new IllegalStateException("Unexpected value: " + scenario.api);
            };

            Map<String, Object> params = new HashMap<>();
            params.put(Extra.SIGNATURES.name(), scenario.numSignatures);
            params.put("numAccountsInvolved", scenario.numAccountsInvolved);
            params.put("numFTNoCustomFeeEntries", scenario.numFTNoCustomFeeEntries);
            params.put("numNFTNoCustomFeeEntries", scenario.numNFTNoCustomFeeEntries);
            params.put("numFTWithCustomFeeEntries", scenario.numFTWithCustomFeeEntries);
            params.put("numNFTWithCustomFeeEntries", scenario.numNFTWithCustomFeeEntries);
            params.put("numAutoAssociationsCreated", scenario.numAutoAssociationsCreated);
            params.put("numAutoAccountsCreated", scenario.numAutoAccountsCreated);

            Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), feesSchedule);
            assertEquals(scenario.expectedFee, fee.usd(), 1e-9, "hbar/token/airdrop test: " + scenario);
        }
    }
}

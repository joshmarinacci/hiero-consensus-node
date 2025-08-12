package com.hedera.node.app.hapi.fees.apis.token;

import com.hedera.node.app.hapi.fees.MockFeesSchedule;
import com.hedera.node.app.hapi.fees.apis.MockExchangeRate;
import com.hedera.node.app.hapi.fees.apis.common.FeeConstants.Extras;
import com.hedera.node.app.hapi.fees.apis.crypto.CryptoTransfer;
import com.hedera.node.app.spi.fees.Fees;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CryptoTokenTransferAirdropTest {
    static MockFeesSchedule feesSchedule;

    @BeforeAll
    static void setup() {
        feesSchedule = new MockFeesSchedule();
        feesSchedule.setExtrasFee(Extras.Keys.toString(),1L);
        feesSchedule.setExtrasFee(Extras.Signatures.toString(),2L);


        feesSchedule.setServiceBaseFee("CryptoCreate",22L);
        feesSchedule.setServiceExtraIncludedCount("CryptoCreate", Extras.Keys,2L);

        feesSchedule.setServiceBaseFee("CryptoTransfer",10L);
        feesSchedule.setServiceExtraIncludedCount("CryptoTransfer", Extras.Keys,1L);

        feesSchedule.setServiceBaseFee("TokenCreate",33L);
        feesSchedule.setServiceExtraIncludedCount("TokenCreate", Extras.Keys,7L);

        feesSchedule.setServiceBaseFee("TokenTransfer",33L);
        feesSchedule.setServiceExtraIncludedCount("TokenTransfer", Extras.Keys,2L);
        feesSchedule.setServiceBaseFee("TokenTransferWithCustomFee",33L);
        feesSchedule.setServiceExtraIncludedCount("TokenTransferWithCustomFee", Extras.Keys,2L);

        feesSchedule.setServiceBaseFee("TokenAirdrop",33L);
        feesSchedule.setServiceExtraIncludedCount("TokenAirdrop", Extras.Keys,7L);
        feesSchedule.setServiceBaseFee("TokenAirdropWithCustomFee",33L);
        feesSchedule.setServiceExtraIncludedCount("TokenAirdropWithCustomFee", Extras.Keys,7L);

        feesSchedule.setServiceBaseFee("TokenCreateWithCustomFee",38L);
        feesSchedule.setServiceExtraIncludedCount("TokenCreateWithCustomFee", Extras.Keys,7L);

        feesSchedule.setServiceBaseFee("ConsensusCreateTopic",15L);
        feesSchedule.setServiceExtraIncludedCount("ConsensusCreateTopic", Extras.Keys,1L);

        feesSchedule.setServiceBaseFee("ContractCreate",15L);
        feesSchedule.setServiceExtraIncludedCount("ContractCreate", Extras.Keys,1L);

        feesSchedule.setServiceBaseFee("ScheduleCreate",15L);
        feesSchedule.setServiceExtraIncludedCount("ScheduleCreate", Extras.Keys,1L);
    }
    List<TransferTestScenario> scenarios = List.of(
            // Either Crypto or TokenTransfer with no tokens should default to CryptoTransfer price
            new TransferTestScenario("CryptoTransfer", 1, 2, 0, 0, 0, 0, 0, 0, feesSchedule.getServiceBaseFee("CryptoTransfer")),
            new TransferTestScenario("TokenTransfer", 1, 2, 0, 0, 0, 0, 0, 0, feesSchedule.getServiceBaseFee("CryptoTransfer")),

            // Either Crypto or TokenTransfer with tokens should default to TokenTransfer price
            new TransferTestScenario("CryptoTransfer", 1, 2, 1, 0, 0, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenTransfer")),
            new TransferTestScenario("TokenTransfer", 1, 2, 1, 0, 0, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenTransfer")),

            // Either Crypto or TokenTransfer with tokens without custom fees should charge for those while giving one token transfer free
            new TransferTestScenario("CryptoTransfer", 1, 2, 2, 0, 0, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenTransfer") + feesSchedule.getServiceBaseFee("TokenTransfer")),
            new TransferTestScenario("CryptoTransfer", 1, 2, 0, 2, 0, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenTransfer")+ feesSchedule.getServiceBaseFee("TokenTransfer")),

            new TransferTestScenario("TokenTransfer", 1, 2, 10, 0, 0, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenTransfer")+ 9 * feesSchedule.getServiceBaseFee("TokenTransfer")),
            new TransferTestScenario("TokenTransfer", 1, 2, 0, 10, 0, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenTransfer")+ 9 * feesSchedule.getServiceBaseFee("TokenTransfer")),
            new TransferTestScenario("TokenAirdrop", 1, 2, 0, 10, 0, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenAirdrop")+ 9 * feesSchedule.getServiceBaseFee("TokenTransfer")),

            // Any API involving one token with custom fees should include that in the base price
            new TransferTestScenario("CryptoTransfer", 1, 2, 0, 0, 1, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee")),
            new TransferTestScenario("TokenTransfer", 1, 2, 0, 0, 1, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee")),
            new TransferTestScenario("TokenAirdrop", 1, 2, 0, 0, 1, 0, 0, 0, feesSchedule.getServiceBaseFee("TokenAirdropWithCustomFee")),

            // Any API involving more than one token with custom fees should include the custom fee token in the base price, and include other tokens
            new TransferTestScenario("CryptoTransfer", 1, 2, 1, 1, 2, 4, 0, 0, feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee") + 2 * feesSchedule.getServiceBaseFee("TokenTransfer") + 5 * feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee")),
            new TransferTestScenario("TokenTransfer", 1, 2, 1, 1, 2, 4, 0, 0, feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee") + 2 * feesSchedule.getServiceBaseFee("TokenTransfer") + 5 * feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee")),
            new TransferTestScenario("TokenAirdrop", 1, 2, 1, 1, 2, 4, 0, 0, feesSchedule.getServiceBaseFee("TokenAirdropWithCustomFee") + 2 * feesSchedule.getServiceBaseFee("TokenTransfer") + 5 * feesSchedule.getServiceBaseFee("TokenTransferWithCustomFee")),

            // Every API should charge overages for signatures, accounts, auto-associations, and auto-account-creations
            new TransferTestScenario("CryptoTransfer", 5, 6, 1, 0, 0, 0, 3, 4, feesSchedule.getServiceBaseFee("TokenTransfer")  + 4 * feesSchedule.getExtrasFee(Extras.Signatures.toString()) + 4 * feesSchedule.getServiceBaseFee("PerCryptoTransferAccount") + 3 * feesSchedule.getServiceBaseFee("TokenAssociateToAccount") + 4 * feesSchedule.getServiceBaseFee("CryptoCreate")),
            new TransferTestScenario("TokenTransfer", 5, 6, 1, 0, 0, 0, 3, 4, feesSchedule.getServiceBaseFee("TokenTransfer")  + 4 * feesSchedule.getServiceBaseFee("PerSignature") + 4 * feesSchedule.getServiceBaseFee("PerCryptoTransferAccount") + 3 * feesSchedule.getServiceBaseFee("TokenAssociateToAccount") + 4 * feesSchedule.getServiceBaseFee("CryptoCreate")),
            new TransferTestScenario("TokenAirdrop", 5, 6, 1, 0, 0, 0, 3, 4, feesSchedule.getServiceBaseFee("TokenAirdrop") + 4 * feesSchedule.getServiceBaseFee("PerSignature") + 4 * feesSchedule.getServiceBaseFee("PerCryptoTransferAccount") + 3 * feesSchedule.getServiceBaseFee("TokenAssociateToAccount") + 4 * feesSchedule.getServiceBaseFee("CryptoCreate"))

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
            params.put(Extras.Signatures.toString(), scenario.numSignatures);
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

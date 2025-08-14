package com.hedera.node.app.hapi.simplefees.apis.token;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.node.app.hapi.simplefees.MockFeesSchedule;
import com.hedera.node.app.hapi.simplefees.apis.MockExchangeRate;
import com.hedera.node.app.spi.fees.Fees;
import org.hiero.hapi.support.fees.Extra;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hedera.hapi.node.base.HederaFunctionality.*;
import static org.hiero.hapi.support.fees.Extra.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenAirdropOperationsTest {
    static MockFeesSchedule schedule;

    @BeforeAll
    static void setup() {
        schedule = new MockFeesSchedule();
    }

    static class TokenAirdropOperationsTestScenario {
        String api;
        int numSignatures;
        int numTokenTypes;
        double expectedFee;

        public TokenAirdropOperationsTestScenario(String api, int numSignatures, int numTokenTypes, double expectedFee) {
            this.api = api;
            this.numSignatures = numSignatures;
            this.numTokenTypes = numTokenTypes;
            this.expectedFee = expectedFee;
        }

        @Override
        public String toString() {
            return "AirdropOperationsTestScenario{" +
                    "api='" + api + '\'' +
                    ", numSignatures=" + numSignatures +
                    ", numTokenTypes=" + numTokenTypes +
                    ", expectedFee=" + expectedFee +
                    '}';
        }
    }

    List<TokenAirdropOperationsTestScenario> scenarios = List.of(
            // Base cases - 1 token should be included in the base price
            new TokenAirdropOperationsTestScenario("TokenClaimAirdrop", 1, 1, schedule.getServiceBaseFee(TOKEN_CLAIM_AIRDROP)),
            new TokenAirdropOperationsTestScenario("TokenCancelAirdrop", 1, 1, schedule.getServiceBaseFee(TOKEN_CANCEL_AIRDROP)),
            new TokenAirdropOperationsTestScenario("TokenRejectAirdrop", 1, 1, schedule.getServiceBaseFee(TOKEN_REJECT)),

            // Multiple tokens case
            new TokenAirdropOperationsTestScenario("TokenClaimAirdrop", 1, 10, 10 * schedule.getServiceBaseFee(TOKEN_CLAIM_AIRDROP)),
            new TokenAirdropOperationsTestScenario("TokenCancelAirdrop", 1, 10, 10 * schedule.getServiceBaseFee(TOKEN_CANCEL_AIRDROP)),
            new TokenAirdropOperationsTestScenario("TokenRejectAirdrop", 1, 10, 10 * schedule.getServiceBaseFee(TOKEN_REJECT)),

            // Additional signatures are charged
            new TokenAirdropOperationsTestScenario("TokenClaimAirdrop", 5, 10, 10 * schedule.getServiceBaseFee(TOKEN_CLAIM_AIRDROP) + 4 * schedule.getExtrasFee(SIGNATURES)),
            new TokenAirdropOperationsTestScenario("TokenCancelAirdrop", 5, 10, 10 * schedule.getServiceBaseFee(TOKEN_CANCEL_AIRDROP) + 4 * schedule.getExtrasFee(SIGNATURES)),
            new TokenAirdropOperationsTestScenario("TokenRejectAirdrop", 5, 10, 10 * schedule.getServiceBaseFee(TOKEN_REJECT) + 4 * schedule.getExtrasFee(SIGNATURES))
    );

    @Test
    void testPredefinedScenarios() {
        for (var scenario: scenarios) {
            TokenAirdropOperations op = switch (scenario.api) {
                case "TokenClaimAirdrop" -> new TokenAirdropOperations("TokenClaimAirdrop", "dummy");
                case "TokenCancelAirdrop" -> new TokenAirdropOperations("TokenCancelAirdrop", "dummy");
                case "TokenRejectAirdrop" -> new TokenAirdropOperations("TokenRejectAirdrop", "dummy");
                default -> throw new IllegalStateException("Unexpected value: " + scenario.api);
            };

            Map<String, Object> params = new HashMap<>();
            params.put(SIGNATURES.name(), scenario.numSignatures);
            params.put(TOKEN_TYPES.name(), scenario.numTokenTypes);
            Fees fee = op.computeFee(params, new MockExchangeRate().activeRate(), schedule);
            assertEquals(scenario.expectedFee, fee.usd(), 1e-9, "Airdrop claim,cancel,reject test: " + scenario);
        }
    }
}

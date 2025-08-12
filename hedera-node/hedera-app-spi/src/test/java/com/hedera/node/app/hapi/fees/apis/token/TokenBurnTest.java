package com.hedera.node.app.hapi.fees.apis.token;

import com.hedera.node.app.hapi.fees.MockFeesSchedule;
import com.hedera.node.app.hapi.fees.apis.common.FTOrNFT;
import com.hedera.node.app.hapi.fees.apis.MockExchangeRate;
import com.hedera.node.app.hapi.fees.apis.common.FeeConstants.Extras;
import com.hedera.node.app.spi.fees.Fees;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenBurnTest {
    static MockFeesSchedule schedule;

    @BeforeAll
    static void setup() {
        schedule = new MockFeesSchedule();
    }

    @Test
    void testTokenBurnSingle() {
        TokenBurn topic = new TokenBurn();
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put("fungibleOrNonFungible", FTOrNFT.NonFungible);
        params.put("numTokens", 1);

        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0.001, fee.usd(), "Token Burn");
    }

    @Test
    void testTokenBurnMultipleFungible() {
        TokenBurn topic = new TokenBurn();
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put("fungibleOrNonFungible", FTOrNFT.Fungible);
        params.put("numTokens", 10);

        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0.001, fee.usd(), "Token Burn Fungible - 10");
    }
    @Test
    void testTokenBurnMultipleNonFungible() {
        TokenBurn topic = new TokenBurn();
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put("fungibleOrNonFungible", FTOrNFT.NonFungible);
        params.put("numTokens", 10);

        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0.001 * 10, fee.usd(), "Token Burn Non Fungible - 10");
    }

    @Test
    void testTokenBurnMultipleWithMultipleSignatures() {
        TokenBurn topic = new TokenBurn();
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 5L);
        params.put("fungibleOrNonFungible", FTOrNFT.NonFungible);
        params.put("numTokens", 10);

        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0.001 * 10 + 4 * 0.0001, fee.usd(), "Token Burn - 10 with multiple signatures");
    }

}

package com.hedera.node.app.hapi.simplefees.apis.token;

import com.hedera.node.app.hapi.simplefees.MockFeesSchedule;
import com.hedera.node.app.hapi.simplefees.apis.MockExchangeRate;
import com.hedera.node.app.spi.fees.Fees;
import org.hiero.hapi.support.fees.Extra;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenMintTest {
    static MockFeesSchedule schedule;

    @BeforeAll
    static void setup() {
        schedule = new MockFeesSchedule();
        schedule.setExtrasFee(Extra.SIGNATURES,1L);
        schedule.setExtrasFee(Extra.KEYS,1L);
        schedule.setExtrasFee(Extra.ACCOUNTS,2L);
        schedule.setExtrasFee(Extra.STANDARD_FUNGIBLE_TOKENS, 5L);

        schedule.setServiceBaseFee("TokenMint",10L);
        schedule.setServiceExtraIncludedCount("TokenMint", Extra.STANDARD_FUNGIBLE_TOKENS,1L);
        schedule.setServiceExtraIncludedCount("TokenMint", Extra.STANDARD_NON_FUNGIBLE_TOKENS,1L);
    }

    @Test
    void testTokenFTMint() {
        TokenMint topic = new TokenMint();
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.toString(), 1L);
        params.put(Extra.STANDARD_FUNGIBLE_TOKENS.toString(), 10L);
        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10 + (10-1)*5, fee.usd(), "Fungible Token Mint");
    }

    @Test
    void testTokenNFTMintOne() {
        TokenMint topic = new TokenMint();
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.toString(), 1L);
        params.put(Extra.STANDARD_FUNGIBLE_TOKENS.toString(), 0L);
        params.put(Extra.STANDARD_NON_FUNGIBLE_TOKENS.toString(), 10L);
        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10, fee.usd(), "Non Fungible Token Mint - 1");
    }

    @Test
    void testTokenNFTMintMultiple() {
        TokenMint topic = new TokenMint();
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.toString(), 1L);
        params.put(Extra.STANDARD_NON_FUNGIBLE_TOKENS.toString(), 10L);
        params.put(Extra.STANDARD_FUNGIBLE_TOKENS.toString(), 0L);
        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10, fee.usd(), "Non Fungible Token Mint - 10");
    }


    @Test
    void testTokenMintWithMultipleSignatures() {
        TokenMint topic = new TokenMint();
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.toString(), 6L);
        params.put(Extra.STANDARD_FUNGIBLE_TOKENS.toString(), 0L);
        params.put(Extra.STANDARD_NON_FUNGIBLE_TOKENS.toString(), 1L);
        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10, fee.usd(), "NFT mint with multiple signatures");
    }
}

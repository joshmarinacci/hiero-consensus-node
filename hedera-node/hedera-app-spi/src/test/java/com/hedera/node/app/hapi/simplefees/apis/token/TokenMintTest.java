package com.hedera.node.app.hapi.simplefees.apis.token;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.node.app.hapi.simplefees.MockFeesSchedule;
import com.hedera.node.app.hapi.simplefees.apis.MockExchangeRate;
import com.hedera.node.app.spi.fees.Fees;
import org.hiero.hapi.support.fees.Extra;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.hedera.hapi.node.base.HederaFunctionality.*;
import static org.hiero.hapi.support.fees.Extra.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenMintTest {
    static MockFeesSchedule schedule;

    @BeforeAll
    static void setup() {
        schedule = new MockFeesSchedule();
        schedule.setExtrasFee(SIGNATURES,1L);
        schedule.setExtrasFee(KEYS,1L);
        schedule.setExtrasFee(ACCOUNTS,2L);
        schedule.setExtrasFee(STANDARD_FUNGIBLE_TOKENS, 5L);

        schedule.setServiceBaseFee(TOKEN_MINT,10L);
        schedule.setServiceExtraIncludedCount(TOKEN_MINT, STANDARD_FUNGIBLE_TOKENS,1L);
        schedule.setServiceExtraIncludedCount(TOKEN_MINT, STANDARD_NON_FUNGIBLE_TOKENS,1L);
    }

    @Test
    void testTokenFTMint() {
        TokenMint topic = new TokenMint();
        Map<String, Object> params = new HashMap<>();
        params.put(SIGNATURES.toString(), 1L);
        params.put(STANDARD_FUNGIBLE_TOKENS.toString(), 10L);
        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10 + (10-1)*5, fee.usd(), "Fungible Token Mint");
    }

    @Test
    void testTokenNFTMintOne() {
        TokenMint topic = new TokenMint();
        Map<String, Object> params = new HashMap<>();
        params.put(SIGNATURES.toString(), 1L);
        params.put(STANDARD_FUNGIBLE_TOKENS.toString(), 0L);
        params.put(STANDARD_NON_FUNGIBLE_TOKENS.toString(), 10L);
        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10, fee.usd(), "Non Fungible Token Mint - 1");
    }

    @Test
    void testTokenNFTMintMultiple() {
        TokenMint topic = new TokenMint();
        Map<String, Object> params = new HashMap<>();
        params.put(SIGNATURES.toString(), 1L);
        params.put(STANDARD_NON_FUNGIBLE_TOKENS.toString(), 10L);
        params.put(STANDARD_FUNGIBLE_TOKENS.toString(), 0L);
        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10, fee.usd(), "Non Fungible Token Mint - 10");
    }


    @Test
    void testTokenMintWithMultipleSignatures() {
        TokenMint topic = new TokenMint();
        Map<String, Object> params = new HashMap<>();
        params.put(SIGNATURES.toString(), 6L);
        params.put(STANDARD_FUNGIBLE_TOKENS.toString(), 0L);
        params.put(STANDARD_NON_FUNGIBLE_TOKENS.toString(), 1L);
        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10, fee.usd(), "NFT mint with multiple signatures");
    }
}

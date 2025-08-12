package com.hedera.node.app.hapi.fees.apis.token;

import com.hedera.node.app.hapi.fees.MockFeesSchedule;
import com.hedera.node.app.hapi.fees.apis.MockExchangeRate;
import com.hedera.node.app.hapi.fees.apis.common.FeeConstants.Extras;
import com.hedera.node.app.spi.fees.Fees;
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
        schedule.setExtrasFee(Extras.Signatures,1L);
        schedule.setExtrasFee(Extras.Keys,1L);
        schedule.setExtrasFee(Extras.Accounts,2L);
        schedule.setExtrasFee(Extras.StandardFungibleTokens, 5L);

        schedule.setServiceBaseFee("TokenMint",10L);
        schedule.setServiceExtraIncludedCount("TokenMint", Extras.StandardFungibleTokens.name(),1L);
        schedule.setServiceExtraIncludedCount("TokenMint", Extras.StandardNonFungibleTokens.name(),1L);
    }

    @Test
    void testTokenFTMint() {
        TokenMint topic = new TokenMint();
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put(Extras.StandardFungibleTokens.name(), 10L);
        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10 + (10-1)*5, fee.usd(), "Fungible Token Mint");
    }

    @Test
    void testTokenNFTMintOne() {
        TokenMint topic = new TokenMint();
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put(Extras.StandardFungibleTokens.name(), 0L);
        params.put(Extras.StandardNonFungibleTokens.name(), 10L);
        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10, fee.usd(), "Non Fungible Token Mint - 1");
    }

    @Test
    void testTokenNFTMintMultiple() {
        TokenMint topic = new TokenMint();
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put(Extras.StandardNonFungibleTokens.name(), 10L);
        params.put(Extras.StandardFungibleTokens.name(), 0L);
        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10, fee.usd(), "Non Fungible Token Mint - 10");
    }


    @Test
    void testTokenMintWithMultipleSignatures() {
        TokenMint topic = new TokenMint();
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 6L);
        params.put(Extras.StandardFungibleTokens.name(), 0L);
        params.put(Extras.StandardNonFungibleTokens.name(), 1L);
        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10, fee.usd(), "NFT mint with multiple signatures");
    }
}

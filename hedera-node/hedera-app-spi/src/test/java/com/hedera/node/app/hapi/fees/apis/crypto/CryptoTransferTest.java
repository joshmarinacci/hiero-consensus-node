package com.hedera.node.app.hapi.fees.apis.crypto;

import com.hedera.node.app.hapi.fees.FeeCheckResult;
import com.hedera.node.app.hapi.fees.MockFeesSchedule;
import com.hedera.node.app.hapi.fees.apis.MockExchangeRate;
import com.hedera.node.app.hapi.fees.apis.common.FeeConstants.Extras;
import com.hedera.node.app.spi.fees.Fees;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CryptoTransferTest {
    static MockFeesSchedule schedule;

    @BeforeAll
    static void setup() {
        schedule = new MockFeesSchedule();
        schedule.setExtrasFee(Extras.Signatures,1L);
        schedule.setExtrasFee(Extras.Keys,1L);
        schedule.setExtrasFee(Extras.Accounts,2L);
        schedule.setExtrasFee(Extras.StandardFungibleTokens, 5L);

        schedule.setServiceBaseFee("CryptoTransfer",10L);
        schedule.setServiceExtraIncludedCount("CryptoTransfer", Extras.Keys,2L);
        schedule.setServiceExtraIncludedCount("CryptoTransfer", Extras.StandardFungibleTokens,2L);
        schedule.setServiceExtraIncludedCount("CryptoTransfer", Extras.Accounts,1L);

        schedule.setServiceBaseFee("TokenTransfer",15L);
        schedule.setServiceExtraIncludedCount("TokenTransfer", Extras.Keys,1L);
        schedule.setServiceExtraIncludedCount("TokenTransfer", Extras.Accounts,2L);
        schedule.setServiceExtraIncludedCount("TokenTransfer", Extras.StandardFungibleTokens,2L);
    }

    @Test
    void testSimpleHbarTransfer() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", "CryptoTransfer");
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put(Extras.Accounts.name(), 2L);
        Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10+2, fee.usd(), "Simple hbar transfer");
    }

    @Test
    void testMultipleHbarTransfer() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", "CryptoTransfer");
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put(Extras.Accounts.name(), 5L);
        Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10 + (5-1)*2, fee.usd(), "Multiple hbar transfer");
    }

    @Test
    void testTokenTransfer() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", "CryptoTransfer");
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put(Extras.Accounts.name(), 5L);
        params.put(Extras.StandardFungibleTokens.name(), 1L);
        Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(15 +  (5-2)*2, fee.usd(), "Simple token transfer");
    }

    @Test
    void testMultipleTokenTransfer() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", "CryptoTransfer");
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put(Extras.Accounts.name(), 10L);
        params.put(Extras.StandardFungibleTokens.name(), 5L);

        Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(15 + ((10-2) * 2) + ((5-2) * 15), fee.usd(),"Multiple token transfers");
    }

    @Test
    void testMultipleHbarAndMultipleTokenTransfer() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", "CryptoTransfer");
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put(Extras.Accounts.name(), 10L);
        params.put(Extras.StandardFungibleTokens.name(), 5L);

        Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(15  + (10-2)*2 + (5-2) * 15, fee.usd(),"Multiple hbar and token transfers");
    }

    @Test
    void testInvalidParamsFailCheck() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", "CryptoTransfer");
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Accounts.name(), 0L);
        params.put(Extras.StandardFungibleTokens.name(), 0L);
        params.put("numNFTNoCustomFeeEntries", 0);
        params.put("numFTWithCustomFeeEntries", 0);
        params.put("numNFTWithCustomFeeEntries", 0);
        params.put("numAutoAssociationsCreated", 0);
        params.put("numAutoAccountsCreated", 0);
        params.put(Extras.Signatures.name(), 1L);

        FeeCheckResult result = transfer.checkParameters(params);
        assertFalse(result.result, "Expected parameters to fail due to minimum transfer entry rule");
    }
}

package com.hedera.node.app.hapi.fees.apis.crypto;

import com.hedera.node.app.hapi.fees.AbstractFeesSchedule.Extras;
import com.hedera.node.app.hapi.fees.FeeCheckResult;
import com.hedera.node.app.hapi.fees.MockFeesSchedule;
import com.hedera.node.app.hapi.fees.apis.MockExchangeRate;
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
        schedule.setExtrasFee(Extras.Keys,1L);

        schedule.setServiceBaseFee("CryptoTransfer",10L);
        schedule.setServiceExtraIncludedCount("CryptoTransfer", Extras.Keys,2L);
        schedule.setServiceBaseFee("TokenTransfer",15L);
    }

    @Test
    void testSimpleHbarTransfer() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", "CryptoTransfer");
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put("numAccountsInvolved", 2L);
        Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10, fee.usd(), "Simple hbar transfer");
    }

    @Test
    void testMultipleHbarTransfer() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", "CryptoTransfer");
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put("numAccountsInvolved", 5);
        Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(13 /* 0.0001 + (3 * 0.00001) */, fee.usd(), "Multiple hbar transfer");
    }

    @Test
    void testTokenTransfer() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", "CryptoTransfer");
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put("numAccountsInvolved", 5);
        params.put("numFTNoCustomFeeEntries", 1);
        Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(   (3 * 1) + 10, fee.usd(), "Simple token transfer");
    }

    @Test
    void testMultipleTokenTransfer() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", "CryptoTransfer");
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put("numAccountsInvolved", 10);
        params.put("numFTNoCustomFeeEntries", 5);

        Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals( (8 * 0.00001) + (5 * 0.001), fee.usd(),"Multiple token transfers");
    }

    @Test
    void testMultipleHbarAndMultipleTokenTransfer() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", "CryptoTransfer");
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put("numAccountsInvolved", 10);
        params.put("numFTNoCustomFeeEntries", 5);

        Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals((8 * 0.00001) + (5 * 0.001), fee.usd(),"Multiple hbar and token transfers");
    }

    @Test
    void testInvalidParamsFailCheck() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", "CryptoTransfer");
        Map<String, Object> params = new HashMap<>();
        params.put("numAccountsInvolved", 0);
        params.put("numFTNoCustomFeeEntries", 0);
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

package com.hedera.node.app.hapi.simplefees.apis.crypto;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.node.app.hapi.simplefees.FeeCheckResult;
import com.hedera.node.app.hapi.simplefees.MockFeesSchedule;
import com.hedera.node.app.hapi.simplefees.apis.MockExchangeRate;
import com.hedera.node.app.spi.fees.Fees;
import org.hiero.hapi.support.fees.Extra;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.hedera.hapi.node.base.HederaFunctionality.*;
import static org.junit.jupiter.api.Assertions.*;

class CryptoTransferTest {
    static MockFeesSchedule schedule;

    @BeforeAll
    static void setup() {
        schedule = new MockFeesSchedule();
        schedule.setExtrasFee(Extra.SIGNATURES,1L);
        schedule.setExtrasFee(Extra.KEYS,1L);
        schedule.setExtrasFee(Extra.ACCOUNTS,2L);
        schedule.setExtrasFee(Extra.STANDARD_FUNGIBLE_TOKENS, 5L);

        schedule.setServiceBaseFee(CRYPTO_TRANSFER,10L);
        schedule.setServiceExtraIncludedCount(CRYPTO_TRANSFER, Extra.KEYS,2L);
        schedule.setServiceExtraIncludedCount(CRYPTO_TRANSFER, Extra.STANDARD_FUNGIBLE_TOKENS,2L);
        schedule.setServiceExtraIncludedCount(CRYPTO_TRANSFER, Extra.ACCOUNTS,1L);

//        schedule.setServiceBaseFee("TokenTransfer",15L);
//        schedule.setServiceExtraIncludedCount("TokenTransfer", Extra.KEYS,1L);
//        schedule.setServiceExtraIncludedCount("TokenTransfer", Extra.ACCOUNTS,2L);
//        schedule.setServiceExtraIncludedCount("TokenTransfer", Extra.STANDARD_FUNGIBLE_TOKENS,2L);
    }

    @Test
    void testSimpleHbarTransfer() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", CRYPTO_TRANSFER);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.ACCOUNTS.name(), 2L);
        Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10+2, fee.usd(), "Simple hbar transfer");
    }

    @Test
    void testMultipleHbarTransfer() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", CRYPTO_TRANSFER);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.ACCOUNTS.name(), 5L);
        Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10 + (5-1)*2, fee.usd(), "Multiple hbar transfer");
    }

    @Test
    void testTokenTransfer() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", CRYPTO_TRANSFER);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.ACCOUNTS.name(), 5L);
        params.put(Extra.STANDARD_FUNGIBLE_TOKENS.name(), 1L);
        Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(15 +  (5-2)*2, fee.usd(), "Simple token transfer");
    }

    @Test
    void testMultipleTokenTransfer() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", CRYPTO_TRANSFER);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.ACCOUNTS.name(), 10L);
        params.put(Extra.STANDARD_FUNGIBLE_TOKENS.name(), 5L);

        Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(15 + ((10-2) * 2) + ((5-2) * 15), fee.usd(),"Multiple token transfers");
    }

    @Test
    void testMultipleHbarAndMultipleTokenTransfer() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", CRYPTO_TRANSFER);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(),  1L);
        params.put(Extra.ACCOUNTS.name(), 10L);
        params.put(Extra.STANDARD_FUNGIBLE_TOKENS.name(),  5L);

        Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(15  + (10-2)*2 + (5-2) * 15, fee.usd(),"Multiple hbar and token transfers");
    }

    @Test
    void testInvalidParamsFailCheck() {
        CryptoTransfer transfer = new CryptoTransfer("Crypto", CRYPTO_TRANSFER);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.ACCOUNTS.name(), 0L);
        params.put(Extra.STANDARD_FUNGIBLE_TOKENS.name(), 0L);
        params.put("numNFTNoCustomFeeEntries", 0);
        params.put("numFTWithCustomFeeEntries", 0);
        params.put("numNFTWithCustomFeeEntries", 0);
        params.put("numAutoAssociationsCreated", 0);
        params.put("numAutoAccountsCreated", 0);
        params.put(Extra.SIGNATURES.name(), 1L);

        FeeCheckResult result = transfer.checkParameters(params);
        assertFalse(result.result, "Expected parameters to fail due to minimum transfer entry rule");
    }
}

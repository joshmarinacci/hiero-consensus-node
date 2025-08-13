package com.hedera.node.app.hapi.simplefees.apis.token;

import com.hedera.node.app.hapi.simplefees.MockFeesSchedule;
import com.hedera.node.app.hapi.simplefees.apis.MockExchangeRate;
import com.hedera.node.app.hapi.simplefees.apis.common.FeeConstants;
import com.hedera.node.app.spi.fees.Fees;
import org.hiero.hapi.support.fees.Extra;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenWipeTest {

    @Test
    void testTokenWipeOne() {
        TokenWipe topic = new TokenWipe();
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put("numTokens", 1);
        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), new MockFeesSchedule());
        assertEquals(0.001, fee.usd(), "Token Wipe - 1");
    }

    @Test
    void testTokenWipeMultiple() {
        TokenWipe topic = new TokenWipe();
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put("numTokens", 5);

        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), new MockFeesSchedule());
        assertEquals(5 * 0.001, fee.usd(), "Token Wipe - multiple");
    }
}

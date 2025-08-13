package com.hedera.node.app.hapi.simplefees.apis.token;

import com.hedera.node.app.hapi.simplefees.MockFeesSchedule;
import com.hedera.node.app.hapi.simplefees.apis.common.AssociateOrDissociate;
import com.hedera.node.app.hapi.simplefees.apis.MockExchangeRate;
import com.hedera.node.app.spi.fees.Fees;
import org.hiero.hapi.support.fees.Extra;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenAssociateDissociateTest {
    static MockFeesSchedule schedule;

    @BeforeAll
    static void setup() {
        schedule = new MockFeesSchedule();
    }


    @Test
    void testTokenAssociateOne() {
        TokenAssociateDissociate topic = new TokenAssociateDissociate(AssociateOrDissociate.Associate);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.TOKEN_TYPES.name(), 1L);

        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0.05, fee.usd(), "Token associate");
    }

    @Test
    void testTokenAssociateMultiple() {
        TokenAssociateDissociate topic = new TokenAssociateDissociate(AssociateOrDissociate.Associate);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.TOKEN_TYPES.name(), 10L);

        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10 * 0.05, fee.usd(), "Token associate - 10");
    }

    @Test
    void testTokenDissociateOne() {
        TokenAssociateDissociate topic = new TokenAssociateDissociate(AssociateOrDissociate.Dissociate);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.TOKEN_TYPES.name(), 1L);

        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0.05, fee.usd(), "Token dissociate");
    }

    @Test
    void testTokenDissociateMultiple() {
        TokenAssociateDissociate topic = new TokenAssociateDissociate(AssociateOrDissociate.Dissociate);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.TOKEN_TYPES.name(), 10L);

        Fees fee = topic.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(10 * 0.05, fee.usd(), "Token dissociate - 10");
    }

}

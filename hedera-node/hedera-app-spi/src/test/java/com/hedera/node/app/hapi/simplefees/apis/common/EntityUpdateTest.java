package com.hedera.node.app.hapi.simplefees.apis.common;

import com.hedera.node.app.hapi.simplefees.MockFeesSchedule;
import com.hedera.node.app.hapi.simplefees.apis.MockExchangeRate;
import com.hedera.node.app.spi.fees.Fees;
import org.hiero.hapi.support.fees.Extra;
import org.hiero.hapi.support.fees.FeeSchedule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.hedera.hapi.node.base.HederaFunctionality.*;
import static com.hedera.node.app.hapi.simplefees.MockFeesSchedule.makeExtraDef;
import static com.hedera.node.app.hapi.simplefees.MockFeesSchedule.makeExtraIncluded;
import static com.hedera.node.app.hapi.simplefees.MockFeesSchedule.makeService;
import static com.hedera.node.app.hapi.simplefees.MockFeesSchedule.makeServiceFee;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityUpdateTest {
    static MockFeesSchedule schedule;

    @BeforeAll
    static void setup() {
        schedule = new MockFeesSchedule();
        final FeeSchedule raw = FeeSchedule.DEFAULT.copyBuilder().definedExtras(
                makeExtraDef(Extra.KEYS, 1),
                makeExtraDef(Extra.SIGNATURES, 1)
        ).serviceFees(
                        makeService("Crypto",
                                makeServiceFee(CRYPTO_UPDATE,22,
                                        makeExtraIncluded(Extra.KEYS,1))),
                        makeService("Token",
                                makeServiceFee(TOKEN_UPDATE,1,
                                        makeExtraIncluded(Extra.KEYS,7))),
                        makeService("Consenus",
                                makeServiceFee(CONSENSUS_UPDATE_TOPIC,22,
                                        makeExtraIncluded(Extra.KEYS,1))),
                        makeService("Contract",
                                makeServiceFee(CONTRACT_UPDATE,26,
                                        makeExtraIncluded(Extra.KEYS,1)))

                )
                .build();
        schedule.setRawSchedule(raw);
    }

    @Test
    void testEntityUpdateCryptoService() {
        EntityUpdate entity = new EntityUpdate("Crypto", CRYPTO_UPDATE, "Update an account");
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.toString(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(22 + 9 * 1, fee.usd(), "Crypto update");
    }

    @Test
    void testEntityUpdateTokenService() {
        EntityUpdate entity = new EntityUpdate("Token", TOKEN_UPDATE, "Update a token type");
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.toString(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(1 + 3 * 1, fee.usd(), "Token update");
    }

    @Test
    void testEntityUpdateTopicService() {
        EntityUpdate entity = new EntityUpdate("Topic",CONSENSUS_UPDATE_TOPIC, "Update a topic");
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.toString(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(22 + 9 * 1, fee.usd(), "Topic update");
    }

    @Test
    void testEntityUpdateContractService() {
        EntityUpdate entity = new EntityUpdate("Smart Contract",CONTRACT_UPDATE, "Update a smart contract");
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.toString(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(26 + 9 * 1, fee.usd(), "Smart contract update");
    }

}

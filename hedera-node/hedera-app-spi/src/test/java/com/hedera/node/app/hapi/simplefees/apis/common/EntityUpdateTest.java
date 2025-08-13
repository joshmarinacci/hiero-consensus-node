package com.hedera.node.app.hapi.simplefees.apis.common;

import com.hedera.node.app.hapi.simplefees.MockFeesSchedule;
import com.hedera.node.app.hapi.simplefees.apis.MockExchangeRate;
import com.hedera.node.app.spi.fees.Fees;
import org.hiero.hapi.support.fees.Extra;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityUpdateTest {
    static MockFeesSchedule schedule;

    @BeforeAll
    static void setup() {
        schedule = new MockFeesSchedule();
        schedule.setExtrasFee(Extra.KEYS,1L);

        schedule.setServiceBaseFee("CryptoUpdate",22L);
        schedule.setServiceExtraIncludedCount("CryptoUpdate", Extra.KEYS,1L);

        schedule.setServiceBaseFee("TokenUpdate",1L);
        schedule.setServiceExtraIncludedCount("TokenUpdate", Extra.KEYS,7L);

        schedule.setServiceBaseFee("ConsensusUpdateTopic",22L);
        schedule.setServiceExtraIncludedCount("ConsensusUpdateTopic", Extra.KEYS,1L);

        schedule.setServiceBaseFee("ContractUpdate",26L);
        schedule.setServiceExtraIncludedCount("ContractUpdate", Extra.KEYS,1L);
    }

    @Test
    void testEntityUpdateCryptoService() {
        EntityUpdate entity = new EntityUpdate("Crypto", "CryptoUpdate", "Update an account");
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.toString(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(22 + 9 * 1, fee.usd(), "Crypto update");
    }

    @Test
    void testEntityUpdateTokenService() {
        EntityUpdate entity = new EntityUpdate("Token", "TokenUpdate", "Update a token type");
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.toString(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(1 + 3 * 1, fee.usd(), "Token update");
    }

    @Test
    void testEntityUpdateTopicService() {
        EntityUpdate entity = new EntityUpdate("Topic", "ConsensusUpdateTopic", "Update a topic");
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.toString(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(22 + 9 * 1, fee.usd(), "Topic update");
    }

    @Test
    void testEntityUpdateContractService() {
        EntityUpdate entity = new EntityUpdate("Smart Contract", "ContractUpdate", "Update a smart contract");
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.toString(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(26 + 9 * 1, fee.usd(), "Smart contract update");
    }

}

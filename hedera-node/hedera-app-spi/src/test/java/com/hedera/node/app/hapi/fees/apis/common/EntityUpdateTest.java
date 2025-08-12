package com.hedera.node.app.hapi.fees.apis.common;

import com.hedera.node.app.hapi.fees.MockFeesSchedule;
import com.hedera.node.app.hapi.fees.apis.MockExchangeRate;
import com.hedera.node.app.hapi.fees.apis.common.FeeConstants.Extras;
import com.hedera.node.app.spi.fees.Fees;
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
        schedule.setExtrasFee(Extras.Keys,1L);

        schedule.setServiceBaseFee("CryptoUpdate",22L);
        schedule.setServiceExtraIncludedCount("CryptoUpdate", Extras.Keys,1L);

        schedule.setServiceBaseFee("TokenUpdate",1L);
        schedule.setServiceExtraIncludedCount("TokenUpdate", Extras.Keys,7L);

        schedule.setServiceBaseFee("ConsensusUpdateTopic",22L);
        schedule.setServiceExtraIncludedCount("ConsensusUpdateTopic", Extras.Keys,1L);

        schedule.setServiceBaseFee("ContractUpdate",26L);
        schedule.setServiceExtraIncludedCount("ContractUpdate", Extras.Keys,1L);
    }

    @Test
    void testEntityUpdateCryptoService() {
        EntityUpdate entity = new EntityUpdate("Crypto", "CryptoUpdate", "Update an account");
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.toString(), 1L);
        params.put(Extras.Keys.toString(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(22 + 9 * 1, fee.usd(), "Crypto update");
    }

    @Test
    void testEntityUpdateTokenService() {
        EntityUpdate entity = new EntityUpdate("Token", "TokenUpdate", "Update a token type");
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.toString(), 1L);
        params.put(Extras.Keys.toString(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(1 + 3 * 1, fee.usd(), "Token update");
    }

    @Test
    void testEntityUpdateTopicService() {
        EntityUpdate entity = new EntityUpdate("Topic", "ConsensusUpdateTopic", "Update a topic");
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.toString(), 1L);
        params.put(Extras.Keys.toString(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(22 + 9 * 1, fee.usd(), "Topic update");
    }

    @Test
    void testEntityUpdateContractService() {
        EntityUpdate entity = new EntityUpdate("Smart Contract", "ContractUpdate", "Update a smart contract");
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.toString(), 1L);
        params.put(Extras.Keys.toString(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(26 + 9 * 1, fee.usd(), "Smart contract update");
    }

}

package com.hedera.node.app.hapi.fees.apis.common;

import com.hedera.node.app.hapi.fees.AbstractFeesSchedule.Extras;
import com.hedera.node.app.hapi.fees.MockFeesSchedule;
import com.hedera.node.app.hapi.fees.apis.MockExchangeRate;
import com.hedera.node.app.spi.fees.Fees;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityCreateTest {

    static MockFeesSchedule schedule;

    @BeforeAll
    static void setup() {
        schedule = new MockFeesSchedule();
        schedule.setExtrasFee(Extras.Keys.toString(),1L);

        schedule.setServiceBaseFee("CryptoCreate",22L);
        schedule.setServiceExtraIncludedCount("CryptoCreate",Extras.Keys,2L);

        schedule.setServiceBaseFee("TokenCreate",33L);
        schedule.setServiceExtraIncludedCount("TokenCreate",Extras.Keys,7L);

        schedule.setServiceBaseFee("TokenCreateWithCustomFee",38L);
        schedule.setServiceExtraIncludedCount("TokenCreateWithCustomFee",Extras.Keys,7L);

        schedule.setServiceBaseFee("ConsensusCreateTopic",15L);
        schedule.setServiceExtraIncludedCount("ConsensusCreateTopic",Extras.Keys,1L);

        schedule.setServiceBaseFee("ContractCreate",15L);
        schedule.setServiceExtraIncludedCount("ContractCreate",Extras.Keys,1L);

        schedule.setServiceBaseFee("ScheduleCreate",15L);
        schedule.setServiceExtraIncludedCount("ScheduleCreate",Extras.Keys,1L);
    }
    @Test
    void testEntityCreateCryptoService() {
        EntityCreate entity = new EntityCreate("Crypto", "CryptoCreate", "Create an account", false);
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.toString(), 1L);
        params.put(Extras.Keys.toString(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(schedule.getServiceBaseFee("CryptoCreate") + 8 * schedule.getExtrasFee("Keys"), fee.usd(), "Crypto Create");
    }

    @Test
    void testEntityCreateCustomFeeCapableTokenServiceNoCustomFee() {
        EntityCreate entity = new EntityCreate("Token", "TokenCreate", "Create a token type", true);
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.toString(), 1L);
        params.put(Extras.Keys.toString(), 10L);
        params.put("hasCustomFee", YesOrNo.NO);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(schedule.getServiceBaseFee("TokenCreate") + 3 * schedule.getExtrasFee("Keys"), fee.usd(), "Token Create - no custom fee");
    }

    @Test
    void testEntityCreateCustomFeeCapableTokenServiceWithCustomFee() {
        EntityCreate entity = new EntityCreate("Token", "TokenCreate", "Create a token type", true);
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.toString(), 1L);
        params.put(Extras.Keys.toString(), 10L);
        params.put("hasCustomFee", YesOrNo.YES);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(schedule.getServiceBaseFee("TokenCreateWithCustomFee") + 3 * schedule.getExtrasFee("Keys"), fee.usd(), "Token Create - has custom fee");
    }

    @Test
    void testEntityCreateCustomFeeCapableTopicServiceNoCustomFee() {
        EntityCreate entity = new EntityCreate("Topic", "ConsensusCreateTopic", "Create a topic",  true);
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.toString(), 1L);
        params.put(Extras.Keys.toString(), 5L);
        params.put("hasCustomFee", YesOrNo.NO);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0 + schedule.getServiceBaseFee("ConsensusCreateTopic") + 4 * schedule.getExtrasFee("Keys"), fee.usd(), "Topic Create - no custom fee");
    }

    @Test
    void testEntityCreateCustomFeeCapableTopicServiceWithCustomFee() {
        EntityCreate entity = new EntityCreate("Topic", "ConsensusCreateTopic", "Create a topic",  true);
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.toString(), 1L);
        params.put(Extras.Keys.toString(), 5L);
        params.put("hasCustomFee", YesOrNo.YES);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0 + schedule.getServiceBaseFee("ConsensusCreateTopicWithCustomFee") + 4 * schedule.getExtrasFee("Keys"), fee.usd(), "Topic Create - with custom fee");
    }

    @Test
    void testEntityCreateContractService() {
        EntityCreate entity = new EntityCreate("Smart Contract", "ContractCreate", "Create a smart contract", false);
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.toString(), 1L);
        params.put(Extras.Keys.toString(), 5L);
        params.put("numSignatures", 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0 + schedule.getServiceBaseFee("ContractCreate") + 9 * schedule.getExtrasFee("Keys"), fee.usd(), "Contract Create");
    }

    @Test
    void testEntityCreateScheduleService() {
        EntityCreate entity = new EntityCreate("Miscellaneous", "ScheduleCreate", "Create a schedule", false);
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.toString(), 1L);
        params.put(Extras.Keys.toString(), 5L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0 + schedule.getServiceBaseFee("ScheduleCreate") + 4 * schedule.getExtrasFee("Keys"), fee.usd(), "Contract Create");
    }


}

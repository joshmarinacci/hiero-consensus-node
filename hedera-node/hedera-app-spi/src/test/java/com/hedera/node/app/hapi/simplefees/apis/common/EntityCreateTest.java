package com.hedera.node.app.hapi.simplefees.apis.common;

import com.hedera.node.app.hapi.simplefees.MockFeesSchedule;
import com.hedera.node.app.hapi.simplefees.apis.MockExchangeRate;
import com.hedera.node.app.hapi.simplefees.apis.common.FeeConstants.Params;
import com.hedera.node.app.spi.fees.Fees;
import org.hiero.hapi.support.fees.Extra;
import org.hiero.hapi.support.fees.ExtraFeeDefinition;
import org.hiero.hapi.support.fees.ExtraFeeReference;
import org.hiero.hapi.support.fees.FeeSchedule;
import org.hiero.hapi.support.fees.Service;
import org.hiero.hapi.support.fees.ServiceFee;
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
        final FeeSchedule raw = FeeSchedule.DEFAULT.copyBuilder().definedExtras(
                    ExtraFeeDefinition.newBuilder().name(Extra.KEYS).fee(1).build()
                )
                .serviceFees(Service.DEFAULT.copyBuilder()
                        .name("Crypto")
                            .transactions(
                                   ServiceFee.DEFAULT.copyBuilder()
                                           .name("CryptoCreate").baseFee(22)
                                           .extras(
                                                   ExtraFeeReference.DEFAULT.copyBuilder()
                                                           .name(Extra.KEYS).includedCount(2).build()
                                           ).build(),
                                    ServiceFee.DEFAULT.copyBuilder()
                                            .name("TokenCreate").baseFee(33)
                                            .extras(
                                                    ExtraFeeReference.DEFAULT.copyBuilder()
                                                            .name(Extra.KEYS).includedCount(7).build()
                                            ).build()

                            )
                        .build())
                .build();
        schedule.setRawSchedule(raw);

        schedule.setServiceBaseFee("TokenCreateWithCustomFee",38L);
        schedule.setServiceExtraIncludedCount("TokenCreateWithCustomFee", Extra.KEYS,7L);

        schedule.setServiceBaseFee("ConsensusCreateTopic",15L);
        schedule.setServiceExtraIncludedCount("ConsensusCreateTopic", Extra.KEYS,1L);

        schedule.setServiceBaseFee("ConsensusCreateTopicWithCustomFee",30L);
        schedule.setServiceExtraIncludedCount("ConsensusCreateTopicWithCustomFee", Extra.KEYS,1L);


        schedule.setServiceBaseFee("ContractCreate",15L);
        schedule.setServiceExtraIncludedCount("ContractCreate", Extra.KEYS,1L);

        schedule.setServiceBaseFee("ScheduleCreate",15L);
        schedule.setServiceExtraIncludedCount("ScheduleCreate", Extra.KEYS,1L);
    }
    @Test
    void testEntityCreateCryptoService() {
        EntityCreate entity = new EntityCreate("Crypto", "CryptoCreate", "Create an account", false);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.name(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(schedule.getServiceBaseFee("CryptoCreate") + 8 * schedule.getExtrasFee(Extra.KEYS), fee.usd(), "Crypto Create");
    }

    @Test
    void testEntityCreateCustomFeeCapableTokenServiceNoCustomFee() {
        EntityCreate entity = new EntityCreate("Token", "TokenCreate", "Create a token type", true);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.name(), 10L);
        params.put(Params.HasCustomFee.name(), YesOrNo.NO);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(schedule.getServiceBaseFee("TokenCreate") + 3 * schedule.getExtrasFee(Extra.KEYS), fee.usd(), "Token Create - no custom fee");
    }

    @Test
    void testEntityCreateCustomFeeCapableTokenServiceWithCustomFee() {
        EntityCreate entity = new EntityCreate("Token", "TokenCreate", "Create a token type", true);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.name(), 10L);
        params.put(Params.HasCustomFee.name(), YesOrNo.YES);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(schedule.getServiceBaseFee("TokenCreateWithCustomFee") + 3 * schedule.getExtrasFee(Extra.KEYS), fee.usd(), "Token Create - has custom fee");
    }

    @Test
    void testEntityCreateCustomFeeCapableTopicServiceNoCustomFee() {
        EntityCreate entity = new EntityCreate("Topic", "ConsensusCreateTopic", "Create a topic",  true);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.name(), 5L);
        params.put(Params.HasCustomFee.name(), YesOrNo.NO);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0 + schedule.getServiceBaseFee("ConsensusCreateTopic") + 4 * schedule.getExtrasFee(Extra.KEYS), fee.usd(), "Topic Create - no custom fee");
    }

    @Test
    void testEntityCreateCustomFeeCapableTopicServiceWithCustomFee() {
        EntityCreate entity = new EntityCreate("Topic", "ConsensusCreateTopic", "Create a topic",  true);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.name(), 5L);
        params.put(Params.HasCustomFee.name(), YesOrNo.YES);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0 + schedule.getServiceBaseFee("ConsensusCreateTopicWithCustomFee") + 4 * schedule.getExtrasFee(Extra.KEYS), fee.usd(), "Topic Create - with custom fee");
    }

    @Test
    void testEntityCreateContractService() {
        EntityCreate entity = new EntityCreate("Smart Contract", "ContractCreate", "Create a smart contract", false);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.name(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0 + schedule.getServiceBaseFee("ContractCreate") + 9 * schedule.getExtrasFee(Extra.KEYS), fee.usd(), "Contract Create");
    }

    @Test
    void testEntityCreateScheduleService() {
        EntityCreate entity = new EntityCreate("Miscellaneous", "ScheduleCreate", "Create a schedule", false);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.name(), 5L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0 + schedule.getServiceBaseFee("ScheduleCreate") + 4 * schedule.getExtrasFee(Extra.KEYS), fee.usd(), "Contract Create");
    }


}

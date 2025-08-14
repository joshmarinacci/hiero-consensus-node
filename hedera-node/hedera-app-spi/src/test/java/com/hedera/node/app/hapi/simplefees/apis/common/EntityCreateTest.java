package com.hedera.node.app.hapi.simplefees.apis.common;

import com.hedera.hapi.node.base.HederaFunctionality;
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

import static com.hedera.hapi.node.base.HederaFunctionality.*;
import static com.hedera.node.app.hapi.simplefees.MockFeesSchedule.makeExtraDef;
import static com.hedera.node.app.hapi.simplefees.MockFeesSchedule.makeExtraIncluded;
import static com.hedera.node.app.hapi.simplefees.MockFeesSchedule.makeService;
import static com.hedera.node.app.hapi.simplefees.MockFeesSchedule.makeServiceFee;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityCreateTest {

    static MockFeesSchedule schedule;

    @BeforeAll
    static void setup() {
        schedule = new MockFeesSchedule();
        final FeeSchedule raw = FeeSchedule.DEFAULT.copyBuilder().definedExtras(
                        makeExtraDef(Extra.KEYS, 1),
                        makeExtraDef(Extra.SIGNATURES, 1)
                )
                .serviceFees(
                        makeService("Crypto",
                                makeServiceFee(CRYPTO_CREATE, 22,
                                        makeExtraIncluded(Extra.KEYS, 2)
                                ),
                                makeServiceFee(TOKEN_CREATE, 33,
                                        makeExtraIncluded(Extra.KEYS, 7)
                                )),
                        makeService("Token",
                                makeServiceFee(NONE,38,// "TokenCreateWithCustomFee",38,
                                        makeExtraIncluded(Extra.KEYS, 7)
                                )
                        ),
                        makeService("Consensus",
                                makeServiceFee(CONSENSUS_CREATE_TOPIC,15,
                                        makeExtraIncluded(Extra.KEYS, 1)
                                ),
                                makeServiceFee(NONE, 30, //"ConsensusCreateTopicWithCustomFee",30,
                                        makeExtraIncluded(Extra.KEYS, 1)
                                )
                        ),
                        makeService("Contract",
                                makeServiceFee(CONTRACT_CREATE,15,
                                        makeExtraIncluded(Extra.KEYS, 1)
                                        )
                                ),
                        makeService("Schedule",
                                makeServiceFee(SCHEDULE_CREATE,15,
                                        makeExtraIncluded(Extra.KEYS, 1)
                                        )
                                )
                )
                .build();
        schedule.setRawSchedule(raw);
    }

    @Test
    void testEntityCreateCryptoService() {
        EntityCreate entity = new EntityCreate("Crypto", "CryptoCreate", "Create an account", false);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.name(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(schedule.getServiceBaseFee(CRYPTO_CREATE) + 8 * schedule.getExtrasFee(Extra.KEYS), fee.usd(), "Crypto Create");
    }

    @Test
    void testEntityCreateCustomFeeCapableTokenServiceNoCustomFee() {
        EntityCreate entity = new EntityCreate("Token", "TokenCreate", "Create a token type", true);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.name(), 10L);
        params.put(Params.HasCustomFee.name(), YesOrNo.NO);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(schedule.getServiceBaseFee(TOKEN_CREATE) + 3 * schedule.getExtrasFee(Extra.KEYS), fee.usd(), "Token Create - no custom fee");
    }

    @Test
    void testEntityCreateCustomFeeCapableTokenServiceWithCustomFee() {
        EntityCreate entity = new EntityCreate("Token", "TokenCreate", "Create a token type", true);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.name(), 10L);
        params.put(Params.HasCustomFee.name(), YesOrNo.YES);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
//        assertEquals(schedule.getServiceBaseFee("TokenCreateWithCustomFee") + 3 * schedule.getExtrasFee(Extra.KEYS), fee.usd(), "Token Create - has custom fee");
    }

    @Test
    void testEntityCreateCustomFeeCapableTopicServiceNoCustomFee() {
        EntityCreate entity = new EntityCreate("Topic", "ConsensusCreateTopic", "Create a topic", true);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.name(), 5L);
        params.put(Params.HasCustomFee.name(), YesOrNo.NO);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0 + schedule.getServiceBaseFee(CONSENSUS_CREATE_TOPIC) + 4 * schedule.getExtrasFee(Extra.KEYS), fee.usd(), "Topic Create - no custom fee");
    }

    @Test
    void testEntityCreateCustomFeeCapableTopicServiceWithCustomFee() {
        EntityCreate entity = new EntityCreate("Topic", "ConsensusCreateTopic", "Create a topic", true);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.name(), 5L);
        params.put(Params.HasCustomFee.name(), YesOrNo.YES);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
//        assertEquals(0 + schedule.getServiceBaseFee("ConsensusCreateTopicWithCustomFee") + 4 * schedule.getExtrasFee(Extra.KEYS), fee.usd(), "Topic Create - with custom fee");
    }

    @Test
    void testEntityCreateContractService() {
        EntityCreate entity = new EntityCreate("Smart Contract", "ContractCreate", "Create a smart contract", false);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.name(), 10L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0 + schedule.getServiceBaseFee(CONTRACT_CREATE) + 9 * schedule.getExtrasFee(Extra.KEYS), fee.usd(), "Contract Create");
    }

    @Test
    void testEntityCreateScheduleService() {
        EntityCreate entity = new EntityCreate("Miscellaneous", "ScheduleCreate", "Create a schedule", false);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.name(), 5L);

        Fees fee = entity.computeFee(params, new MockExchangeRate().activeRate(), schedule);
        assertEquals(0 + schedule.getServiceBaseFee(SCHEDULE_CREATE) + 4 * schedule.getExtrasFee(Extra.KEYS), fee.usd(), "Contract Create");
    }


}

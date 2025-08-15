package org.hiero.hapi.fees.apis.common;

import org.hiero.hapi.fees.FeeModel;
import org.hiero.hapi.fees.FeeModelRegistry;
import org.hiero.hapi.fees.FeeResult;
import org.hiero.hapi.support.fees.Extra;
import org.hiero.hapi.support.fees.FeeSchedule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.util.HashMap;
import java.util.Map;

import static com.hedera.hapi.node.base.HederaFunctionality.CONSENSUS_CREATE_TOPIC;
import static org.hiero.hapi.fees.FeeScheduleUtils.makeExtraDef;
import static org.hiero.hapi.fees.FeeScheduleUtils.makeExtraIncluded;
import static org.hiero.hapi.fees.FeeScheduleUtils.makeService;
import static org.hiero.hapi.fees.FeeScheduleUtils.makeServiceFee;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityCreateTest {
    static FeeSchedule feeSchedule;
    @BeforeAll
    static void setup() {
        feeSchedule = FeeSchedule.DEFAULT.copyBuilder()
                .definedExtras(
                        makeExtraDef(Extra.KEYS,2),
                        makeExtraDef(Extra.SIGNATURES,3)
                )
                .serviceFees(
                        makeService("Consensus",
                                makeServiceFee(CONSENSUS_CREATE_TOPIC,15,
                                        makeExtraIncluded(Extra.KEYS, 1)
                                )
                        )
                )
                .build();
    }
    @Test
    void createTopicWithExtraKeys() {
        FeeModel model = FeeModelRegistry.registry.get(CONSENSUS_CREATE_TOPIC);
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.name(), 5L);
//        params.put(Params.HasCustomFee.name(), YesOrNo.NO);

        FeeResult fee = model.computeFee(params, new MockExchangeRate().activeRate(), feeSchedule);
        System.out.println(fee);
        assertEquals(
                15 + (1*3)+ (5-1)*2, fee.total(), "Topic Create - no custom fee");
    }

    @Test
    void createTopicWithCustomFee() {

    }
}

package com.hedera.node.app.hapi.fees.apis.schedule;

import com.hedera.hapi.node.consensus.ServiceMethod;
import com.hedera.node.app.hapi.fees.JsonFeesSchedule;
import com.hedera.node.app.hapi.fees.MockFeesSchedule;
import com.hedera.node.app.hapi.fees.apis.MockExchangeRate;
import com.hedera.node.app.hapi.fees.apis.common.EntityCreate;
import com.hedera.node.app.hapi.fees.apis.common.YesOrNo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.hedera.node.app.hapi.fees.apis.common.FeesHelper.createModel;
import static org.junit.jupiter.api.Assertions.*;

public class FeeScheduleTest {
    @Test
    void testLoadingFeeScheduleFromJson() {
        assertDoesNotThrow(() -> {
            final var feeSchedule = JsonFeesSchedule.fromJson();
            assertEquals(feeSchedule.getNodeBaseFee("ConsensusCreateTopic"),0.01000);
            assertEquals(feeSchedule.getNetworkBaseFee("ConsensusCreateTopic"),0.02000);

            // should be 4 extras
            assertEquals(feeSchedule.schedule.extras().size(),4);
            // check that sig verifications is there
            assertNotNull(feeSchedule.extras.get("SignatureVerification"));
            assertNull(feeSchedule.extras.get("MadeUpFieldName"));
        });
    }
    @Test
    void testMissingJsonFields() {
        assertDoesNotThrow(() -> {
            final var feeSchedule = JsonFeesSchedule.fromJson();
            for (var service : feeSchedule.schedule.services()) {
                var methods = new ArrayList<ServiceMethod>();
                methods.addAll(service.transactions());
                methods.addAll(service.queries());
                for (var method : methods) {
                    assertTrue(method.hasNode(),method.name() + ".node is missing");
                    assertTrue(method.hasNetwork(),method.name() + ".network is missing");
                    assertDoesNotThrow(() -> Double.parseDouble(method.node().base()),method.name()+".node.base cannot be parsed as a double");
                    assertDoesNotThrow(() -> Double.parseDouble(method.network().base()),method.name()+".network.base cannot be parsed as a double");
                }
            }
        });
    }

    @Test
    //test that we can load the mock fees and use them
    void mockTest() {
        var schedule = new MockFeesSchedule();
        schedule.setNetworkBaseFee("ConsensusCreateTopic",8.8);
        assertEquals(schedule.getNetworkBaseFee("ConsensusCreateTopic"),8.8);
    }

    @Test
    //test that we can create a fees model from the service name
    void createModelFromNames() {
        var service = "Consensus";
        var method = "ConsensusCreateTopic";
        var schedule = new MockFeesSchedule();
        schedule.setNetworkBaseFee("ConsensusCreateTopic",8.8);
        schedule.setNodeBaseFee("ConsensusCreateTopic",9.9);

        var model = createModel(service,method);
        assertInstanceOf(EntityCreate.class, model);

        Map<String, Object> params = new HashMap<>();
        params.put("numSignatures", 0);
        params.put("numKeys", 0);
        params.put("hasCustomFee", YesOrNo.NO);
        model.checkParameters(params);
        var fees = model.computeFee2(params,new MockExchangeRate().activeRate(),schedule);
        assertTrue(Math.abs(fees.usd()-(8.8+9.9))<0.1);
    }
}

package com.hedera.node.app.hapi.simplefees.apis.schedule;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.node.app.hapi.simplefees.JsonFeesSchedule;
import com.hedera.node.app.hapi.simplefees.MockFeesSchedule;
import com.hedera.node.app.hapi.simplefees.apis.MockExchangeRate;
import com.hedera.node.app.hapi.simplefees.apis.common.EntityCreate;
import com.hedera.node.app.hapi.simplefees.apis.common.FeeConstants.Params;
import com.hedera.node.app.hapi.simplefees.apis.common.YesOrNo;
import com.hedera.node.app.hapi.simplefees.apis.consensus.HCSSubmit;
import org.hiero.hapi.support.fees.Extra;
import org.hiero.hapi.support.fees.FeeSchedule;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.hedera.hapi.node.base.HederaFunctionality.*;
import static com.hedera.node.app.hapi.simplefees.FeeModelRegistry.createModel;
import static com.hedera.node.app.hapi.simplefees.MockFeesSchedule.makeExtraDef;
import static org.hiero.hapi.support.fees.Extra.*;
import static org.junit.jupiter.api.Assertions.*;

public class FeeScheduleTest {
    @Test
    void testLoadingFeeScheduleFromJson() {
        assertDoesNotThrow(() -> {
            final var feeSchedule = JsonFeesSchedule.fromJson();
            assertEquals(1,feeSchedule.getNodeBaseFee());
            assertEquals(1,feeSchedule.getNodeExtraIncludedCount(SIGNATURES));
            Extra[] nodeExtras = {SIGNATURES};
            assertArrayEquals(nodeExtras,feeSchedule.getNodeExtraNames().toArray(new Extra[0]));
//            String[] definedExtras = {
//                    Extra.SIGNATURES.toString(),
//                    Extra.BYTES.toString(),
//                    Extra.KEYS.toString(),
//                    Extras.TokenTypes.toString(),
//            };
//            assertArrayEquals(definedExtras, feeSchedule.getDefinedExtraNames().toArray(new String[0]));
//            assertEquals(feeSchedule.getServiceBaseFee("ConsensusCreateTopic"),2);

            // should be 4 extras
//            assertEquals(feeSchedule.getNodeExtraNames().size(),2);
//            // check that sig verifications is there
//            assertNotNull(feeSchedule.extras.get("SignatureVerification"));
//            assertNull(feeSchedule.extras.get("MadeUpFieldName"));
        });
    }
    @Test
    void validateJSON() {
        assertDoesNotThrow(() -> {
            final var feeSchedule = JsonFeesSchedule.fromJson();
            // check that all extras in the enum are in the actual json
            var definedExtras = Arrays.stream(Extra.values()).toArray(Extra[]::new);
            for (Extra extra : definedExtras) {
                assertDoesNotThrow(() -> feeSchedule.getExtrasFee(extra));
            }
            // check that there are no extra Extras in the JSON
            for (Extra name : feeSchedule.getDefinedExtraNames()) {
//                assertDoesNotThrow(() -> Extras.valueOf(name));
            }

            // check that all extras referenced in services are in the actual json
            for (HederaFunctionality methodName : feeSchedule.getServiceNames()) {
                for (Extra extraName : feeSchedule.getServiceExtras(methodName)) {
                    assertDoesNotThrow(() -> feeSchedule.getServiceExtraIncludedCount(methodName, extraName));
                    assertDoesNotThrow(() -> feeSchedule.getExtrasFee(extraName));
                }
            }
        });
    }
//    @Test
//    void testMissingJsonFields() {
//        assertDoesNotThrow(() -> {
//            final var feeSchedule = JsonFeesSchedule.fromJson();
//            for (var service : feeSchedule.schedule.services()) {
//                var methods = new ArrayList<ServiceMethod>();
//                methods.addAll(service.transactions());
//                methods.addAll(service.queries());
//                for (var method : methods) {
//                    assertTrue(method.hasNode(),method.name() + ".node is missing");
//                    assertTrue(method.hasNetwork(),method.name() + ".network is missing");
//                    assertDoesNotThrow(() -> Double.parseDouble(method.node().base()),method.name()+".node.base cannot be parsed as a double");
//                    assertDoesNotThrow(() -> Double.parseDouble(method.network().base()),method.name()+".network.base cannot be parsed as a double");
//                }
//            }
//        });
//    }

    @Test
    //test that we can load the mock fees and use them
    void mockTest() {
        var schedule = new MockFeesSchedule();
        schedule.setExtrasFee(SIGNATURES,8);
        assertEquals(schedule.getExtrasFee(SIGNATURES),8);
    }

    @Test
    //test that we can create a fees model from the service name
    void createModelFromStrings() {
        var schedule = new MockFeesSchedule();
        FeeSchedule raw  = FeeSchedule.DEFAULT.copyBuilder().definedExtras(
                makeExtraDef(SIGNATURES,6)
        ).build();
        schedule.setRawSchedule(raw);
//        schedule.setExtrasFee(SIGNATURES,6);
        schedule.setNodeBaseFee(2);
        schedule.setNodeExtraIncludedCount(SIGNATURES,2L);
        schedule.setNetworkMultiplier(3);
        schedule.setServiceBaseFee(CONSENSUS_CREATE_TOPIC,10L);
        schedule.setServiceExtraIncludedCount(CONSENSUS_CREATE_TOPIC, SIGNATURES,1L);

        var model = createModel("Consensus","ConsensusCreateTopic");
        assertInstanceOf(EntityCreate.class, model);

        Map<String, Object> params = new HashMap<>();
        {
            params.put(SIGNATURES.name(), 0L);
//        model.checkParameters(params);
            var fees = model.computeFee(params, new MockExchangeRate().activeRate(), schedule);
            // zero sigs, so just method base fee + node base fee + network multiplier * node base fee
            // 2 + 2*3 + 10 = 18
            assertEquals(18,fees.nodeFee() + fees.networkFee() + fees.serviceFee());
        }
        {
            // now set the sigs to 3
            params.put(SIGNATURES.name(), 3L);
            // 6 for each sig, with 2 included for the node and 1 included for the service
            // node = 2 + (3-2)*6 = 8
            // network = 3 * 8 = 24
            // service = 10 + (3-1)*6 = 22
            // total = 8 + 24 + 22 = 54
            var fees = model.computeFee(params, new MockExchangeRate().activeRate(), schedule);
            // zero sigs, so just method base fee + node base fee + network multiplier * node base fee
            // 2 + 2*3 + 10 = 18
            assertEquals(54, fees.nodeFee() + fees.networkFee() + fees.serviceFee());
        }
    }

    @Test
    // account for differences in included signature verifications
    void createModelWithVaryingIncludedSignatures() {
        var schedule = new MockFeesSchedule();
        schedule.setExtrasFee(SIGNATURES,1);
        schedule.setNodeBaseFee(2);
//        schedule.setNetworkBaseFee("ConsensusCreateTopic",8.8);
        schedule.setNodeExtraIncludedCount(SIGNATURES,1L);
//        schedule.setNetworkExtrasIncluded("ConsensusCreateTopic","SignatureVerifications",1);
        schedule.setNetworkMultiplier(3);
        schedule.setServiceBaseFee(CONSENSUS_CREATE_TOPIC,10L);
        schedule.setServiceExtraIncludedCount(CONSENSUS_CREATE_TOPIC, SIGNATURES,2);

        var model = createModel("Consensus","ConsensusCreateTopic");
        assertInstanceOf(EntityCreate.class, model);
        Map<String, Object> params = new HashMap<>();
        params.put(SIGNATURES.toString(), 8L);
        params.put(KEYS.toString(), 0);
        params.put(Params.HasCustomFee.name(), YesOrNo.NO);

                /*
         fee should be
            extras cost is 1 per sig
            node base is 2 + 1 included sig, so 2 + (8-1)*1 = 9
            network is node * 3, 9*3, 27
            service is base 10 + 1 included sig, so 10 + (8-2)*1 = 16
            total is 9 + 27 + 16
         */
        long correct =9 + 27 + 16;
        var exchangeRate = new MockExchangeRate().activeRate(); // 1/12

        var fees = model.computeFee(params, exchangeRate, schedule);
        assertEquals(correct, fees.nodeFee() + fees.networkFee() + fees.serviceFee());
    }

    @Test
    void testTopicSubmit() {
        // submit a message with 1600 bytes and 1 sig
        var schedule = new MockFeesSchedule();
        schedule.setServiceBaseFee(CONSENSUS_SUBMIT_MESSAGE,10L);
        schedule.setServiceExtraIncludedCount(CONSENSUS_SUBMIT_MESSAGE, BYTES,0);
        schedule.setExtrasFee(SIGNATURES,1L);
        schedule.setExtrasFee(BYTES,1L);
        var exchangeRate = new MockExchangeRate().activeRate(); // 1/12
        Map<String, Object> params = new HashMap<>();
        params.put(Params.HasCustomFee.name(), YesOrNo.NO);
        params.put(BYTES.toString(), 1600L);
        params.put(SIGNATURES.toString(), 1L);
        var model = createModel("Consensus","ConsensusSubmitMessage");
        model.checkParameters(params);
        var fees = model.computeFee(params, exchangeRate, schedule);
        long correct = 10 + 1600;
        assertEquals(correct, fees.nodeFee() + fees.networkFee() + fees.serviceFee());
    }

    @Test
    void ensureCorrectModelsAreCreated() {
        assertInstanceOf(EntityCreate.class, createModel("Consensus","ConsensusCreateTopic"));
        assertInstanceOf(HCSSubmit.class, createModel("Consensus","ConsensusSubmitMessage"));
        assertInstanceOf(HCSSubmit.class, createModel("Consensus","ConsensusSubmitMessageWithCustomFee"));
    }
//
//    @Test
//    void testGenericFeeCompute() {
//        var schedule = new MockFeesSchedule();
//        schedule.setExtrasFee("SignatureVerifications",1);
//        schedule.setNetworkBaseFee("ConsensusCreateTopic",8.8);
//        schedule.setNetworkExtrasIncluded("ConsensusCreateTopic","SignatureVerifications",1);
//        schedule.setNodeBaseFee("ConsensusCreateTopic",9.9);
//        schedule.setNodeExtrasIncluded("ConsensusCreateTopic","SignatureVerifications",2);
//        var correct = 8.8 + 9.9 + 1*7.0 + 1*6.0;
//
//        Map<String, Object> params = new HashMap<>();
//        params.put("numSignatures", 8);
//        params.put("numKeys", 0);
//        params.put("hasCustomFee", YesOrNo.NO);
//
//        var rate = new MockExchangeRate().activeRate();
//
//        var fees = genericComputeFee("Consensus","ConsensusCreateTopic",params,rate,schedule);
//        assertTrue(Math.abs(fees.usd()-correct)<0.1);
//    }
}

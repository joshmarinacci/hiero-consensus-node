package com.hedera.node.app.hapi.fees.apis.schedule;

import com.hedera.node.app.hapi.fees.AbstractFeesSchedule.Extras;
import com.hedera.node.app.hapi.fees.JsonFeesSchedule;
import com.hedera.node.app.hapi.fees.MockFeesSchedule;
import com.hedera.node.app.hapi.fees.apis.MockExchangeRate;
import com.hedera.node.app.hapi.fees.apis.common.EntityCreate;
import com.hedera.node.app.hapi.fees.apis.common.YesOrNo;
import com.hedera.node.app.hapi.fees.apis.consensus.HCSSubmit;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.hedera.node.app.hapi.fees.AbstractFeesSchedule.SIGNATURES;
import static com.hedera.node.app.hapi.fees.apis.common.FeesHelper.createModel;
import static org.junit.jupiter.api.Assertions.*;

public class FeeScheduleTest {
    @Test
    void testLoadingFeeScheduleFromJson() {
        assertDoesNotThrow(() -> {
            final var feeSchedule = JsonFeesSchedule.fromJson();
            assertEquals(1,feeSchedule.getNodeBaseFee());
            assertEquals(1,feeSchedule.getNodeExtraIncludedCount(Extras.Signatures.toString()));
            String[] nodeExtras = {Extras.Signatures.toString()};
            assertArrayEquals(nodeExtras,feeSchedule.getNodeExtraNames().toArray(new String[0]));
            String[] definedExtras = {
                    Extras.Signatures.toString(),
                    Extras.Bytes.toString(),
                    Extras.Keys.toString(),
                    Extras.TokenTypes.toString(),
            };
            assertArrayEquals(definedExtras, feeSchedule.getDefinedExtraNames().toArray(new String[0]));
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
            String[] definedExtras = Arrays.stream(Extras.values()).map(e -> e.name()).toArray(String[]::new);
            assertArrayEquals(definedExtras, feeSchedule.getDefinedExtraNames().toArray(new String[0]));
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
        schedule.setExtrasFee(Extras.Signatures,8);
        assertEquals(schedule.getExtrasFee(Extras.Signatures.toString()),8);
    }

    @Test
    //test that we can create a fees model from the service name
    void createModelFromStrings() {
        var schedule = new MockFeesSchedule();
        schedule.setExtrasFee(Extras.Signatures,6);
        schedule.setNodeBaseFee(2);
        schedule.setNodeExtraIncludedCount(Extras.Signatures.toString(),2L);
        schedule.setNetworkMultiplier(3);
        schedule.setServiceBaseFee("ConsensusCreateTopic",10L);
        schedule.setServiceExtraIncludedCount("ConsensusCreateTopic",SIGNATURES,1L);

        var model = createModel("Consensus","ConsensusCreateTopic");
        assertInstanceOf(EntityCreate.class, model);

        Map<String, Object> params = new HashMap<>();
        {
            params.put(SIGNATURES, 0L);
//        model.checkParameters(params);
            var fees = model.computeFee(params, new MockExchangeRate().activeRate(), schedule);
            // zero sigs, so just method base fee + node base fee + network multiplier * node base fee
            // 2 + 2*3 + 10 = 18
            assertEquals(18,fees.nodeFee() + fees.networkFee() + fees.serviceFee());
        }
        {
            // now set the sigs to 3
            params.put(SIGNATURES, 3L);
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
        schedule.setExtrasFee(Extras.Signatures,1);
        schedule.setNodeBaseFee(2);
//        schedule.setNetworkBaseFee("ConsensusCreateTopic",8.8);
        schedule.setNodeExtraIncludedCount(Extras.Signatures.toString(),1L);
//        schedule.setNetworkExtrasIncluded("ConsensusCreateTopic","SignatureVerifications",1);
        schedule.setNetworkMultiplier(3);
        schedule.setServiceBaseFee("ConsensusCreateTopic",10L);
        schedule.setServiceExtraIncludedCount("ConsensusCreateTopic",Extras.Signatures.toString(),2);

        var model = createModel("Consensus","ConsensusCreateTopic");
        assertInstanceOf(EntityCreate.class, model);
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.toString(), 8L);
        params.put(Extras.Keys.toString(), 0);
        params.put("hasCustomFee", YesOrNo.NO);

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
        schedule.setServiceBaseFee("ConsensusSubmitMessage",10L);
        schedule.setServiceExtraIncludedCount("ConsensusSubmitMessage",Extras.Bytes,0);
        schedule.setExtrasFee(Extras.Signatures,1L);
        schedule.setExtrasFee(Extras.Bytes,1L);
        var exchangeRate = new MockExchangeRate().activeRate(); // 1/12
        Map<String, Object> params = new HashMap<>();
        params.put("hasCustomFee", YesOrNo.NO);
        params.put(Extras.Bytes.toString(), 1600L);
        params.put(Extras.Signatures.toString(), 1L);
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

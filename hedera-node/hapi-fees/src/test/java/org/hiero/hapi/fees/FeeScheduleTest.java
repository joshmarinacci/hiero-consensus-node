// SPDX-License-Identifier: Apache-2.0
package org.hiero.hapi.fees;

import static org.hiero.hapi.fees.FeeScheduleUtils.makeExtraDef;
import static org.hiero.hapi.fees.FeeScheduleUtils.makeExtraIncluded;
import static org.hiero.hapi.fees.FeeScheduleUtils.makeService;
import static org.hiero.hapi.fees.FeeScheduleUtils.makeServiceFee;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.stream.ReadableStreamingData;
import java.util.Objects;
import org.hiero.hapi.support.fees.Extra;
import org.hiero.hapi.support.fees.FeeSchedule;
import org.hiero.hapi.support.fees.NodeFee;
import org.hiero.hapi.support.fees.ServiceFeeDefinition;
import org.hiero.hapi.support.fees.UnreadableTransactionFee;
import org.junit.jupiter.api.Test;

public class FeeScheduleTest {

    @Test
    void testLoadingFeeScheduleFromJson() throws ParseException {
        final var fin = FeeScheduleTest.class.getClassLoader().getResourceAsStream("simple-fee-schedule.json");
        final FeeSchedule buf = FeeSchedule.JSON.parse(new ReadableStreamingData(Objects.requireNonNull(fin)));
        assertTrue(FeeScheduleUtils.validate(buf), "Fee schedule validation failed");
    }

    // extra definitions must have positive values
    @Test
    void catchNegativeValue() {
        FeeSchedule badSchedule = FeeSchedule.DEFAULT
                .copyBuilder()
                .extras(makeExtraDef(Extra.KEYS, -88))
                .build();
        assertFalse(FeeScheduleUtils.validate(badSchedule), "Fee schedule validation didn't catch negative value");
    }

    @Test
    void checkAllExtrasDefined() {
        // should fail because only one of the extras in the Enum is defined
        FeeSchedule missingExtras = FeeSchedule.DEFAULT
                .copyBuilder()
                .extras(
                        makeExtraDef(Extra.SIGNATURES, 1)
                        ,makeExtraDef(Extra.KEYS, 1)
                )
                .build();
        assertFalse(FeeScheduleUtils.validateAllExtrasDefined(missingExtras), "Fee schedule validation failed");

        FeeSchedule zeroFee = FeeSchedule.DEFAULT
                .copyBuilder()
                .extras(
                        makeExtraDef(Extra.SIGNATURES, 0)
                )
                .build();
        assertFalse(FeeScheduleUtils.validateAllExtrasDefined(zeroFee), "Fee schedule validation failed");
    }

    // all referenced extras must exist in the defined extras
    @Test
    void catchMissingExtraDef() {
        FeeSchedule badSchedule = FeeSchedule.DEFAULT
                .copyBuilder()
                .extras(makeExtraDef(Extra.KEYS, 1))
                .services(makeService(
                        "Consensus",
                        makeServiceFee(
                                HederaFunctionality.CONSENSUS_CREATE_TOPIC,
                                22,
                                makeExtraIncluded(Extra.KEYS, 1),
                                makeExtraIncluded(Extra.SIGNATURES, 1))))
                .build();
        assertFalse(
                FeeScheduleUtils.validate(badSchedule),
                "Fee schedule validation failed to find the missing extras def");
    }

    @Test
    void catchMissingNodeNetworkFees() {
        FeeSchedule missingNode = FeeSchedule.DEFAULT
                .copyBuilder()
                .build();
        assertFalse(FeeScheduleUtils.validateNodeFee(missingNode));

        FeeSchedule missingMultiplier = FeeSchedule.DEFAULT
                .copyBuilder()
                .node(NodeFee.DEFAULT.copyBuilder().baseFee(55).build())
                .build();
        assertFalse(FeeScheduleUtils.validateNodeFee(missingMultiplier));
    }

    @Test
    void catchDuplicateServices() {
        FeeSchedule nonDuplicateServices = FeeSchedule.DEFAULT.copyBuilder()
                .services(
                        makeService("foo",makeServiceFee(HederaFunctionality.CONSENSUS_CREATE_TOPIC,22)),
                        makeService("bar",makeServiceFee(HederaFunctionality.CONSENSUS_DELETE_TOPIC,22))
                ).build();
        assertTrue(FeeScheduleUtils.validateServiceNames(nonDuplicateServices));
        FeeSchedule duplicateServices = FeeSchedule.DEFAULT.copyBuilder()
                .services(
                        makeService("foo",makeServiceFee(HederaFunctionality.CONSENSUS_CREATE_TOPIC,22)),
                        makeService("foo",makeServiceFee(HederaFunctionality.CONSENSUS_DELETE_TOPIC,22))
                ).build();
        assertFalse(FeeScheduleUtils.validateServiceNames(duplicateServices));

        FeeSchedule duplicateServiceNames = FeeSchedule.DEFAULT.copyBuilder()
                .services(
                        makeService("foo",makeServiceFee(HederaFunctionality.CONSENSUS_CREATE_TOPIC,22)),
                        makeService("bar",makeServiceFee(HederaFunctionality.CONSENSUS_CREATE_TOPIC,22))
                ).build();
        assertFalse(FeeScheduleUtils.validateServiceNames(duplicateServiceNames));
    }

    @Test
    void checkInvalidServiceDef() {
        FeeSchedule noFees = FeeSchedule.DEFAULT.copyBuilder()
                .services(
                        makeService("foo",
                                ServiceFeeDefinition.DEFAULT
                                        .copyBuilder()
                                        .name(HederaFunctionality.CONSENSUS_CREATE_TOPIC)
                                        .baseFee(0)
                                        .extras()
                                        .free(false).build())
                ).build();
        assertFalse(FeeScheduleUtils.validateServiceScheduleFees(noFees));
        FeeSchedule withOneExtra = FeeSchedule.DEFAULT.copyBuilder()
                .services(
                        makeService("foo",
                                ServiceFeeDefinition.DEFAULT
                                        .copyBuilder()
                                        .name(HederaFunctionality.CONSENSUS_CREATE_TOPIC)
                                        .baseFee(0)
                                        .extras(
                                            makeExtraIncluded(Extra.KEYS, 1)
                                        )
                                        .free(false).build())
                ).build();
        assertTrue(FeeScheduleUtils.validateServiceScheduleFees(withOneExtra));
    }

    @Test
    void checkMissingUnreadable() {
        FeeSchedule hasUnreadable = FeeSchedule.DEFAULT.copyBuilder()
                .unreadable(UnreadableTransactionFee.DEFAULT.copyBuilder().fee(10).build()).build();
        assertTrue(FeeScheduleUtils.validateUnreadableFees(hasUnreadable));
        FeeSchedule missingUnreadable = FeeSchedule.DEFAULT.copyBuilder()
                .build();
        assertFalse(FeeScheduleUtils.validateUnreadableFees(missingUnreadable));
    }

}

package org.hiero.hapi.fees;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.stream.ReadableStreamingData;
import org.hiero.hapi.support.fees.Extra;
import org.hiero.hapi.support.fees.FeeSchedule;
import org.junit.jupiter.api.Test;


import java.util.Objects;

import static org.hiero.hapi.fees.FeeScheduleUtils.makeExtraDef;
import static org.hiero.hapi.fees.FeeScheduleUtils.makeExtraIncluded;
import static org.hiero.hapi.fees.FeeScheduleUtils.makeService;
import static org.hiero.hapi.fees.FeeScheduleUtils.makeServiceFee;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FeeScheduleTest {

    @Test
    void testLoadingFeeScheduleFromJson() {
        try {
            final var fin = FeeScheduleTest.class.getClassLoader().getResourceAsStream("simple-fee-schedule.json");
            final FeeSchedule  buf = FeeSchedule.JSON.parse(new ReadableStreamingData(Objects.requireNonNull(fin)));
            assertTrue(FeeScheduleUtils.validate(buf),"Fee schedule validation failed");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    // extra definitions must have positive values
    @Test
    void catchNegativeValue() {
        FeeSchedule badSchedule = FeeSchedule.DEFAULT.copyBuilder().definedExtras(
                makeExtraDef(Extra.KEYS,-88)
        ).build();
        assertFalse(FeeScheduleUtils.validate(badSchedule),"Fee schedule validation didn't catch negative value");
    }

    // all referenced extras must exist in the defined extras
    @Test
    void catchMissingExtraDef() {
        FeeSchedule badSchedule = FeeSchedule.DEFAULT.copyBuilder()
                .definedExtras(
                    makeExtraDef(Extra.KEYS,1)
                )
                .serviceFees(
                        makeService("Consensus",
                                makeServiceFee(HederaFunctionality.CONSENSUS_CREATE_TOPIC, 22,
                                        makeExtraIncluded(Extra.KEYS,1),
                                        makeExtraIncluded(Extra.SIGNATURES,1)
                                        )
                                )
                )
                .build();
        assertFalse(FeeScheduleUtils.validate(badSchedule),"Fee schedule validation failed to find the missing extras def");
    }
}

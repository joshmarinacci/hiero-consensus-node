package org.hiero.hapi.fees;

import com.hedera.pbj.runtime.ParseException;
import com.hedera.pbj.runtime.io.stream.ReadableStreamingData;
import org.hiero.hapi.support.fees.Extra;
import org.hiero.hapi.support.fees.FeeSchedule;
import org.junit.jupiter.api.Test;


import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static org.hiero.hapi.support.fees.Extra.SIGNATURES;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class FeeScheduleTest {

    @Test
    void testLoadingFeeScheduleFromJson() {
        try {
            final var fin = FeeScheduleTest.class.getClassLoader().getResourceAsStream("simple-fee-schedule.json");
            final FeeSchedule  buf = FeeSchedule.JSON.parse(new ReadableStreamingData(Objects.requireNonNull(fin)));
            System.out.println("parsed simple fees schedule: " + buf);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        assertDoesNotThrow(() -> {
            System.out.println(Extra.KEYS);
//            final var feeSchedule = JsonFeesSchedule.fromJson();
//            assertEquals(1,feeSchedule.getNodeBaseFee());
//            assertEquals(1,feeSchedule.getNodeExtraIncludedCount(SIGNATURES));
            Extra[] nodeExtras = {SIGNATURES};
//            assertArrayEquals(nodeExtras,feeSchedule.getNodeExtraNames().toArray(new Extra[0]));
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
}

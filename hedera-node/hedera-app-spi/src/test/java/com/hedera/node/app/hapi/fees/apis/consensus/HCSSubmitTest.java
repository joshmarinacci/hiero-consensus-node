package com.hedera.node.app.hapi.fees.apis.consensus;

import com.hedera.node.app.hapi.fees.MockFeesSchedule;
import com.hedera.node.app.hapi.fees.apis.MockExchangeRate;
import com.hedera.node.app.hapi.fees.apis.common.FeeConstants.Extras;
import com.hedera.node.app.hapi.fees.apis.common.YesOrNo;
import com.hedera.node.app.spi.fees.Fees;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.hedera.node.app.hapi.fees.apis.common.FeeConstants.HCS_FREE_BYTES;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HCSSubmitTest {
    static MockFeesSchedule schedule;

    @BeforeAll
    static void setup() {
        schedule = new MockFeesSchedule();
        schedule.setExtrasFee(Extras.Keys,1L);
        schedule.setExtrasFee(Extras.Bytes,1L);

        schedule.setServiceBaseFee("ConsensusSubmitMessage",15L);
        schedule.setServiceExtraIncludedCount("ConsensusSubmitMessage", Extras.Keys,1L);
        schedule.setServiceExtraIncludedCount("ConsensusSubmitMessage", Extras.Bytes,(long)HCS_FREE_BYTES);

        schedule.setServiceBaseFee("ConsensusSubmitMessageWithCustomFee",25L);
        schedule.setServiceExtraIncludedCount("ConsensusSubmitMessageWithCustomFee", Extras.Keys,1L);
        schedule.setServiceExtraIncludedCount("ConsensusSubmitMessageWithCustomFee", Extras.Bytes,(long)HCS_FREE_BYTES);
    }

    @Test
    void testHCSSubmitNoCustomFee() {
        HCSSubmit transfer = new HCSSubmit();
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.toString(), 1L);
        params.put("hasCustomFee", YesOrNo.NO);

        for (int numBytes = 10; numBytes < 1000; numBytes += 10) {
            params.put(Extras.Bytes.name(), (long)numBytes);
            Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
            long overage = (numBytes <= HCS_FREE_BYTES)
                    ? 15
                    : ((15 + (numBytes - HCS_FREE_BYTES) * 1));
            assertEquals(overage, fee.usd(), "HCS topic Submit without custom fee - " + numBytes + " bytes");
        }
    }

    @Test
    void testHCSSubmitWithCustomFee() {
        HCSSubmit transfer = new HCSSubmit();
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.toString(), 1L);
        params.put("hasCustomFee", YesOrNo.YES);

        for (int numBytes = 10; numBytes < 1000; numBytes += 10) {
            params.put(Extras.Bytes.name(), (long)numBytes);
            Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
            long overage = (numBytes <= HCS_FREE_BYTES)
                    ? 25
                    : ((25+ (numBytes - HCS_FREE_BYTES) * 1));
            assertEquals(overage, fee.usd(), "HCS topic Submit without custom fee - " + numBytes + " bytes");
        }
    }
}

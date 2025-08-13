package com.hedera.node.app.hapi.simplefees.apis.consensus;

import com.hedera.node.app.hapi.simplefees.MockFeesSchedule;
import com.hedera.node.app.hapi.simplefees.apis.MockExchangeRate;
import com.hedera.node.app.hapi.simplefees.apis.common.FeeConstants.Params;
import com.hedera.node.app.hapi.simplefees.apis.common.YesOrNo;
import com.hedera.node.app.spi.fees.Fees;
import org.hiero.hapi.support.fees.Extra;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.hedera.node.app.hapi.simplefees.apis.common.FeeConstants.HCS_FREE_BYTES;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HCSSubmitTest {
    static MockFeesSchedule schedule;

    @BeforeAll
    static void setup() {
        schedule = new MockFeesSchedule();
        schedule.setExtrasFee(Extra.KEYS,1L);
        schedule.setExtrasFee(Extra.BYTES,1L);

        schedule.setServiceBaseFee("ConsensusSubmitMessage",15L);
        schedule.setServiceExtraIncludedCount("ConsensusSubmitMessage", Extra.KEYS,1L);
        schedule.setServiceExtraIncludedCount("ConsensusSubmitMessage", Extra.BYTES,(long)HCS_FREE_BYTES);

        schedule.setServiceBaseFee("ConsensusSubmitMessageWithCustomFee",25L);
        schedule.setServiceExtraIncludedCount("ConsensusSubmitMessageWithCustomFee", Extra.KEYS,1L);
        schedule.setServiceExtraIncludedCount("ConsensusSubmitMessageWithCustomFee", Extra.BYTES,(long)HCS_FREE_BYTES);
    }

    @Test
    void testHCSSubmitNoCustomFee() {
        HCSSubmit transfer = new HCSSubmit();
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Params.HasCustomFee.name(), YesOrNo.NO);

        for (int numBytes = 10; numBytes < 1000; numBytes += 10) {
            params.put(Extra.BYTES.name(), (long)numBytes);
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
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Params.HasCustomFee.name(), YesOrNo.YES);

        for (int numBytes = 10; numBytes < 1000; numBytes += 10) {
            params.put(Extra.BYTES.name(), (long)numBytes);
            Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);
            long overage = (numBytes <= HCS_FREE_BYTES)
                    ? 25
                    : ((25+ (numBytes - HCS_FREE_BYTES) * 1));
            assertEquals(overage, fee.usd(), "HCS topic Submit with custom fee - " + numBytes + " bytes");
        }
    }
}

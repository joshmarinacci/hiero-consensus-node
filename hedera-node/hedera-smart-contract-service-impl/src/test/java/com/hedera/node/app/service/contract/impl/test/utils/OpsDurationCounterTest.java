// SPDX-License-Identifier: Apache-2.0
package com.hedera.node.app.service.contract.impl.test.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.hedera.node.app.service.contract.impl.exec.utils.OpsDurationCounter;
import com.hedera.node.app.service.contract.impl.hevm.OpsDurationSchedule;
import org.junit.jupiter.api.Test;

class OpsDurationCounterTest {
    @Test
    void testRecordOpsDurationConsumed() {
        final var schedule = mock(OpsDurationSchedule.class);
        OpsDurationCounter opsDurationCounter = OpsDurationCounter.withSchedule(schedule);
        opsDurationCounter.recordOpsDurationUnitsConsumed(25L);
        assertEquals(25L, opsDurationCounter.opsDurationUnitsConsumed());
    }
}

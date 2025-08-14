package com.hedera.node.app.hapi.simplefees.apis.file;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.node.app.hapi.simplefees.MockFeesSchedule;
import com.hedera.node.app.hapi.simplefees.apis.MockExchangeRate;
import com.hedera.node.app.spi.fees.Fees;
import org.hiero.hapi.support.fees.Extra;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.hedera.hapi.node.base.HederaFunctionality.*;
import static com.hedera.node.app.hapi.simplefees.apis.common.FeeConstants.FILE_FREE_BYTES;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FileOperationsTest {
    static MockFeesSchedule schedule;

    @BeforeAll
    static void setup() {
        schedule = new MockFeesSchedule();
        schedule.setExtrasFee(Extra.KEYS, 1L);
        schedule.setExtrasFee(Extra.SIGNATURES, 1L);
        schedule.setExtrasFee(Extra.BYTES, 11L);

        schedule.setServiceBaseFee(FILE_CREATE,50L);
    }

    @Test
    void testFileOperations() {
        FileOperations transfer = new FileOperations("FileCreate", "dummy description");
        Map<String, Object> params = new HashMap<>();
        params.put(Extra.SIGNATURES.name(), 1L);
        params.put(Extra.KEYS.name(), 1L);

        for (int numBytes = 10; numBytes < 100000; numBytes += 100) {
            params.put(Extra.BYTES.name(), (long)numBytes);
            Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);

            long overage = (numBytes <= FILE_FREE_BYTES)
                    ? 50
                    : ((50 + (numBytes - FILE_FREE_BYTES) * 11));
            assertEquals(overage, fee.usd(), "FILE operation fee - " + numBytes + " bytes");
        }
    }
}

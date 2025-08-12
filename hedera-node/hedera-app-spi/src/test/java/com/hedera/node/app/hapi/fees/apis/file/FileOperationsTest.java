package com.hedera.node.app.hapi.fees.apis.file;

import com.hedera.node.app.hapi.fees.MockFeesSchedule;
import com.hedera.node.app.hapi.fees.apis.MockExchangeRate;
import com.hedera.node.app.hapi.fees.apis.common.FeeConstants.Extras;
import com.hedera.node.app.spi.fees.Fees;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.hedera.node.app.hapi.fees.apis.common.FeeConstants.FILE_FREE_BYTES;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FileOperationsTest {
    static MockFeesSchedule schedule;

    @BeforeAll
    static void setup() {
        schedule = new MockFeesSchedule();
        schedule.setExtrasFee(Extras.Keys, 1L);
        schedule.setExtrasFee(Extras.Signatures, 1L);
        schedule.setExtrasFee(Extras.Bytes, 11L);

        schedule.setServiceBaseFee("FileCreate",50L);
    }

    @Test
    void testFileOperations() {
        FileOperations transfer = new FileOperations("FileCreate", "dummy description");
        Map<String, Object> params = new HashMap<>();
        params.put(Extras.Signatures.name(), 1L);
        params.put(Extras.Keys.name(), 1L);

        for (int numBytes = 10; numBytes < 100000; numBytes += 100) {
            params.put(Extras.Bytes.name(), (long)numBytes);
            Fees fee = transfer.computeFee(params, new MockExchangeRate().activeRate(), schedule);

            long overage = (numBytes <= FILE_FREE_BYTES)
                    ? 50
                    : ((50 + (numBytes - FILE_FREE_BYTES) * 11));
            assertEquals(overage, fee.usd(), "FILE operation fee - " + numBytes + " bytes");
        }
    }
}

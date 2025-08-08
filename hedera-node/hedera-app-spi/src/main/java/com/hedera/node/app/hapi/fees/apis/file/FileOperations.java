package com.hedera.node.app.hapi.fees.apis.file;

import com.hedera.node.app.hapi.fees.AbstractFeeModel;
import com.hedera.node.app.hapi.fees.AbstractFeesSchedule;
import com.hedera.node.app.hapi.fees.AbstractFeesSchedule.Extras;
import com.hedera.node.app.hapi.fees.FeeResult;
import com.hedera.node.app.hapi.fees.ParameterDefinition;

import java.util.List;
import java.util.Map;

import static com.hedera.node.app.hapi.fees.apis.common.FeeConstants.*;


public class FileOperations extends AbstractFeeModel {
    String api;
    String description;

    private final List<ParameterDefinition> params = List.of(
            new ParameterDefinition("numKeys", "number", null,MIN_KEYS, MIN_KEYS, MAX_KEYS, "Number of keys"),
            new ParameterDefinition("numBytes", "number", null, FILE_FREE_BYTES, FILE_MIN_BYTES, FILE_MAX_BYTES, "Size of the file (bytes)")
    );

    public FileOperations( String api, String description) {
        this.api = api;
        this.description = description;
    }

    @Override
    public String getService() { return "File"; }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getMethodName() {
        return this.api;
    }

    @Override
    protected List<ParameterDefinition> apiSpecificParams() {
        return params;
    }

    @Override
    protected FeeResult computeApiSpecificFee(Map<String, Object> values, AbstractFeesSchedule feesSchedule) {
        FeeResult fee = new FeeResult();

        fee.addDetail("Base fee", 1, feesSchedule.getServiceBaseFee(api));

        long numKeys = (long) values.get(Extras.Keys.name());
        long numFreeKeys = 1;
        if (numKeys > numFreeKeys) {
            fee.addDetail("Additional keys", numKeys - numFreeKeys, (numKeys - numFreeKeys) * feesSchedule.getServiceBaseFee(Extras.Keys.name()));
        }

        long numBytes = (long) values.get(Extras.Bytes.name());
        if (numBytes > FILE_FREE_BYTES) {
            fee.addDetail("Additional file size", (numBytes - FILE_FREE_BYTES), (numBytes - FILE_FREE_BYTES) * feesSchedule.getExtrasFee(Extras.Bytes.name()));
        }
        System.out.println("total fee is " + fee);
        return fee;
    }
}

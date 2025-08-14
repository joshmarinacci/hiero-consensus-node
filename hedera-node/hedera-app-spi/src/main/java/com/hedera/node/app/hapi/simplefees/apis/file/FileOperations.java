package com.hedera.node.app.hapi.simplefees.apis.file;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.node.app.hapi.simplefees.AbstractFeeModel;
import com.hedera.node.app.hapi.simplefees.AbstractFeesSchedule;
import com.hedera.node.app.hapi.simplefees.FeeResult;
import com.hedera.node.app.hapi.simplefees.ParameterDefinition;
import org.hiero.hapi.support.fees.Extra;

import java.util.List;
import java.util.Map;

import static com.hedera.node.app.hapi.simplefees.apis.common.FeeConstants.*;


public class FileOperations extends AbstractFeeModel {
    HederaFunctionality api;
    String description;

    private final List<ParameterDefinition> params = List.of(
            new ParameterDefinition("numKeys", "number", null,MIN_KEYS, MIN_KEYS, MAX_KEYS, "Number of keys"),
            new ParameterDefinition("numBytes", "number", null, FILE_FREE_BYTES, FILE_MIN_BYTES, FILE_MAX_BYTES, "Size of the file (bytes)")
    );

    public FileOperations( HederaFunctionality api, String description) {
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
        return this.api.name();
    }

    @Override
    protected List<ParameterDefinition> apiSpecificParams() {
        return params;
    }

    @Override
    protected FeeResult computeApiSpecificFee(Map<String, Object> params, AbstractFeesSchedule feesSchedule) {
        FeeResult fee = new FeeResult();

        fee.addDetail("Base fee", 1, feesSchedule.getServiceBaseFee(api));

        long numKeys = (long) params.get(Extra.KEYS.name());
        long numFreeKeys = 1;
        if (numKeys > numFreeKeys) {
            fee.addDetail("Additional keys", numKeys - numFreeKeys, (numKeys - numFreeKeys) * feesSchedule.getExtrasFee(Extra.KEYS));
        }

        long numBytes = (long) params.get(Extra.BYTES.name());
        if (numBytes > FILE_FREE_BYTES) {
            fee.addDetail("Additional file size", (numBytes - FILE_FREE_BYTES), (numBytes - FILE_FREE_BYTES) * feesSchedule.getExtrasFee(Extra.BYTES));
        }
        System.out.println("total fee is " + fee);
        return fee;
    }
}

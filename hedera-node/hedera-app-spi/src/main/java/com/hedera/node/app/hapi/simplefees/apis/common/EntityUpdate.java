package com.hedera.node.app.hapi.simplefees.apis.common;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.node.app.hapi.simplefees.AbstractFeeModel;
import com.hedera.node.app.hapi.simplefees.AbstractFeesSchedule;
import com.hedera.node.app.hapi.simplefees.FeeResult;
import com.hedera.node.app.hapi.simplefees.ParameterDefinition;
import org.hiero.hapi.support.fees.Extra;

import java.util.List;
import java.util.Map;

import static com.hedera.node.app.hapi.simplefees.apis.common.FeeConstants.MAX_KEYS;
import static com.hedera.node.app.hapi.simplefees.apis.common.FeeConstants.MIN_KEYS;


public class EntityUpdate extends AbstractFeeModel {
    private final String service;
    private final String api;
    private final String description;

    private final List<ParameterDefinition> params = List.of(
            new ParameterDefinition("numKeys", "number", null,MIN_KEYS, MIN_KEYS, MAX_KEYS, "Number of keys")
    );

    public EntityUpdate(String service, String api, String description) {
        this.service = service;
        this.api = api;
        this.description = description;
    }

    @Override
    public String getService() { return service; }

    @Override
    public String getMethodName() { return this.api; }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    protected List<ParameterDefinition> apiSpecificParams() {
        return params;
    }

    @Override
    protected FeeResult computeApiSpecificFee(Map<String, Object> values, AbstractFeesSchedule feesSchedule) {
        FeeResult result = new FeeResult();
        result.addDetail("Base Fee", 1, feesSchedule.getServiceBaseFee(HederaFunctionality.valueOf(api)));
        final long numKeys = (long) values.get(Extra.KEYS.name());
        final long numFreeKeys = feesSchedule.getServiceExtraIncludedCount(HederaFunctionality.valueOf(api), Extra.KEYS);
        if (numKeys > numFreeKeys) {
            result.addDetail("Additional Keys", numKeys - numFreeKeys, (numKeys - numFreeKeys) * feesSchedule.getExtrasFee(Extra.KEYS));
        }
        return result;
    }
}

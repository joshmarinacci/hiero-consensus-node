package com.hedera.node.app.hapi.fees.apis.common;

import com.hedera.node.app.hapi.fees.AbstractFeeModel;
import com.hedera.node.app.hapi.fees.AbstractFeesSchedule;
import com.hedera.node.app.hapi.fees.AbstractFeesSchedule.Extras;
import com.hedera.node.app.hapi.fees.FeeResult;
import com.hedera.node.app.hapi.fees.ParameterDefinition;

import java.util.List;
import java.util.Map;

import static com.hedera.node.app.hapi.fees.apis.common.FeeConstants.MAX_KEYS;
import static com.hedera.node.app.hapi.fees.apis.common.FeeConstants.MIN_KEYS;


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
        result.addDetail("Base result", 1, feesSchedule.getServiceBaseFee(api));
        final long numKeys = (long) values.get(Extras.Keys.toString());
        final long numFreeKeys = feesSchedule.getServiceExtraIncludedCount(api,Extras.Keys.toString());
        if (numKeys > numFreeKeys) {
            result.addDetail("Additional Keys", numKeys - numFreeKeys, (numKeys - numFreeKeys) * feesSchedule.getExtrasFee(Extras.Keys.toString()));
        }
        return result;
    }
}

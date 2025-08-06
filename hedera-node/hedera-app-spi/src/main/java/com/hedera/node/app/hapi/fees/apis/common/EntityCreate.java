package com.hedera.node.app.hapi.fees.apis.common;

import com.hedera.node.app.hapi.fees.AbstractFeeModel;
import com.hedera.node.app.hapi.fees.AbstractFeesSchedule;
import com.hedera.node.app.hapi.fees.AbstractFeesSchedule.Extras;
import com.hedera.node.app.hapi.fees.FeeResult;
import com.hedera.node.app.hapi.fees.ParameterDefinition;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hedera.node.app.hapi.fees.apis.common.FeeConstants.MAX_KEYS;
import static com.hedera.node.app.hapi.fees.apis.common.FeeConstants.MIN_KEYS;

public class EntityCreate extends AbstractFeeModel {
    private final String service;
    private final String api;
    private final String description;
    private final boolean customFeeCapable;

    private final List<ParameterDefinition> params = List.of(
            new ParameterDefinition(Extras.Keys.toString(), "number", null, MIN_KEYS, MIN_KEYS, MAX_KEYS, "Number of keys")
    );
    private final List<ParameterDefinition> customFeeParams = List.of(
            new ParameterDefinition("hasCustomFee", "list", new Object[] {YesOrNo.YES, YesOrNo.NO}, YesOrNo.NO, 0, 0, "Enable custom fee?")
    );

    public EntityCreate(String service, String api, String description, boolean customFeeCapable) {
        this.service = service;
        this.api = api;
        this.description = description;
        this.customFeeCapable = customFeeCapable;
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public String getMethodName() { return this.api; }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    protected List<ParameterDefinition> apiSpecificParams() {
        if (customFeeCapable) {
            return Stream.concat(params.stream(), customFeeParams.stream()).collect(Collectors.toList());
        }
        return params;
    }

    @Override
    protected FeeResult computeApiSpecificFee(Map<String, Object> values, AbstractFeesSchedule feesSchedule) {
        FeeResult result = new FeeResult();
        if (customFeeCapable && values.get("hasCustomFee") == YesOrNo.YES) {
            result.addDetail("Base result", 1, feesSchedule.getServiceBaseFee(api + "WithCustomFee"));
        } else {
            result.addDetail("Base result", 1, feesSchedule.getServiceBaseFee(api));
        }

        final List<String> extras = feesSchedule.getServiceExtras(api);
        for(var extra : extras) {
            final long node_bytes_fee = feesSchedule.getExtrasFee(extra);
            final long node_bytes_included = feesSchedule.getServiceExtraIncludedCount(api,extra);
            System.out.println("processing extra " + extra);
            final long used = (long) values.get(extra);
            if (used > node_bytes_included) {
                final long additional_bytes = used - node_bytes_included;
                final long more_fee = additional_bytes * node_bytes_fee;
                result.addDetail("Node Bytes Overage", additional_bytes, more_fee);
            }
        }
        return result;
    }
}


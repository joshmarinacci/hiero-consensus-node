package com.hedera.node.app.hapi.simplefees.apis.common;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.node.app.hapi.simplefees.AbstractFeeModel;
import com.hedera.node.app.hapi.simplefees.AbstractFeesSchedule;
import com.hedera.node.app.hapi.simplefees.FeeResult;
import com.hedera.node.app.hapi.simplefees.ParameterDefinition;
import com.hedera.node.app.hapi.simplefees.apis.common.FeeConstants.Params;
import org.hiero.hapi.support.fees.Extra;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hedera.node.app.hapi.simplefees.apis.common.FeeConstants.MAX_KEYS;
import static com.hedera.node.app.hapi.simplefees.apis.common.FeeConstants.MIN_KEYS;

public class EntityCreate extends AbstractFeeModel {
    private final String service;
    private final String api;
    private final String description;
    private final boolean customFeeCapable;

    private final List<ParameterDefinition> params = List.of(
            new ParameterDefinition(Extra.KEYS.name(), "number", null, MIN_KEYS, MIN_KEYS, MAX_KEYS, "Number of keys")
    );
    private final List<ParameterDefinition> customFeeParams = List.of(
            new ParameterDefinition(Params.HasCustomFee.toString(), "list", new Object[] {YesOrNo.YES, YesOrNo.NO}, YesOrNo.NO, 0, 0, "Enable custom fee?")
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
        if (customFeeCapable && values.get(Params.HasCustomFee.name()) == YesOrNo.YES) {
            result.addDetail("Base fee for " + this.api + " with custom fee ", 1, feesSchedule.getServiceBaseFee(HederaFunctionality.valueOf(api + "WithCustomFee")));
        } else {
            result.addDetail("Base fee for " + this.api, 1, feesSchedule.getServiceBaseFee(HederaFunctionality.valueOf(api)));
        }

        final var extras = feesSchedule.getServiceExtras(HederaFunctionality.valueOf(api));
        for(var extra : extras) {
            final long extraFee = feesSchedule.getExtrasFee(extra);
            final long extraIncludedCount = feesSchedule.getServiceExtraIncludedCount(HederaFunctionality.valueOf(api),extra);
            final long extraUsed = (long) values.get(extra.name());
            if (extraUsed > extraIncludedCount) {
                final long overage = extraUsed - extraIncludedCount;
                result.addDetail( extra +" Overage", overage, overage * extraFee);
            }
        }
        return result;
    }
}


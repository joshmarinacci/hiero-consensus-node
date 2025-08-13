package com.hedera.node.app.hapi.simplefees.apis.crypto;

import com.hedera.node.app.hapi.simplefees.AbstractFeeModel;
import com.hedera.node.app.hapi.simplefees.AbstractFeesSchedule;
import com.hedera.node.app.hapi.simplefees.FeeResult;
import com.hedera.node.app.hapi.simplefees.ParameterDefinition;

import java.util.List;
import java.util.Map;

import static com.hedera.node.app.hapi.simplefees.apis.common.FeeConstants.*;

public class CryptoAllowance extends AbstractFeeModel {
    String api;
    String description;

    private final List<ParameterDefinition> params = List.of(
            new ParameterDefinition("numAllowances", "number", null, MIN_ALLOWANCES, MIN_ALLOWANCES, MAX_ALLOWANCES, "Number of Allowances")
    );

    public CryptoAllowance(String api, String description) {
        this.api = api;
        this.description = description;
    }

    @Override
    public String getService() {
        return "Crypto";
    }

    @Override
    public String getMethodName() {
        return this.api;
    }

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
        FeeResult fee = new FeeResult();
        fee.addDetail("Base fee", 1, feesSchedule.getServiceBaseFee(api));

        int numAllowances = (int) values.get("numAllowances");
        if (numAllowances > FREE_ALLOWANCES) {
            fee.addDetail("Additional allowances", (numAllowances - FREE_ALLOWANCES), (numAllowances - FREE_ALLOWANCES) * feesSchedule.getServiceBaseFee(api));
        }
        return fee;
    }
}

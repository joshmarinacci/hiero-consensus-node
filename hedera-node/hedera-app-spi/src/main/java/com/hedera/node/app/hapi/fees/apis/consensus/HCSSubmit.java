package com.hedera.node.app.hapi.fees.apis.consensus;

import com.hedera.node.app.hapi.fees.AbstractFeeModel;
import com.hedera.node.app.hapi.fees.AbstractFeesSchedule;
import com.hedera.node.app.hapi.fees.apis.common.FeeConstants.Extras;
import com.hedera.node.app.hapi.fees.FeeResult;
import com.hedera.node.app.hapi.fees.ParameterDefinition;
import com.hedera.node.app.hapi.fees.apis.common.YesOrNo;

import java.util.List;
import java.util.Map;

import static com.hedera.node.app.hapi.fees.apis.common.FeeConstants.*;

public class HCSSubmit extends AbstractFeeModel {

    private final List<ParameterDefinition> params = List.of(
            new ParameterDefinition(Params.HasCustomFee.toString(), "list", new String[] { "Yes", "No" }, "No", 0, 0, "Does this topic have custom fee"),
            new ParameterDefinition(Extras.Bytes.toString(), "number", null, null, HCS_MIN_BYTES, HCS_MAX_BYTES, "Size of the message (bytes)")
    );

    @Override
    public String getService() {
        return "Consensus";
    }

    @Override
    public String getDescription() {
        return "Submit a message to an existing topic";
    }

    @Override
    public String getMethodName() {
        return "ConsensusSubmitMessage";
    }

    @Override
    protected List<ParameterDefinition> apiSpecificParams() {
        return params;
    }

    @Override
    protected FeeResult computeApiSpecificFee(Map<String, Object> values, AbstractFeesSchedule feesSchedule) {
        FeeResult fee = new FeeResult();

        YesOrNo hasCustomFee = (YesOrNo) values.get(Params.HasCustomFee.toString());
        if (!values.containsKey(Params.HasCustomFee.toString())) {
            throw new Error(" Missing hasCustomFee parameter.");
        }
        if (hasCustomFee == YesOrNo.NO) {
            fee.addDetail("Base fee", 1, feesSchedule.getServiceBaseFee("ConsensusSubmitMessage"));
        } else {
            fee.addDetail("Base fee", 1, feesSchedule.getServiceBaseFee("ConsensusSubmitMessageWithCustomFee"));
        }
        if(!values.containsKey(Extras.Bytes.toString())) {
            throw new Error("Missing Bytes parameter.");
        }
        long numBytes = (long) values.get(Extras.Bytes.toString());
        var free = feesSchedule.getServiceExtraIncludedCount("ConsensusSubmitMessage", Extras.Bytes.toString());
        var excessBytes = numBytes - free;
        if (excessBytes > 0) {
            fee.addDetail("Additional message size",  excessBytes, excessBytes * feesSchedule.getExtrasFee(Extras.Bytes.toString()));
        }
        return fee;
    }
}

package com.hedera.node.app.hapi.simplefees.apis.consensus;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.node.app.hapi.simplefees.AbstractFeeModel;
import com.hedera.node.app.hapi.simplefees.AbstractFeesSchedule;
import com.hedera.node.app.hapi.simplefees.FeeResult;
import com.hedera.node.app.hapi.simplefees.ParameterDefinition;
import com.hedera.node.app.hapi.simplefees.apis.common.YesOrNo;
import org.hiero.hapi.support.fees.Extra;

import java.util.List;
import java.util.Map;

import static com.hedera.node.app.hapi.simplefees.apis.common.FeeConstants.*;

public class HCSSubmit extends AbstractFeeModel {

    private final List<ParameterDefinition> params = List.of(
            new ParameterDefinition(Params.HasCustomFee.toString(), "list", new String[] { "Yes", "No" }, "No", 0, 0, "Does this topic have custom fee"),
            new ParameterDefinition(Extra.BYTES.toString(), "number", null, null, HCS_MIN_BYTES, HCS_MAX_BYTES, "Size of the message (bytes)")
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
        return HederaFunctionality.CONSENSUS_SUBMIT_MESSAGE.name();
    }

    @Override
    protected List<ParameterDefinition> apiSpecificParams() {
        return params;
    }

    @Override
    protected FeeResult computeApiSpecificFee(Map<String, Object> values, AbstractFeesSchedule feesSchedule) {
        FeeResult fee = new FeeResult();

        YesOrNo hasCustomFee = (YesOrNo) values.get(Params.HasCustomFee.name());
        if (!values.containsKey(Params.HasCustomFee.name())) {
            throw new Error(" Missing hasCustomFee parameter.");
        }
        if (hasCustomFee == YesOrNo.NO) {
            fee.addDetail("Base fee", 1, feesSchedule.getServiceBaseFee(HederaFunctionality.CONSENSUS_SUBMIT_MESSAGE));
        } else {
            fee.addDetail("Base fee", 1, feesSchedule.getServiceBaseFee(HederaFunctionality.valueOf("ConsensusSubmitMessageWithCustomFee")));
        }
        if(!values.containsKey(Extra.BYTES.toString())) {
            throw new Error("Missing Bytes parameter.");
        }
        long numBytes = (long) values.get(Extra.BYTES.toString());
        var free = feesSchedule.getServiceExtraIncludedCount(HederaFunctionality.CONSENSUS_SUBMIT_MESSAGE, Extra.BYTES);
        var excessBytes = numBytes - free;
        if (excessBytes > 0) {
            fee.addDetail("Additional message size",  excessBytes, excessBytes * feesSchedule.getExtrasFee(Extra.BYTES));
        }
        return fee;
    }
}

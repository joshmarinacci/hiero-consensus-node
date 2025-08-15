package org.hiero.hapi.fees;

import com.hedera.hapi.node.base.HederaFunctionality;
import org.hiero.hapi.fees.apis.common.BaseFeeModel;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.hedera.hapi.node.base.HederaFunctionality.CONSENSUS_CREATE_TOPIC;
import static com.hedera.hapi.node.base.HederaFunctionality.CONSENSUS_DELETE_TOPIC;
import static com.hedera.hapi.node.base.HederaFunctionality.CONSENSUS_SUBMIT_MESSAGE;
import static com.hedera.hapi.node.base.HederaFunctionality.CONSENSUS_UPDATE_TOPIC;

public class FeeModelRegistry {
    private static final Map<HederaFunctionality, FeeModel> registry = new LinkedHashMap<>();
    static {
        registry.put(CONSENSUS_CREATE_TOPIC, new BaseFeeModel(CONSENSUS_CREATE_TOPIC, "Create a new topic"));
        registry.put(CONSENSUS_UPDATE_TOPIC, new BaseFeeModel(CONSENSUS_UPDATE_TOPIC,"Update topic"));
        registry.put(CONSENSUS_SUBMIT_MESSAGE, new BaseFeeModel(CONSENSUS_SUBMIT_MESSAGE, "Submit message"));
        registry.put(CONSENSUS_DELETE_TOPIC, new BaseFeeModel(CONSENSUS_DELETE_TOPIC, "Delete topic"));
    }

    public static FeeModel lookupModel(HederaFunctionality service) {
        if(!registry.containsKey(service)) {
            throw new Error("No registered model found for service " + service);
        }
        return registry.get(service);
    }
}

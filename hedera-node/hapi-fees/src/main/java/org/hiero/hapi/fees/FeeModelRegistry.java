package org.hiero.hapi.fees;

import com.hedera.hapi.node.base.HederaFunctionality;
import org.hiero.hapi.fees.apis.common.BaseFeeModel;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.hedera.hapi.node.base.HederaFunctionality.CONSENSUS_CREATE_TOPIC;
import static com.hedera.hapi.node.base.HederaFunctionality.CONSENSUS_UPDATE_TOPIC;

public class FeeModelRegistry {
    public static final Map<HederaFunctionality, FeeModel> registry = new LinkedHashMap<>();
    static {
        registry.put(CONSENSUS_CREATE_TOPIC, new BaseFeeModel(CONSENSUS_CREATE_TOPIC, "Create a new topic"));
        registry.put(CONSENSUS_UPDATE_TOPIC, new BaseFeeModel(CONSENSUS_UPDATE_TOPIC,"Update topic"));
    }
}

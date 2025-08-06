package com.hedera.node.app.hapi.fees.apis.common;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.hapi.node.transaction.ExchangeRate;
import com.hedera.node.app.hapi.fees.AbstractFeeModel;
import com.hedera.node.app.hapi.fees.AbstractFeesSchedule;
import com.hedera.node.app.hapi.fees.apis.consensus.HCSSubmit;
import com.hedera.node.app.spi.fees.Fees;

import java.util.Map;

public class FeesHelper {
    public static AbstractFeeModel createModel(String service, String method) {
        return switch (method) {
            case "ConsensusCreateTopic" -> new EntityCreate(service,method, "description",false);
            case "ConsensusSubmitMessage" -> new HCSSubmit();
            case "ConsensusSubmitMessageWithCustomFee" -> new HCSSubmit();
            default -> throw new IllegalStateException("Unexpected value: " + method);
        };
    }
    public static EntityCreate makeCreateEntity(HederaFunctionality api, String description, boolean customFeeCapable) {
        return new EntityCreate(lookupServiceName(api), lookupAPIName(api), description, customFeeCapable);
    }

    private static String lookupServiceName(HederaFunctionality api) {
        return switch (api) {
            case TOKEN_CREATE -> "Token";
            case CONSENSUS_CREATE_TOPIC, CONSENSUS_DELETE_TOPIC, CONSENSUS_UPDATE_TOPIC, CONSENSUS_GET_TOPIC_INFO -> "Consensus";
            case CRYPTO_CREATE, CRYPTO_UPDATE ->  "Crypto";
            default -> throw new IllegalArgumentException("Unsupported Hedera API: " + api);
        };
    }

    public static String lookupAPIName(HederaFunctionality api) {
        return switch (api) {
            case TOKEN_CREATE -> "TokenCreate";
            case CONSENSUS_CREATE_TOPIC -> "ConsensusCreateTopic";
            case CONSENSUS_DELETE_TOPIC -> "ConsensusDeleteTopic";
            case CONSENSUS_UPDATE_TOPIC -> "ConsensusUpdateTopic";
            case CONSENSUS_GET_TOPIC_INFO -> "ConsensusGetTopicInfo";
            case CRYPTO_CREATE -> "CryptoCreate";
            case CRYPTO_UPDATE -> "CryptoUpdate";
            default -> throw new IllegalArgumentException("Unsupported Hedera API: " + api);
        };
    }
    public static Fees genericComputeFee(String service, String method, Map<String,Object> params, ExchangeRate rate, AbstractFeesSchedule feeSchedule) {
        throw new UnsupportedOperationException("Not supported yet.");
//        return createModel(service, method).computeFee2(params,rate,feeSchedule);
    }
}


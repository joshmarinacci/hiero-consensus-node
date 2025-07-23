package com.hedera.node.app.hapi.fees.apis.common;

import com.hedera.hapi.node.base.HederaFunctionality;

public class FeesHelper {
    public static EntityCreate makeEntity(HederaFunctionality api, String description, int numFreeKeys, boolean customFeeCapable) {
        return new EntityCreate(lookupServiceName(api), lookupAPIName(api), description, numFreeKeys, customFeeCapable);
    }

    private static String lookupServiceName(HederaFunctionality api) {
        return switch (api) {
            case TOKEN_CREATE -> "Token";
            case CONSENSUS_CREATE_TOPIC, CONSENSUS_DELETE_TOPIC, CONSENSUS_UPDATE_TOPIC, CONSENSUS_GET_TOPIC_INFO -> "Consensus";
            case CRYPTO_CREATE, CRYPTO_UPDATE ->  "Crypto";
            default -> throw new IllegalArgumentException("Unsupported Hedera API: " + api);
        };
    }

    private static String lookupAPIName(HederaFunctionality api) {
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
}


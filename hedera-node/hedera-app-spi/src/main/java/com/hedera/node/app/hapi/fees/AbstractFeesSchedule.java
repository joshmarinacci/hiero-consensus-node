package com.hedera.node.app.hapi.fees;

import java.util.List;

public interface AbstractFeesSchedule {

    enum Extras {
        Signatures,
        Bytes,
        Keys,
        TokenTypes,
        Accounts,
        StandardFungibleTokens,
        StandardNonFungibleTokens
    }

    List<String> getDefinedExtraNames();
    long getExtrasFee(String name);

    long getNodeBaseFee();
    List<String> getNodeExtraNames();
    long getNodeExtraIncludedCount(String name);

    long getNetworkMultiplier();

    long getServiceBaseFee(String method);
    List<String> getServiceExtras(String method);
    long getServiceExtraIncludedCount(String method, String name);


}

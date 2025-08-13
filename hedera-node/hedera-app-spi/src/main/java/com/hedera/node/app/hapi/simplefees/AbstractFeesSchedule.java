package com.hedera.node.app.hapi.simplefees;

import java.util.List;

public interface AbstractFeesSchedule {

    List<String> getDefinedExtraNames();
    long getExtrasFee(String name);

    long getNodeBaseFee();
    List<String> getNodeExtraNames();
    long getNodeExtraIncludedCount(String name);

    long getNetworkMultiplier();

    List<String> getServiceNames();
    long getServiceBaseFee(String method);
    List<String> getServiceExtras(String method);
    long getServiceExtraIncludedCount(String method, String name);


}

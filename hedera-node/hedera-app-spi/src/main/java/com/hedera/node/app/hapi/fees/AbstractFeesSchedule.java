package com.hedera.node.app.hapi.fees;

public interface AbstractFeesSchedule {
    String SignatureVerifications = "SignatureVerifications";
    String Bytes = "Bytes";

    double getExtrasFee(String name);

    double getNetworkBaseFee(String api);
    double getNodeBaseFee(String api);

    int getNetworkBaseExtrasIncluded(String api, String name);
}

package com.hedera.node.app.hapi.simplefees;

import org.hiero.hapi.support.fees.Extra;
import org.hiero.hapi.support.fees.FeeSchedule;

import java.util.List;

public interface AbstractFeesSchedule {
    FeeSchedule getRawSchedule();

    List<Extra> getDefinedExtraNames();
    long getExtrasFee(Extra name);

    long getNodeBaseFee();
    List<Extra> getNodeExtraNames();
    long getNodeExtraIncludedCount(Extra name);

    long getNetworkMultiplier();

    List<String> getServiceNames();
    long getServiceBaseFee(String method);
    List<Extra> getServiceExtras(String method);
    long getServiceExtraIncludedCount(String method, Extra name);


}

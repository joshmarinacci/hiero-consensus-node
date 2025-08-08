package com.hedera.node.app.hapi.fees;

import com.hedera.hapi.node.transaction.ExchangeRate;
import com.hedera.node.app.hapi.fees.apis.common.AssociateOrDissociate;
import com.hedera.node.app.hapi.fees.apis.common.FTOrNFT;
import com.hedera.node.app.hapi.fees.apis.common.YesOrNo;
import com.hedera.node.app.spi.fees.Fees;

import java.util.*;

import static com.hedera.node.app.hapi.fees.apis.common.FeeConstants.MAX_SIGNATURES;
import static com.hedera.node.app.hapi.fees.apis.common.FeeConstants.MIN_SIGNATURES;
import static com.hedera.node.app.hapi.utils.CommonPbjConverters.fromPbj;

public abstract class AbstractFeeModel {
    protected static final List<ParameterDefinition> COMMON_PARAMS = List.of(
            new ParameterDefinition("numSignatures", "number", null,MIN_SIGNATURES, MIN_SIGNATURES, MAX_SIGNATURES, "Executed Signatures Verifications count")
    );

    // Returns the service name of the API
    public abstract String getService();

    public String getMethodName() {
        throw  new UnsupportedOperationException("Not supported yet.");
    }

    // Returns the description of the API
    public abstract String getDescription();

    protected abstract List<ParameterDefinition> apiSpecificParams();

    // Get the list of parameters that are relevant for the specified API
    public List<ParameterDefinition> getParameters() {
        List<ParameterDefinition> merged = new ArrayList<>(COMMON_PARAMS);
        merged.addAll(apiSpecificParams());
        return merged;
    }

    // Check the values of parameters based on the specified API
    public FeeCheckResult checkParameters(Map<String, Object> values) {
        for (ParameterDefinition p : getParameters()) {
            if ("number".equals(p.type)) {
                try {
                    Object value = values.get(p.name);
                    if (value == null) {
                        continue;
                    }
                    int val = (int) value;
                    if (val < p.min || val > p.max) {
                        return FeeCheckResult.failure("Parameter " + p.name + " must be in range [" + p.min + ", " + p.max + "]");
                    }
                } catch (ClassCastException ex) {
                    return FeeCheckResult.failure("Parameter " + p.name + " must be a number");
                }
            }
        }
        return FeeCheckResult.success();
    }

    // Compute the fee. There are 2 parts to the fee. There's the API specific fee (e.g. cryptoCreate price is based on the number of keys), and there's fee for parameters that are common across all APIs (e.g. number of signatures)
    public Fees computeFee(Map<String, Object> values, ExchangeRate exchangeRate, AbstractFeesSchedule feesSchedule) {
        checkParameters(values);
        System.out.println("params are " + values);
        final List<String> serviceExtras = feesSchedule.getServiceExtras(this.getMethodName());
        for (String key : serviceExtras) {
            if (!values.containsKey(key)) {
                System.err.println("input params missing " + key + " required by method " + this.getMethodName());
            }
        }

        preprocessEnumValues(values);
        //  get base fee for the service
        var result = computeApiSpecificFee(values, feesSchedule);

        //  calculate the node fee
        final var node_base_fee = feesSchedule.getNodeBaseFee();
        long total_node_fee = node_base_fee;
        result.details.put("Node Base Fee", new FeeResult.FeeDetail(1, node_base_fee));
        final List<String> extras = feesSchedule.getNodeExtraNames();
        for(var extra : extras) {
            final var node_bytes_fee = feesSchedule.getExtrasFee(extra);
            final long node_bytes_included = feesSchedule.getNodeExtraIncludedCount(extra);
            System.out.println("Node Extra: " + extra + " costs " + node_bytes_fee+ " each, included " + node_bytes_included);
            System.out.println("using " + values.get(extra) + " of " + extra);
            final long used = (long) values.get(extra);
            if (used > node_bytes_included) {
                final long additional_bytes = used - node_bytes_included;
                System.out.println("overage " + additional_bytes);
                final long extras_fee = additional_bytes * node_bytes_fee;
                System.out.println("overage total fee " + extras_fee);
                total_node_fee += extras_fee;
                result.details.put("Node Bytes Overage", new FeeResult.FeeDetail(additional_bytes, extras_fee));
            }
        }
        final long total_network_fee = total_node_fee * feesSchedule.getNetworkMultiplier();
        System.out.println("total node fee: " + total_node_fee);
        System.out.println("total network fee: " + total_network_fee);
        System.out.println("total node fee: " + result.service);
        result.details.put("Network Fee ", new FeeResult.FeeDetail(1, total_network_fee));

        //TODO: I'm pretty sure these calculations are wrong
//        final var nodeTc = this.tinyCentsToTinyBar(this.usdToTinycents(result.node), exchangeRate);
//        final var networkTc = this.tinyCentsToTinyBar(this.usdToTinycents(result.network*0.5), exchangeRate);
//        final var serviceTc = this.tinyCentsToTinyBar(this.usdToTinycents(result.network*0.5), exchangeRate);
//        return base_fee + total_node_fee + service_fee;
        final var fees =  new Fees(total_node_fee, total_network_fee, result.service, result.service + result.node + result.network, result.details);
        System.out.println("Fees: " + this.getService() + ":" + this.getDescription()+":\n  " + fees);
        return fees;
    }
    private long tinyCentsToTinyBar(long tcents, ExchangeRate cr) {
        final var currentRate = fromPbj(cr);
        return tcents* currentRate.getHbarEquiv()/currentRate.getCentEquiv() * 100;
    }

    private long usdToTinycents(double v) {
        return Math.round(v * 100_000_000L);
    }

    // Compute API specific fee (e.g. cryptoCreate price is based on the number of keys)
    protected abstract FeeResult computeApiSpecificFee(Map<String, Object> values, AbstractFeesSchedule feesSchedule);

    @SuppressWarnings("unchecked")
    private void preprocessEnumValues(Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object val = entry.getValue();

            if (val instanceof String strVal) {
                try {
                    if (strVal.equalsIgnoreCase("YES") || strVal.equalsIgnoreCase("NO")) {
                        entry.setValue(YesOrNo.valueOf(strVal.toUpperCase()));
                    } else if (strVal.equalsIgnoreCase("Fungible") || strVal.equalsIgnoreCase("NonFungible")) {
                        entry.setValue(FTOrNFT.valueOf(strVal));
                    } else if (strVal.equalsIgnoreCase("Associate") || strVal.equalsIgnoreCase("Dissociate")) {
                        entry.setValue(AssociateOrDissociate.valueOf(strVal));
                    }
                } catch (IllegalArgumentException e) {
                    // Ignore or log invalid enums if desired
                }
            }
        }
    }
}


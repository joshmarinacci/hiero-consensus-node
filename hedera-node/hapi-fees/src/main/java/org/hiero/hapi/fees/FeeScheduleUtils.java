// SPDX-License-Identifier: Apache-2.0
package org.hiero.hapi.fees;

import com.hedera.hapi.node.base.HederaFunctionality;
import org.hiero.hapi.support.fees.Extra;
import org.hiero.hapi.support.fees.ExtraFeeDefinition;
import org.hiero.hapi.support.fees.ExtraFeeReference;
import org.hiero.hapi.support.fees.FeeSchedule;
import org.hiero.hapi.support.fees.Service;
import org.hiero.hapi.support.fees.ServiceFee;

public class FeeScheduleUtils {
    public static ExtraFeeDefinition makeExtraDef(Extra extra, long fee) {
        return ExtraFeeDefinition.newBuilder().name(extra).fee(fee).build();
    }

    public static ExtraFeeReference makeExtraIncluded(Extra extra, int included) {
        return ExtraFeeReference.DEFAULT
                .copyBuilder()
                .name(extra)
                .includedCount(included)
                .build();
    }

    public static ServiceFee makeServiceFee(HederaFunctionality name, long baseFee, ExtraFeeReference... reference) {
        return ServiceFee.DEFAULT
                .copyBuilder()
                .name(name)
                .baseFee(baseFee)
                .extras(reference)
                .build();
    }

    public static Service makeService(String name, ServiceFee... services) {
        return Service.DEFAULT.copyBuilder().name(name).transactions(services).build();
    }

    public static ExtraFeeDefinition lookupExtraFee(FeeSchedule feeSchedule, ExtraFeeReference ref) {
        for (ExtraFeeDefinition def : feeSchedule.definedExtras()) {
            if (def.name().equals(ref.name())) {
                return def;
            }
        }
        throw new Error("Extra Fee definition not found for " + ref.name());
    }

    public static ServiceFee lookupServiceFee(FeeSchedule feeSchedule, HederaFunctionality api) {
        for (Service service : feeSchedule.serviceFees()) {
            for (ServiceFee trans : service.transactions()) {
                if (trans.name() == api) {
                    return trans;
                }
            }
        }
        throw new Error("Service definition not found for " + api.toString());
    }

    /**
     * Validate the fee schedule. There must be
     * no
     * @param feeSchedule
     */
    public static boolean validate(FeeSchedule feeSchedule) {
        //        System.out.println("validating " + feeSchedule);
        for (ExtraFeeDefinition def : feeSchedule.definedExtras()) {
            // no negative values or greater than MAX long
            if (def.fee() < 0) {
                return false;
            }
            if (def.fee() > Long.MAX_VALUE) {
                return false;
            }
        }

        // all referenced extras are defined
        for (Service service : feeSchedule.serviceFees()) {
            for (ServiceFee trans : service.transactions()) {
                for (ExtraFeeReference ref : trans.extras()) {
                    try {
                        lookupExtraFee(feeSchedule, ref);
                    } catch (Error e) {
                        return false;
                    }
                }
            }
        }
        for (ExtraFeeReference ref : feeSchedule.nodeFee().extras()) {
            try {
                lookupExtraFee(feeSchedule, ref);
            } catch (Error e) {
                return false;
            }
        }
        return true;
    }
}

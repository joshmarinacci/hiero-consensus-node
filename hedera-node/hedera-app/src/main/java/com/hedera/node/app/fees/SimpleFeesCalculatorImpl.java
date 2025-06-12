package com.hedera.node.app.fees;


import static com.hedera.hapi.node.transaction.TransactionBody.DataOneOfType.CONSENSUS_CREATE_TOPIC;
import static com.hedera.hapi.util.HapiUtils.countOfCryptographicKeys;
import static com.hedera.hapi.util.HapiUtils.functionOf;
import static com.hedera.node.app.hapi.utils.CommonPbjConverters.fromPbj;
import static java.util.Objects.requireNonNull;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.hapi.node.base.Key;
import com.hedera.hapi.node.transaction.TransactionBody;
import com.hedera.node.app.hapi.fees.FeeResult;
import com.hedera.node.app.hapi.fees.apis.common.EntityCreate;
import com.hedera.node.app.hapi.fees.apis.common.FeesHelper;
import com.hedera.node.app.hapi.fees.apis.common.YesOrNo;
import com.hedera.node.app.hapi.fees.usage.SigUsage;
import com.hedera.node.app.hapi.utils.fee.SigValueObj;
import com.hedera.node.app.spi.fees.FeeCalculator;
import com.hedera.node.app.spi.fees.Fees;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


public class SimpleFeesCalculatorImpl implements FeeCalculator {
    private final SigUsage sigUsage;
    private final TransactionBody txBody;
    private final FeeContextImpl feeContext;

    SimpleFeesCalculatorImpl(
            FeeContextImpl feeContext,
            @NonNull TransactionBody txBody,
            @NonNull Key payerKey,
            final int numVerifications,
            final int signatureMapSize
//            @NonNull final com.hedera.hapi.node.base.FeeData feeData,
//            @NonNull final ExchangeRate currentRate,
//            final boolean isInternalDispatch,
//            final CongestionMultipliers congestionMultipliers,
//            final ReadableStoreFactory storeFactory
    ) {
        requireNonNull(txBody);
        this.feeContext = feeContext;
        System.out.println("creating a simple fees calculator 1");
        sigUsage = new SigUsage(numVerifications, signatureMapSize, countOfCryptographicKeys(payerKey));
        System.out.println("the txn body is " + txBody);
        System.out.println("kind is " + txBody.data().kind());
        this.txBody = txBody;
    }
    @Override
    public @NonNull FeeCalculator withResourceUsagePercent(double percent) {
        return this;
    }

    @Override
    public @NonNull FeeCalculator addBytesPerTransaction(long bytes) {
        return this;
    }

    @Override
    public @NonNull FeeCalculator addStorageBytesSeconds(long seconds) {
        return this;
    }

    @Override
    public @NonNull FeeCalculator addNetworkRamByteSeconds(long amount) {
        return this;
    }

    @Override
    public @NonNull FeeCalculator addRamByteSeconds(long amount) {
        return this;
    }

    @Override
    public @NonNull FeeCalculator addVerificationsPerTransaction(long amount) {
        return this;
    }

    @NonNull
    @Override
    public Fees legacyCalculate(@NonNull Function<SigValueObj, com.hederahashgraph.api.proto.java.FeeData> callback) {
        System.out.println("SimpleFeesCalculatorImpl.legacyCalculate() called");
        final var sigValueObject = new SigValueObj(sigUsage.numSigs(), sigUsage.numPayerKeys(), sigUsage.sigsSize());
//        System.out.println("sig value object is " + sigValueObject);
        final var matrix = callback.apply(sigValueObject);
//        System.out.println("matrix is " + matrix);

        if(this.txBody.data().kind() == CONSENSUS_CREATE_TOPIC) {
            System.out.println("consensus create topic");
            EntityCreate entity = FeesHelper.makeEntity(HederaFunctionality.CONSENSUS_CREATE_TOPIC, "Create a topic", 0, true);
            Map<String, Object> params = new HashMap<>();
            params.put("numSignatures", feeContext.numTxnSignatures());
            params.put("numKeys", 0);
            params.put("hasCustomFee", YesOrNo.NO);
            FeeResult simpleFee = entity.computeFee(params);
            return new Fees(0,0,0, simpleFee.fee);
        }
        return new Fees(0,0,0, 0);
    }

    @Override
    public @NonNull Fees calculate() {
        return new Fees(0,0,0);
    }

    @Override
    public long getCongestionMultiplier() {
        return 0;
    }

    @Override
    public @NonNull FeeCalculator resetUsage() {
        return this;
    }
}

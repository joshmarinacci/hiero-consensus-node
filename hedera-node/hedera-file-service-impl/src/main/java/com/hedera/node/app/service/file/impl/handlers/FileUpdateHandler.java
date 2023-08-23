/*
 * Copyright (C) 2022-2023 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hedera.node.app.service.file.impl.handlers;

import static com.hedera.hapi.node.base.ResponseCodeEnum.AUTORENEW_DURATION_NOT_IN_RANGE;
import static com.hedera.hapi.node.base.ResponseCodeEnum.FILE_DELETED;
import static com.hedera.hapi.node.base.ResponseCodeEnum.INVALID_FILE_ID;
import static com.hedera.hapi.node.base.ResponseCodeEnum.MAX_FILE_SIZE_EXCEEDED;
import static com.hedera.hapi.node.base.ResponseCodeEnum.UNAUTHORIZED;
import static com.hedera.node.app.service.file.impl.utils.FileServiceUtils.preValidate;
import static com.hedera.node.app.service.file.impl.utils.FileServiceUtils.validateAndAddRequiredKeys;
import static com.hedera.node.app.service.mono.pbj.PbjConverter.fromPbj;
import static com.hedera.node.app.spi.workflows.HandleException.validateFalse;
import static com.hedera.node.app.spi.workflows.HandleException.validateTrue;
import static java.util.Objects.requireNonNull;

import com.hedera.hapi.node.base.FileID;
import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.hapi.node.base.SubType;
import com.hedera.hapi.node.base.Timestamp;
import com.hedera.hapi.node.file.FileUpdateTransactionBody;
import com.hedera.hapi.node.state.file.File;
import com.hedera.node.app.hapi.fees.usage.file.FileOpsUsage;
import com.hedera.node.app.service.file.ReadableFileStore;
import com.hedera.node.app.service.file.impl.WritableFileStore;
import com.hedera.node.app.service.file.impl.WritableUpgradeFileStore;
import com.hedera.node.app.service.mono.fees.calculation.file.txns.FileUpdateResourceUsage;
import com.hedera.node.app.spi.validation.AttributeValidator;
import com.hedera.node.app.spi.workflows.HandleContext;
import com.hedera.node.app.spi.workflows.HandleException;
import com.hedera.node.app.spi.workflows.PreCheckException;
import com.hedera.node.app.spi.workflows.PreHandleContext;
import com.hedera.node.app.spi.workflows.TransactionHandler;
import com.hedera.node.config.data.EntitiesConfig;
import com.hedera.node.config.data.FilesConfig;
import com.hederahashgraph.api.proto.java.Duration;
import edu.umd.cs.findbugs.annotations.NonNull;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This class contains all workflow-related functionality regarding {@link HederaFunctionality#FILE_UPDATE}.
 */
@Singleton
public class FileUpdateHandler implements TransactionHandler {
    private static final Timestamp EXPIRE_NEVER =
            Timestamp.newBuilder().seconds(Long.MAX_VALUE - 1).build();
    private final FileOpsUsage fileOpsUsage;

    @Inject
    public FileUpdateHandler(final FileOpsUsage fileOpsUsage) {
        this.fileOpsUsage = fileOpsUsage;
    }

    /**
     * This method is called during the pre-handle workflow.
     *
     * <p>Determines signatures needed for update a file
     *
     * @param context the {@link PreHandleContext} which collects all information that will be
     *                passed to {@code #handle()}
     * @throws PreCheckException if any issue happens on the pre handle level
     */
    @Override
    public void preHandle(@NonNull final PreHandleContext context) throws PreCheckException {
        requireNonNull(context);
        final var transactionBody = context.body().fileUpdateOrThrow();
        final var fileStore = context.createStore(ReadableFileStore.class);
        preValidate(transactionBody.fileID(), fileStore, context, false);

        var file = fileStore.getFileLeaf(transactionBody.fileID());
        validateAndAddRequiredKeys(file.orElse(null), transactionBody.keys(), context);
    }

    @Override
    public void handle(@NonNull final HandleContext handleContext) throws HandleException {
        requireNonNull(handleContext);

        final var fileStore = handleContext.writableStore(WritableFileStore.class);
        final var fileUpdate = handleContext.body().fileUpdateOrThrow();

        final var fileServiceConfig = handleContext.configuration().getConfigData(FilesConfig.class);

        if (fileUpdate.fileID() == null) {
            throw new HandleException(INVALID_FILE_ID);
        }

        // the update file always will be for the node, not a particular ledger that's why we just compare the fileNum
        // and ignore shard and realm
        if (fileUpdate.fileIDOrThrow().fileNum() == fileServiceConfig.upgradeFileNumber()) {
            handleUpdateUpgradeFile(fileUpdate, handleContext);
            return;
        }

        final var maybeFile = fileStore.get(fileUpdate.fileIDOrElse(FileID.DEFAULT));
        if (maybeFile.isEmpty()) {
            throw new HandleException(INVALID_FILE_ID);
        }

        final var file = maybeFile.get();
        validateFalse(file.deleted(), FILE_DELETED);

        final var fees = handleContext.feeCalculator(SubType.DEFAULT).legacyCalculate(sigValueObj -> {
            return new FileUpdateResourceUsage(fileOpsUsage)
                    .usageGiven(fromPbj(handleContext.body()), sigValueObj, fromPbj(file));
        });

        handleContext.feeAccumulator().charge(handleContext.payer(), fees);

        // First validate this file is mutable; and the pending mutations are allowed
        // TODO: add or condition for privilege accounts from context
        validateFalse(file.keys() == null, UNAUTHORIZED);

        validateMaybeNewMemo(handleContext.attributeValidator(), fileUpdate);
        validateExpirationTime(fileUpdate, file, handleContext);

        // Now we apply the mutations to a builder
        final var builder = new File.Builder();
        // But first copy over the immutable topic attributes to the builder
        builder.fileId(file.fileId());
        builder.deleted(file.deleted());

        // And then resolve mutable attributes, and put the new topic back
        resolveMutableBuilderAttributes(fileUpdate, builder, fileServiceConfig, file);
        fileStore.put(builder.build());
    }

    private void handleUpdateUpgradeFile(FileUpdateTransactionBody fileUpdate, HandleContext handleContext) {
        final var fileStore = handleContext.writableStore(WritableUpgradeFileStore.class);
        // empty old upgrade file
        fileStore.resetFileContents();
        final var file = new File.Builder()
                .fileId(FileID.newBuilder()
                        .fileNum(fileUpdate.fileIDOrThrow().fileNum())
                        .build())
                .contents(fileUpdate.contents())
                .deleted(false)
                .expirationSecond(fileUpdate.expirationTimeOrElse(EXPIRE_NEVER).seconds())
                .memo(fileUpdate.memo())
                .build();
        fileStore.add(file);
    }

    private void resolveMutableBuilderAttributes(
            @NonNull final FileUpdateTransactionBody op,
            @NonNull final File.Builder builder,
            @NonNull final FilesConfig fileServiceConfig,
            @NonNull final File file) {
        if (op.hasKeys()) {
            builder.keys(op.keys());
        } else {
            builder.keys(file.keys());
        }
        var contentLength = op.contents().length();
        if (contentLength > 0) {
            if (contentLength > fileServiceConfig.maxSizeKb() * 1024L) {
                throw new HandleException(MAX_FILE_SIZE_EXCEEDED);
            }
            builder.contents(op.contents());
        } else {
            builder.contents(file.contents());
        }

        if (op.hasMemo()) {
            builder.memo(op.memo());
        } else {
            builder.memo(file.memo());
        }

        if (op.hasExpirationTime() && op.expirationTime().seconds() > file.expirationSecond()) {
            builder.expirationSecond(op.expirationTime().seconds());
        } else {
            builder.expirationSecond(file.expirationSecond());
        }
    }

    private void validateExpirationTime(FileUpdateTransactionBody op, File file, HandleContext handleContext) {
        if (op.hasExpirationTime()) {
            final var effectiveDuration = Duration.newBuilder()
                    .setSeconds(op.expirationTime().seconds() - file.expirationSecond())
                    .build();
            final var maxEntityLifetime = handleContext
                    .configuration()
                    .getConfigData(EntitiesConfig.class)
                    .maxLifetime();
            final var now = handleContext.consensusNow().getEpochSecond();
            final var expiryGivenMaxLifetime = now + maxEntityLifetime;
            validateTrue(
                    effectiveDuration.getSeconds() > now && effectiveDuration.getSeconds() <= expiryGivenMaxLifetime,
                    AUTORENEW_DURATION_NOT_IN_RANGE);
        }
    }

    public static boolean wantsToMutateNonExpiryField(@NonNull final FileUpdateTransactionBody op) {
        return op.hasMemo() || op.hasKeys();
    }

    private void validateMaybeNewMemo(
            @NonNull final AttributeValidator attributeValidator, @NonNull final FileUpdateTransactionBody op) {
        if (op.hasMemo()) {
            attributeValidator.validateMemo(op.memo());
        }
    }
}

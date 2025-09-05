// SPDX-License-Identifier: Apache-2.0
package com.hedera.node.app.blocks.impl.streaming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.hedera.node.app.blocks.impl.streaming.BlockNodeConnection.ConnectionState;
import com.hedera.node.app.metrics.BlockStreamMetrics;
import com.hedera.node.config.ConfigProvider;
import com.hedera.node.internal.network.BlockNodeConfig;
import com.hedera.pbj.runtime.OneOf;
import com.hedera.pbj.runtime.grpc.Pipeline;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hiero.block.api.BlockStreamPublishServiceInterface.BlockStreamPublishServiceClient;
import org.hiero.block.api.PublishStreamRequest;
import org.hiero.block.api.PublishStreamRequest.EndStream;
import org.hiero.block.api.PublishStreamResponse;
import org.hiero.block.api.PublishStreamResponse.EndOfStream;
import org.hiero.block.api.PublishStreamResponse.EndOfStream.Code;
import org.hiero.block.api.PublishStreamResponse.ResponseOneOfType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockNodeConnectionTest extends BlockNodeCommunicationTestBase {
    private static final long ONCE_PER_DAY_MILLIS = Duration.ofHours(24).toMillis();
    private static final VarHandle isStreamingEnabledHandle;

    static {
        try {
            final Lookup lookup = MethodHandles.lookup();
            isStreamingEnabledHandle = MethodHandles.privateLookupIn(BlockNodeConnectionManager.class, lookup)
                    .findVarHandle(BlockNodeConnectionManager.class, "isStreamingEnabled", AtomicBoolean.class);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BlockNodeConnection connection;
    private BlockNodeConfig nodeConfig;

    private BlockNodeConnectionManager connectionManager;
    private BlockBufferService bufferService;
    private BlockStreamPublishServiceClient grpcServiceClient;
    private BlockStreamMetrics metrics;
    private Pipeline<? super PublishStreamRequest> requestPipeline;
    private ScheduledExecutorService executorService;

    @BeforeEach
    void beforeEach() {
        final ConfigProvider configProvider = createConfigProvider();
        nodeConfig = newBlockNodeConfig(8080, 1);
        connectionManager = mock(BlockNodeConnectionManager.class);
        bufferService = mock(BlockBufferService.class);
        grpcServiceClient = mock(BlockStreamPublishServiceClient.class);
        metrics = mock(BlockStreamMetrics.class);
        requestPipeline = mock(Pipeline.class);
        executorService = mock(ScheduledExecutorService.class);

        connection = new BlockNodeConnection(
                configProvider,
                nodeConfig,
                connectionManager,
                bufferService,
                grpcServiceClient,
                metrics,
                executorService);

        lenient().doReturn(requestPipeline).when(grpcServiceClient).publishBlockStream(connection);
    }

    @Test
    void testCreateRequestPipeline() {
        assertThat(connection.getConnectionState()).isEqualTo(ConnectionState.UNINITIALIZED);

        connection.createRequestPipeline();

        assertThat(connection.getConnectionState()).isEqualTo(ConnectionState.PENDING);
        verify(grpcServiceClient).publishBlockStream(connection);
    }

    @Test
    void testCreateRequestPipeline_alreadyExists() {
        connection.createRequestPipeline();
        connection.createRequestPipeline();

        verify(grpcServiceClient).publishBlockStream(connection); // should only be called once
        verifyNoMoreInteractions(grpcServiceClient);
    }

    @Test
    void testUpdatingConnectionState() {
        final ConnectionState preUpdateState = connection.getConnectionState();
        // this should be uninitialized because we haven't called connect yet
        assertThat(preUpdateState).isEqualTo(ConnectionState.UNINITIALIZED);
        connection.updateConnectionState(ConnectionState.ACTIVE);

        // Verify task was scheduled to periodically reset the stream
        verify(executorService)
                .scheduleAtFixedRate(
                        any(Runnable.class),
                        eq(ONCE_PER_DAY_MILLIS), // initial delay
                        eq(ONCE_PER_DAY_MILLIS), // period
                        eq(TimeUnit.MILLISECONDS));

        final ConnectionState postUpdateState = connection.getConnectionState();
        assertThat(postUpdateState).isEqualTo(ConnectionState.ACTIVE);
    }

    @Test
    void testHandleStreamError() {
        openConnectionAndResetMocks();
        connection.updateConnectionState(ConnectionState.ACTIVE);

        // Verify task was scheduled to periodically reset the stream
        verify(executorService)
                .scheduleAtFixedRate(
                        any(Runnable.class),
                        eq(ONCE_PER_DAY_MILLIS), // initial delay
                        eq(ONCE_PER_DAY_MILLIS), // period
                        eq(TimeUnit.MILLISECONDS));

        // do a quick sanity check on the state
        final ConnectionState preState = connection.getConnectionState();
        assertThat(preState).isEqualTo(ConnectionState.ACTIVE);

        connection.handleStreamFailure();

        final ConnectionState postState = connection.getConnectionState();
        assertThat(postState).isEqualTo(ConnectionState.UNINITIALIZED);

        verify(requestPipeline).onComplete();
        verify(connectionManager).rescheduleAndSelectNewNode(connection, Duration.ofSeconds(30));
        verify(connectionManager).jumpToBlock(-1L);
        verifyNoMoreInteractions(requestPipeline);
        verifyNoMoreInteractions(connectionManager);
    }

    @Test
    void testOnNext_acknowledgement_notStreaming() {
        final PublishStreamResponse response = createBlockAckResponse(10L);
        when(connectionManager.currentStreamingBlockNumber())
                .thenReturn(-1L); // we aren't streaming anything to the block node

        connection.updateConnectionState(ConnectionState.ACTIVE);
        connection.onNext(response);

        verify(connectionManager).currentStreamingBlockNumber();
        verify(bufferService).getLastBlockNumberProduced();
        verify(connectionManager).updateLastVerifiedBlock(connection.getNodeConfig(), 10L);
        verify(metrics).incrementAcknowledgedBlockCount();
        verifyNoMoreInteractions(metrics);
    }

    @Test
    void testOnNext_acknowledgement_olderThanCurrentStreamingAndProducing() {
        final PublishStreamResponse response = createBlockAckResponse(8L);

        when(connectionManager.currentStreamingBlockNumber()).thenReturn(10L);
        when(bufferService.getLastBlockNumberProduced()).thenReturn(10L);

        connection.updateConnectionState(ConnectionState.ACTIVE);
        connection.onNext(response);

        verify(connectionManager).currentStreamingBlockNumber();
        verify(bufferService).getLastBlockNumberProduced();

        verify(connectionManager).updateLastVerifiedBlock(connection.getNodeConfig(), 8L);
        verify(metrics).incrementAcknowledgedBlockCount();
        verifyNoMoreInteractions(connectionManager);
        verifyNoMoreInteractions(bufferService);
        verifyNoMoreInteractions(metrics);
    }

    @Test
    void testOnNext_acknowledgement_newerThanCurrentProducing() {
        // I don't think this scenario is possible... we should never stream a block that is newer than the block
        // currently being produced.
        final PublishStreamResponse response = createBlockAckResponse(11L);

        when(connectionManager.currentStreamingBlockNumber()).thenReturn(11L);
        when(bufferService.getLastBlockNumberProduced()).thenReturn(10L);

        connection.updateConnectionState(ConnectionState.ACTIVE);
        connection.onNext(response);

        verify(connectionManager).currentStreamingBlockNumber();
        verify(bufferService).getLastBlockNumberProduced();
        verify(connectionManager).updateLastVerifiedBlock(connection.getNodeConfig(), 11L);
        verify(connectionManager).jumpToBlock(12L);
        verify(metrics).incrementAcknowledgedBlockCount();
        verifyNoMoreInteractions(connectionManager);
        verifyNoMoreInteractions(metrics);
    }

    @Test
    void testOnNext_acknowledgement_newerThanCurrentStreaming() {
        final PublishStreamResponse response = createBlockAckResponse(11L);

        when(connectionManager.currentStreamingBlockNumber()).thenReturn(10L);
        when(bufferService.getLastBlockNumberProduced()).thenReturn(12L);

        connection.updateConnectionState(ConnectionState.ACTIVE);
        connection.onNext(response);

        verify(connectionManager).currentStreamingBlockNumber();
        verify(bufferService).getLastBlockNumberProduced();
        verify(connectionManager).updateLastVerifiedBlock(connection.getNodeConfig(), 11L);
        verify(connectionManager).jumpToBlock(12L);
        verify(metrics).incrementAcknowledgedBlockCount();
        verifyNoMoreInteractions(metrics);
    }

    @Test
    void testScheduleStreamResetTask() {
        openConnectionAndResetMocks();
        connection.updateConnectionState(ConnectionState.ACTIVE);

        // Verify task was scheduled to periodically reset the stream
        verify(executorService)
                .scheduleAtFixedRate(
                        any(Runnable.class),
                        eq(ONCE_PER_DAY_MILLIS), // initial delay
                        eq(ONCE_PER_DAY_MILLIS), // period
                        eq(TimeUnit.MILLISECONDS));

        verifyNoMoreInteractions(executorService);
        verifyNoInteractions(metrics);
        verifyNoInteractions(bufferService);
    }

    @ParameterizedTest
    @EnumSource(
            value = EndOfStream.Code.class,
            names = {"ERROR", "PERSISTENCE_FAILED"})
    void testOnNext_endOfStream_blockNodeInternalError(final EndOfStream.Code responseCode) {
        openConnectionAndResetMocks();
        connection.updateConnectionState(ConnectionState.ACTIVE);

        final PublishStreamResponse response = createEndOfStreamResponse(responseCode, 10L);
        connection.onNext(response);

        verify(metrics).incrementEndOfStreamCount(responseCode);
        verify(requestPipeline).onComplete();
        verify(connectionManager).jumpToBlock(-1L);
        verify(connectionManager).rescheduleAndSelectNewNode(connection, Duration.ofSeconds(30));
        verify(connectionManager).updateLastVerifiedBlock(connection.getNodeConfig(), 10L);
        verifyNoMoreInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
    }

    @ParameterizedTest
    @EnumSource(
            value = EndOfStream.Code.class,
            names = {"TIMEOUT", "DUPLICATE_BLOCK", "BAD_BLOCK_PROOF", "INVALID_REQUEST"})
    void testOnNext_endOfStream_clientFailures(final EndOfStream.Code responseCode) {
        openConnectionAndResetMocks();
        connection.updateConnectionState(ConnectionState.ACTIVE);

        final PublishStreamResponse response = createEndOfStreamResponse(responseCode, 10L);
        connection.onNext(response);

        verify(metrics).incrementEndOfStreamCount(responseCode);
        verify(requestPipeline).onComplete();
        verify(connectionManager).jumpToBlock(-1L);
        verify(connectionManager).scheduleConnectionAttempt(connection, Duration.ofSeconds(1), 11L);
        verify(connectionManager).updateLastVerifiedBlock(connection.getNodeConfig(), 10L);
        verifyNoMoreInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
    }

    @Test
    void testOnNext_endOfStream_blockNodeGracefulShutdown() {
        openConnectionAndResetMocks();
        // STREAM_ITEMS_SUCCESS is sent when the block node is gracefully shutting down
        final PublishStreamResponse response = createEndOfStreamResponse(Code.SUCCESS, 10L);
        connection.updateConnectionState(ConnectionState.ACTIVE);
        connection.onNext(response);

        verify(metrics).incrementEndOfStreamCount(Code.SUCCESS);
        verify(requestPipeline).onComplete();
        verify(connectionManager).jumpToBlock(-1L);
        verify(connectionManager).updateLastVerifiedBlock(connection.getNodeConfig(), 10L);
        verify(connectionManager).rescheduleAndSelectNewNode(connection, Duration.ofSeconds(30));
        verifyNoMoreInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
    }

    @Test
    void testOnNext_endOfStream_blockNodeBehind_blockExists() {
        openConnectionAndResetMocks();
        final PublishStreamResponse response = createEndOfStreamResponse(Code.BEHIND, 10L);
        when(bufferService.getBlockState(11L)).thenReturn(new BlockState(11L));
        connection.updateConnectionState(ConnectionState.ACTIVE);

        connection.onNext(response);

        verify(metrics).incrementEndOfStreamCount(Code.BEHIND);
        verify(requestPipeline).onComplete();
        verify(connectionManager).jumpToBlock(-1L);
        verify(connectionManager).scheduleConnectionAttempt(connection, Duration.ofSeconds(1), 11L);
        verify(bufferService).getBlockState(11L);
        verify(connectionManager).updateLastVerifiedBlock(connection.getNodeConfig(), 10L);
        verifyNoMoreInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
    }

    @Test
    void testOnNext_endOfStream_blockNodeBehind_blockDoesNotExist() {
        openConnectionAndResetMocks();
        final PublishStreamResponse response = createEndOfStreamResponse(Code.BEHIND, 10L);
        when(bufferService.getBlockState(11L)).thenReturn(null);

        connection.updateConnectionState(ConnectionState.ACTIVE);
        connection.onNext(response);

        verify(metrics).incrementEndOfStreamCount(Code.BEHIND);
        verify(bufferService, times(1)).getEarliestAvailableBlockNumber();
        verify(bufferService, times(1)).getHighestAckedBlockNumber();
        verify(bufferService).getBlockState(11L);
        verify(requestPipeline).onNext(createRequest(EndStream.Code.TOO_FAR_BEHIND));
        verify(requestPipeline).onComplete();
        verify(connectionManager).rescheduleAndSelectNewNode(connection, Duration.ofSeconds(30));
        verify(connectionManager).jumpToBlock(-1L);
        verify(connectionManager).updateLastVerifiedBlock(connection.getNodeConfig(), 10L);
        verifyNoMoreInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
    }

    @Test
    void testOnNext_endOfStream_itemsUnknown() {
        openConnectionAndResetMocks();
        connection.updateConnectionState(ConnectionState.ACTIVE);

        final PublishStreamResponse response = createEndOfStreamResponse(Code.UNKNOWN, 10L);
        connection.onNext(response);

        verify(metrics).incrementEndOfStreamCount(Code.UNKNOWN);
        verify(requestPipeline).onComplete();
        verify(connectionManager).jumpToBlock(-1L);
        verify(connectionManager).rescheduleAndSelectNewNode(connection, Duration.ofSeconds(30));
        verify(connectionManager).updateLastVerifiedBlock(connection.getNodeConfig(), 10L);
        verifyNoMoreInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
    }

    @Test
    void testOnNext_skipBlock_sameAsStreaming() {
        final PublishStreamResponse response = createSkipBlock(25L);
        when(connectionManager.currentStreamingBlockNumber()).thenReturn(25L);
        connection.updateConnectionState(ConnectionState.ACTIVE);
        connection.onNext(response);

        verify(metrics).incrementSkipBlockCount();
        verify(connectionManager).jumpToBlock(26L); // jump to the response block number + 1
        verify(connectionManager).currentStreamingBlockNumber();
        verifyNoMoreInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
        verifyNoMoreInteractions(connectionManager);
        verifyNoInteractions(bufferService);
    }

    @Test
    void testOnNext_skipBlock_notSameAsStreaming() {
        final PublishStreamResponse response = createSkipBlock(25L);
        when(connectionManager.currentStreamingBlockNumber()).thenReturn(26L);
        connection.updateConnectionState(ConnectionState.ACTIVE);

        connection.onNext(response);

        verify(metrics).incrementSkipBlockCount();
        verify(connectionManager).currentStreamingBlockNumber();
        verifyNoMoreInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
        verifyNoMoreInteractions(connectionManager);
        verifyNoInteractions(bufferService);
    }

    @Test
    void testOnNext_resendBlock_blockExists() {
        final PublishStreamResponse response = createResendBlock(10L);
        when(bufferService.getBlockState(10L)).thenReturn(new BlockState(10L));

        connection.onNext(response);

        verify(metrics).incrementResendBlockCount();
        verify(connectionManager).jumpToBlock(10L);
        verify(bufferService).getBlockState(10L);
        verifyNoMoreInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
        verifyNoMoreInteractions(connectionManager);
        verifyNoMoreInteractions(bufferService);
    }

    @Test
    void testOnNext_resendBlock_blockDoesNotExist() {
        openConnectionAndResetMocks();

        final PublishStreamResponse response = createResendBlock(10L);
        when(bufferService.getBlockState(10L)).thenReturn(null);
        connection.updateConnectionState(ConnectionState.ACTIVE);

        connection.onNext(response);

        verify(metrics).incrementResendBlockCount();
        verify(requestPipeline).onComplete();
        verify(connectionManager).jumpToBlock(-1L);
        verify(connectionManager).rescheduleAndSelectNewNode(connection, Duration.ofSeconds(30));
        verify(bufferService).getBlockState(10L);
        verifyNoMoreInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
        verifyNoMoreInteractions(connectionManager);
        verifyNoMoreInteractions(bufferService);
    }

    @Test
    void testOnNext_unknown() {
        final PublishStreamResponse response = new PublishStreamResponse(new OneOf<>(ResponseOneOfType.UNSET, null));
        connection.updateConnectionState(ConnectionState.ACTIVE);
        connection.onNext(response);

        verify(metrics).incrementUnknownResponseCount();

        verifyNoMoreInteractions(metrics);
        verifyNoInteractions(requestPipeline);
        verifyNoInteractions(connectionManager);
        verifyNoInteractions(bufferService);
    }

    @Test
    void testSendRequest() {
        openConnectionAndResetMocks();

        final PublishStreamRequest request = createRequest(newBlockHeaderItem());

        connection.updateConnectionState(ConnectionState.ACTIVE);
        connection.sendRequest(request);

        verify(requestPipeline).onNext(request);
        verifyNoInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
        verifyNoMoreInteractions(connectionManager);
        verifyNoInteractions(bufferService);
    }

    @Test
    void testSendRequest_notActive() {
        final PublishStreamRequest request = createRequest(newBlockHeaderItem());

        connection.createRequestPipeline();
        connection.updateConnectionState(ConnectionState.PENDING);
        connection.sendRequest(request);

        verifyNoInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
        verifyNoMoreInteractions(connectionManager);
        verifyNoInteractions(bufferService);
    }

    @Test
    void testSendRequest_observerNull() {
        final PublishStreamRequest request = createRequest(newBlockHeaderItem());

        // don't create the observer
        connection.updateConnectionState(ConnectionState.PENDING);
        connection.sendRequest(request);

        verifyNoInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
        verifyNoMoreInteractions(connectionManager);
        verifyNoInteractions(bufferService);
    }

    @Test
    void testClose() {
        openConnectionAndResetMocks();
        connection.updateConnectionState(ConnectionState.ACTIVE);

        // Verify task was scheduled to periodically reset the stream
        verify(executorService)
                .scheduleAtFixedRate(
                        any(Runnable.class),
                        eq(ONCE_PER_DAY_MILLIS), // initial delay
                        eq(ONCE_PER_DAY_MILLIS), // period
                        eq(TimeUnit.MILLISECONDS));

        connection.close(true);

        assertThat(connection.getConnectionState()).isEqualTo(ConnectionState.UNINITIALIZED);

        verify(connectionManager).jumpToBlock(-1L);
        verify(requestPipeline).onComplete();
        verifyNoInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
        verifyNoMoreInteractions(connectionManager);
        verifyNoInteractions(bufferService);
    }

    @Test
    void testClose_failure() {
        openConnectionAndResetMocks();
        connection.updateConnectionState(ConnectionState.ACTIVE);

        // Verify task was scheduled to periodically reset the stream
        verify(executorService)
                .scheduleAtFixedRate(
                        any(Runnable.class),
                        eq(ONCE_PER_DAY_MILLIS), // initial delay
                        eq(ONCE_PER_DAY_MILLIS), // period
                        eq(TimeUnit.MILLISECONDS));

        connection.close(true);

        assertThat(connection.getConnectionState()).isEqualTo(ConnectionState.UNINITIALIZED);

        verify(connectionManager).jumpToBlock(-1L);
        verify(requestPipeline).onComplete();
        verifyNoInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
        verifyNoMoreInteractions(connectionManager);
        verifyNoInteractions(bufferService);
    }

    @Test
    void testOnError() {
        openConnectionAndResetMocks();
        connection.updateConnectionState(ConnectionState.ACTIVE);

        connection.onError(new RuntimeException("oh bother"));

        assertThat(connection.getConnectionState()).isEqualTo(ConnectionState.UNINITIALIZED);

        verify(metrics).incrementOnErrorCount();

        verify(connectionManager).jumpToBlock(-1L);
        verify(requestPipeline).onComplete();
        verify(connectionManager).rescheduleAndSelectNewNode(connection, Duration.ofSeconds(30));
        verifyNoMoreInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
        verifyNoMoreInteractions(connectionManager);
        verifyNoInteractions(bufferService);
    }

    @Test
    void testOnCompleted_streamClosingInProgress() {
        openConnectionAndResetMocks();
        connection.close(true); // call this so we mark the connection as closing
        resetMocks();

        connection.onComplete();

        verifyNoInteractions(metrics);
        verifyNoInteractions(requestPipeline);
        verifyNoInteractions(connectionManager);
        verifyNoInteractions(bufferService);
    }

    @Test
    void testOnCompleted_streamClosingNotInProgress() {
        openConnectionAndResetMocks();
        connection.updateConnectionState(ConnectionState.ACTIVE);

        // don't call close so we do not mark the connection as closing
        connection.onComplete();

        verify(connectionManager).jumpToBlock(-1L);
        verify(requestPipeline).onComplete();
        verify(connectionManager).rescheduleAndSelectNewNode(connection, Duration.ofSeconds(30));
        verifyNoMoreInteractions(metrics);
        verifyNoMoreInteractions(requestPipeline);
        verifyNoMoreInteractions(connectionManager);
        verifyNoInteractions(bufferService);
    }

    // Utilities

    private void openConnectionAndResetMocks() {
        connection.createRequestPipeline();
        // reset the mocks interactions to remove tracked interactions as a result of starting the connection
        resetMocks();
    }

    private void resetMocks() {
        reset(connectionManager, requestPipeline, bufferService, metrics);
    }

    private AtomicBoolean isStreamingEnabled() {
        return (AtomicBoolean) isStreamingEnabledHandle.get(connectionManager);
    }
}

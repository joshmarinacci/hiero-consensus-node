// SPDX-License-Identifier: Apache-2.0
package org.hiero.otter.fixtures;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.time.Duration;

/**
 * Interface for performing asynchronous node actions with a specified timeout.
 */
public interface AsyncNodeActions {

    /**
     * Start the node with the configured timeout.
     *
     * @see Node#start()
     */
    void start();

    /**
     * Kill the node without prior cleanup with the configured timeout.
     *
     * @see Node#killImmediately()
     */
    void killImmediately();

    /**
     * Start a synthetic bottleneck on the node with a default delay of 100 milliseconds per round.
     *
     * @param delayPerRound the duration to sleep for each round handled by the execution layer
     */
    void startSyntheticBottleneck(@NonNull Duration delayPerRound);

    /**
     * Stop the synthetic bottleneck on the node.
     *
     * <p>This method will stop any ongoing synthetic bottleneck that was started by
     * {@link #startSyntheticBottleneck(Duration)}.
     */
    void stopSyntheticBottleneck();
}

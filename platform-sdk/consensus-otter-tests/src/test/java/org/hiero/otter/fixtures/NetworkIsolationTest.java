// SPDX-License-Identifier: Apache-2.0
package org.hiero.otter.fixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hiero.consensus.model.status.PlatformStatus.ACTIVE;

import com.swirlds.common.test.fixtures.WeightGenerators;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.hiero.otter.fixtures.network.Partition;
import org.hiero.otter.fixtures.turtle.TurtleTestEnvironment;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the node isolation functionality in the Network interface.
 */
class NetworkIsolationTest {

    private static final long RANDOM_SEED = 0L;

    /**
     * Provides a stream of test environments for the parameterized tests.
     *
     * @return a stream of {@link TestEnvironment} instances
     */
    public static Stream<TestEnvironment> environments() {
        return Stream.of(new TurtleTestEnvironment(RANDOM_SEED));
    }

    /**
     * Test isolating a single node from the network.
     *
     * @param env the test environment for this test
     */
    @ParameterizedTest
    @MethodSource("environments")
    void testIsolateSingleNode(@NonNull final TestEnvironment env) {
        try {
            final Network network = env.network();
            final TimeManager timeManager = env.timeManager();

            // Setup network with 4 nodes
            network.setWeightGenerator(WeightGenerators.BALANCED);
            final List<Node> nodes = network.addNodes(4);
            final Node node0 = nodes.get(0);
            final Node node1 = nodes.get(1);
            final Node node2 = nodes.get(2);
            final Node node3 = nodes.get(3);

            network.start();

            // Wait for nodes to stabilize
            timeManager.waitFor(Duration.ofSeconds(5));

            // Initially, no node should be isolated
            assertThat(network.isIsolated(node0)).isFalse();
            assertThat(network.isIsolated(node1)).isFalse();
            assertThat(network.isIsolated(node2)).isFalse();
            assertThat(network.isIsolated(node3)).isFalse();

            // Isolate node0
            final Partition isolationPartition = network.isolate(node0);

            // Verify the isolation was successful
            assertThat(isolationPartition).isNotNull();
            assertThat(isolationPartition.nodes()).containsExactly(node0);
            assertThat(isolationPartition.contains(node0)).isTrue();
            assertThat(isolationPartition.contains(node1)).isFalse();
            assertThat(isolationPartition.contains(node2)).isFalse();
            assertThat(isolationPartition.contains(node3)).isFalse();

            // Verify node0 is now isolated
            assertThat(network.isIsolated(node0)).isTrue();
            assertThat(network.isIsolated(node1)).isFalse();
            assertThat(network.isIsolated(node2)).isFalse();
            assertThat(network.isIsolated(node3)).isFalse();

            // Verify network knows about the isolation partition
            assertThat(network.getPartitionContaining(node0)).isEqualTo(isolationPartition);
            assertThat(network.partitions()).hasSize(2); // isolation + remaining nodes

            // Verify the remaining nodes are in a complementary partition
            final Partition remainingPartition = network.getPartitionContaining(node1);
            assertThat(remainingPartition).isNotNull().isNotEqualTo(isolationPartition);
            assertThat(remainingPartition.nodes()).containsExactlyInAnyOrder(node1, node2, node3);

            // Wait for nodes to become inactive due to network partition
            timeManager.waitForCondition(node0::isChecking, Duration.ofSeconds(15));
            timeManager.waitFor(Duration.ofSeconds(5)); // just to be sure
            assertThat(node1.platformStatus()).isEqualTo(ACTIVE);
            assertThat(node2.platformStatus()).isEqualTo(ACTIVE);
            assertThat(node3.platformStatus()).isEqualTo(ACTIVE);
        } finally {
            env.destroy();
        }
    }

    /**
     * Test rejoining an isolated node back to the network.
     *
     * @param env the test environment for this test
     */
    @ParameterizedTest
    @MethodSource("environments")
    void testRejoinIsolatedNode(@NonNull final TestEnvironment env) {
        try {
            final Network network = env.network();
            final TimeManager timeManager = env.timeManager();

            // Setup network with 4 nodes
            network.setWeightGenerator(WeightGenerators.BALANCED);
            final List<Node> nodes = network.addNodes(4);
            final Node node0 = nodes.get(0);
            final Node node1 = nodes.get(1);
            final Node node2 = nodes.get(2);
            final Node node3 = nodes.get(3);

            network.start();

            // Wait for nodes to stabilize
            timeManager.waitFor(Duration.ofSeconds(5));

            // Isolate node0
            network.isolate(node0);
            assertThat(network.isIsolated(node0)).isTrue();

            // Wait for nodes to become inactive due to isolation
            timeManager.waitForCondition(node0::isChecking, Duration.ofSeconds(15));

            // Rejoin the isolated node
            network.rejoin(node0);

            // Verify the node is no longer isolated
            assertThat(network.isIsolated(node0)).isFalse();
            assertThat(network.isIsolated(node1)).isFalse();
            assertThat(network.isIsolated(node2)).isFalse();
            assertThat(network.isIsolated(node3)).isFalse();

            // Verify all partitions are removed
            assertThat(network.partitions()).isEmpty();
            assertThat(network.getPartitionContaining(node0)).isNull();
            assertThat(network.getPartitionContaining(node1)).isNull();
            assertThat(network.getPartitionContaining(node2)).isNull();
            assertThat(network.getPartitionContaining(node3)).isNull();

            // Rejoining a network requires the RECONNECT capability.
            if (env.capabilities().contains(Capability.RECONNECT)) {
                // The nodes should be active again
                timeManager.waitForCondition(() -> network.allNodesInStatus(ACTIVE), Duration.ofSeconds(15));
            }
        } finally {
            env.destroy();
        }
    }

    /**
     * Test isolating multiple nodes sequentially.
     *
     * @param env the test environment for this test
     */
    @ParameterizedTest
    @MethodSource("environments")
    void testIsolateMultipleNodesSequentially(@NonNull final TestEnvironment env) {
        try {
            final Network network = env.network();
            final TimeManager timeManager = env.timeManager();

            // Setup network with 5 nodes
            network.setWeightGenerator(WeightGenerators.BALANCED);
            final List<Node> nodes = network.addNodes(4);
            final Node node0 = nodes.get(0);
            final Node node1 = nodes.get(1);
            final Node node2 = nodes.get(2);
            final Node node3 = nodes.get(3);

            network.start();

            // Wait for nodes to stabilize
            timeManager.waitFor(Duration.ofSeconds(5));

            // Isolate first node
            final Partition partition1 = network.isolate(node0);
            assertThat(network.isIsolated(node0)).isTrue();
            assertThat(partition1.nodes()).containsExactly(node0);
            assertThat(network.partitions()).hasSize(2); // isolated node + remaining nodes

            // Isolate second node from the remaining group
            final Partition partition2 = network.isolate(node1);
            assertThat(network.isIsolated(node0)).isTrue();
            assertThat(network.isIsolated(node1)).isTrue();
            assertThat(partition2.nodes()).containsExactly(node1);
            assertThat(network.partitions()).hasSize(3); // two isolated nodes + remaining group

            // Verify remaining nodes are still connected to each other
            final Partition remainingPartition = network.getPartitionContaining(node2);
            assertThat(remainingPartition).isNotNull();
            assertThat(remainingPartition.nodes()).containsExactlyInAnyOrder(node2, node3);
            assertThat(network.getPartitionContaining(node3)).isEqualTo(remainingPartition);

            // Verify isolation status of all nodes
            assertThat(network.isIsolated(node0)).isTrue();
            assertThat(network.isIsolated(node1)).isTrue();
            assertThat(network.isIsolated(node2)).isFalse();
            assertThat(network.isIsolated(node3)).isFalse();
        } finally {
            env.destroy();
        }
    }

    /**
     * Test isolating a node that is part of a partition.
     *
     * @param env the test environment for this test
     */
    @ParameterizedTest
    @MethodSource("environments")
    void testIsolateAlreadyPartitionedNode(@NonNull final TestEnvironment env) {
        try {
            final Network network = env.network();
            final TimeManager timeManager = env.timeManager();

            // Setup network with 5 nodes
            final List<Node> nodes = network.addNodes(4);
            final Node node0 = nodes.get(0);
            final Node node1 = nodes.get(1);
            final Node node2 = nodes.get(2);
            final Node node3 = nodes.get(3);

            network.start();

            // Wait for nodes to stabilize
            timeManager.waitFor(Duration.ofSeconds(5));

            // Create first partition
            final Partition partition1 = network.createPartition(node0, node1);
            assertThat(partition1.nodes()).containsExactlyInAnyOrder(node0, node1);
            assertThat(network.getPartitionContaining(node0)).isEqualTo(partition1);

            // Isolate node0 (should move it from partition)
            final Partition partition2 = network.isolate(node0);

            // Verify node0 moved to partition2
            assertThat(partition2.nodes()).containsExactlyInAnyOrder(node0);
            assertThat(network.getPartitionContaining(node0)).isEqualTo(partition2);

            // Verify partition1 no longer contains node0
            assertThat(partition1.nodes()).containsExactlyInAnyOrder(node1);
            assertThat(network.getPartitionContaining(node1)).isEqualTo(partition1);

            // Verify node2 and node3 are in the remaining partition
            final Partition remainingPartition = network.getPartitionContaining(node2);
            assertThat(remainingPartition).isNotNull();
            assertThat(remainingPartition).isNotEqualTo(partition1);
            assertThat(remainingPartition).isNotEqualTo(partition2);
            assertThat(remainingPartition.nodes()).containsExactlyInAnyOrder(node2, node3);
        } finally {
            env.destroy();
        }
    }

    /**
     * Test that attempting to rejoin a non-isolated node throws an exception.
     *
     * @param env the test environment for this test
     */
    @ParameterizedTest
    @MethodSource("environments")
    void testRejoinNonIsolatedNode(@NonNull final TestEnvironment env) {
        try {
            final Network network = env.network();
            final TimeManager timeManager = env.timeManager();

            // Setup network with 4 nodes
            final List<Node> nodes = network.addNodes(4);
            final Node node0 = nodes.getFirst();

            network.start();

            // Wait for nodes to stabilize
            timeManager.waitFor(Duration.ofSeconds(5));

            // Isolate and rejoin
            network.isolate(node0);
            network.rejoin(node0);

            // Attempt to rejoin a non-isolated node - should throw exception
            assertThatThrownBy(() -> network.rejoin(node0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Node is not isolated");
        } finally {
            env.destroy();
        }
    }

    /**
     * Test isolating and rejoining multiple nodes in different orders.
     *
     * @param env the test environment for this test
     */
    @ParameterizedTest
    @MethodSource("environments")
    void testIsolateAndRejoinMultipleNodes(@NonNull final TestEnvironment env) {
        try {
            final Network network = env.network();
            final TimeManager timeManager = env.timeManager();

            // Setup network with 4 nodes
            network.setWeightGenerator(WeightGenerators.BALANCED);
            final List<Node> nodes = network.addNodes(4);
            final Node node0 = nodes.get(0);
            final Node node1 = nodes.get(1);
            final Node node2 = nodes.get(2);
            final Node node3 = nodes.get(3);

            network.start();

            // Wait for nodes to stabilize
            timeManager.waitFor(Duration.ofSeconds(5));

            // Isolate two nodes
            network.isolate(node0);
            network.isolate(node2);

            assertThat(network.isIsolated(node0)).isTrue();
            assertThat(network.isIsolated(node1)).isFalse();
            assertThat(network.isIsolated(node2)).isTrue();
            assertThat(network.isIsolated(node3)).isFalse();

            assertThat(network.partitions()).hasSize(3); // node0, node2, {node1,node3}

            // Rejoin the first isolated node
            network.rejoin(node0);

            assertThat(network.isIsolated(node0)).isFalse();
            assertThat(network.isIsolated(node2)).isTrue();
            assertThat(network.partitions()).hasSize(2); // node2, {node0,node1,node3}

            // Verify the remaining nodes are connected
            final Partition mainPartition = network.getPartitionContaining(node0);
            assertThat(mainPartition).isNotNull();
            assertThat(mainPartition.nodes()).containsExactlyInAnyOrder(node0, node1, node3);

            // Isolate another node from the main group
            network.isolate(node1);

            assertThat(network.isIsolated(node1)).isTrue();
            assertThat(network.isIsolated(node2)).isTrue();
            assertThat(network.partitions()).hasSize(3); // node1, node2, {node0,node3}
            assertThat(mainPartition.nodes()).containsExactlyInAnyOrder(node0, node3);

            // Rejoin both isolated nodes
            network.rejoin(node1);
            network.rejoin(node2);

            // Verify all nodes are reconnected
            assertThat(network.isIsolated(node0)).isFalse();
            assertThat(network.isIsolated(node1)).isFalse();
            assertThat(network.isIsolated(node2)).isFalse();
            assertThat(network.isIsolated(node3)).isFalse();

            assertThat(network.partitions()).isEmpty();
        } finally {
            env.destroy();
        }
    }

    /**
     * Test that restoreConnectivity works with isolated nodes.
     *
     * @param env the test environment for this test
     */
    @ParameterizedTest
    @MethodSource("environments")
    void testRestoreConnectivityWithIsolatedNodes(@NonNull final TestEnvironment env) {
        try {
            final Network network = env.network();
            final TimeManager timeManager = env.timeManager();

            // Setup network with 4 nodes
            final List<Node> nodes = network.addNodes(4);
            final Node node0 = nodes.get(0);
            final Node node1 = nodes.get(1);
            final Node node2 = nodes.get(2);
            final Node node3 = nodes.get(3);

            network.start();

            // Wait for nodes to stabilize
            timeManager.waitFor(Duration.ofSeconds(5));

            // Isolate two nodes
            network.isolate(node0);
            network.isolate(node2);

            // Verify isolation
            assertThat(network.isIsolated(node0)).isTrue();
            assertThat(network.isIsolated(node2)).isTrue();
            assertThat(network.partitions()).hasSize(3);

            // Restore connectivity
            network.restoreConnectivity();

            // Verify all nodes are no longer isolated
            assertThat(network.isIsolated(node0)).isFalse();
            assertThat(network.isIsolated(node1)).isFalse();
            assertThat(network.isIsolated(node2)).isFalse();
            assertThat(network.isIsolated(node3)).isFalse();

            // Verify all partitions are removed
            assertThat(network.partitions()).isEmpty();
            assertThat(network.getPartitionContaining(node0)).isNull();
            assertThat(network.getPartitionContaining(node1)).isNull();
            assertThat(network.getPartitionContaining(node2)).isNull();
            assertThat(network.getPartitionContaining(node3)).isNull();
        } finally {
            env.destroy();
        }
    }

    /**
     * Test the interaction between isolation and regular partitioning.
     *
     * @param env the test environment for this test
     */
    @ParameterizedTest
    @MethodSource("environments")
    void testIsolationAndPartitionInteraction(@NonNull final TestEnvironment env) {
        try {
            final Network network = env.network();
            final TimeManager timeManager = env.timeManager();

            // Setup network with 6 nodes
            final List<Node> nodes = network.addNodes(6);
            final Node node0 = nodes.get(0);
            final Node node1 = nodes.get(1);
            final Node node2 = nodes.get(2);
            final Node node3 = nodes.get(3);
            final Node node4 = nodes.get(4);
            final Node node5 = nodes.get(5);

            network.start();

            // Wait for nodes to stabilize
            timeManager.waitFor(Duration.ofSeconds(5));

            // Create a regular partition first
            final Partition regularPartition = network.createPartition(node0, node1, node2);
            assertThat(regularPartition.nodes()).containsExactlyInAnyOrder(node0, node1, node2);

            // Isolate a node from the remaining group
            final Partition isolationPartition = network.isolate(node3);

            // Verify the state
            assertThat(network.isIsolated(node0)).isFalse(); // not isolated, but in a partition
            assertThat(network.isIsolated(node1)).isFalse(); // not isolated, but in a partition
            assertThat(network.isIsolated(node2)).isFalse(); // not isolated, but in a partition
            assertThat(network.isIsolated(node3)).isTrue();
            assertThat(network.isIsolated(node4)).isFalse();
            assertThat(network.isIsolated(node5)).isFalse();
            assertThat(network.partitions()).hasSize(3);

            // Verify partition structure
            assertThat(network.getPartitionContaining(node0)).isEqualTo(regularPartition);
            assertThat(network.getPartitionContaining(node3)).isEqualTo(isolationPartition);

            final Partition remainingPartition = network.getPartitionContaining(node4);
            assertThat(remainingPartition).isNotNull();
            assertThat(remainingPartition.nodes()).containsExactlyInAnyOrder(node4, node5);

            // Remove the regular partition
            network.removePartition(regularPartition);

            // This should merge the remaining partitions, leaving only the isolated node
            assertThat(network.isIsolated(node3)).isTrue();
            assertThat(network.partitions()).hasSize(2); // isolated node + merged remaining nodes
        } finally {
            env.destroy();
        }
    }
}

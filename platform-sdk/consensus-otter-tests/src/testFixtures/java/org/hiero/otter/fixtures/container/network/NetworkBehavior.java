// SPDX-License-Identifier: Apache-2.0
package org.hiero.otter.fixtures.container.network;

import static org.hiero.otter.fixtures.network.Topology.DISCONNECTED;

import com.hedera.hapi.node.base.ServiceEndpoint;
import com.hedera.hapi.node.state.roster.Roster;
import com.hedera.hapi.node.state.roster.RosterEntry;
import com.hedera.hapi.platform.state.NodeId;
import com.swirlds.platform.gossip.config.NetworkEndpoint;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.data.Percentage;
import org.hiero.otter.fixtures.Node;
import org.hiero.otter.fixtures.internal.network.ConnectionKey;
import org.hiero.otter.fixtures.network.BandwidthLimit;
import org.hiero.otter.fixtures.network.Topology.ConnectionData;

/**
 * This class is a wrapper around the Toxiproxy client and provides methods to modify the network behavior.
 */
public class NetworkBehavior {

    private static final Logger log = LogManager.getLogger();

    private static final ConnectionData INITIAL_STATE =
            new ConnectionData(true, Duration.ZERO, Percentage.withPercentage(0), BandwidthLimit.UNLIMITED);

    private final ToxiproxyClient toxiproxyClient;
    private final Map<ConnectionKey, Proxy> proxies = new HashMap<>();
    private Map<ConnectionKey, ConnectionData> connections = new HashMap<>();

    /**
     * Constructs a new NetworkBehavior instance using the Toxiproxy client.
     *
     * @param host the host on which the Toxiproxy control server is running
     * @param controlPort the port on which the Toxiproxy control server is running
     * @param roster the roster containing the nodes in the network
     * @param toxiproxyIpAddress the IP address of the Toxiproxy container in the Docker network
     */
    public NetworkBehavior(
            @NonNull final String host,
            final int controlPort,
            @NonNull final Roster roster,
            @NonNull final String toxiproxyIpAddress) {
        toxiproxyClient = new ToxiproxyClient(host, controlPort);

        final String listenAddress = toxiproxyIpAddress + ":0";

        final List<NodeId> nodeIds = roster.rosterEntries().stream()
                .map(RosterEntry::nodeId)
                .map(NodeId::new)
                .toList();
        for (final RosterEntry receiverEntry : roster.rosterEntries()) {
            final NodeId receiver = new NodeId(receiverEntry.nodeId());
            final ServiceEndpoint endpoint = receiverEntry.gossipEndpoint().getFirst();
            final String receiverAddress = "%s:%d".formatted(endpoint.domainName(), endpoint.port());
            for (final NodeId sender : nodeIds) {
                if (sender.equals(receiver)) {
                    continue;
                }
                log.debug(
                        "Creating connection between sender {} and receiver {} at address {}",
                        sender,
                        receiver,
                        receiverAddress);

                final ConnectionKey connectionKey = new ConnectionKey(sender, receiver);
                final String connectionName = "%d-%d".formatted(sender.id(), receiver.id());
                final Proxy proxy = new Proxy(connectionName, listenAddress, receiverAddress, true);
                proxies.put(connectionKey, toxiproxyClient.createProxy(proxy));
                connections.put(connectionKey, INITIAL_STATE);
            }
        }
    }

    /**
     * Updates the connections in the network based on the provided nodes and connection data.
     *
     * @param nodes the list of nodes in the network
     * @param newConnections a map of connections representing the current state of the network
     */
    public void onConnectionsChanged(
            @NonNull List<Node> nodes, @NonNull final Map<ConnectionKey, ConnectionData> newConnections) {
        for (final Node sender : nodes) {
            for (final Node receiver : nodes) {
                if (sender.equals(receiver)) {
                    continue; // Skip self-connections
                }
                final ConnectionKey connectionKey = new ConnectionKey(sender.selfId(), receiver.selfId());
                final ConnectionData oldConnectionData = connections.getOrDefault(connectionKey, DISCONNECTED);
                final ConnectionData newConnectionData = newConnections.getOrDefault(connectionKey, DISCONNECTED);
                if (oldConnectionData.connected() && !newConnectionData.connected()) {
                    disconnect(connectionKey);
                } else if (!oldConnectionData.connected() && newConnectionData.connected()) {
                    connect(connectionKey);
                }
            }
        }
        connections = newConnections;
    }

    private void connect(@NonNull final ConnectionKey connectionKey) {
        log.debug("Connecting sender {} and receiver {}", connectionKey.sender(), connectionKey.receiver());
        final Proxy proxy = proxies.get(connectionKey);
        if (proxy == null) {
            throw new IllegalStateException("No proxy found for sender %s and receiver %s"
                    .formatted(connectionKey.sender(), connectionKey.receiver()));
        }
        proxies.put(connectionKey, toxiproxyClient.updateProxy(proxy.withEnabled(true)));
    }

    private void disconnect(@NonNull final ConnectionKey connectionKey) {
        log.debug("Disconnecting sender {} and receiver {}", connectionKey.sender(), connectionKey.receiver());
        final Proxy proxy = proxies.get(connectionKey);
        if (proxy == null) {
            throw new IllegalStateException("No proxy found for sender %s and receiver %s"
                    .formatted(connectionKey.sender(), connectionKey.receiver()));
        }
        proxies.put(connectionKey, toxiproxyClient.updateProxy(proxy.withEnabled(false)));
    }

    /**
     * Gets the {@link NetworkEndpoint} of the proxy for a connection between two nodes.
     *
     * @param sender the node that sends messages
     * @param receiver the node that receives messages
     * @return the endpoint of the proxy
     * @throws NullPointerException if either sender or receiver is {@code null}
     * @throws IllegalStateException if the connection cannot be found
     */
    @NonNull
    public NetworkEndpoint getProxyEndpoint(@NonNull final Node sender, @NonNull final Node receiver) {
        final ConnectionKey connectionKey = new ConnectionKey(sender.selfId(), receiver.selfId());
        final Proxy proxy = proxies.get(connectionKey);
        if (proxy == null) {
            throw new IllegalStateException(
                    "No proxy found for sender %s and receiver %s".formatted(sender.selfId(), receiver.selfId()));
        }

        try {
            final URI uri = URI.create("http://" + proxy.listen());
            final InetAddress hostname = InetAddress.getByName(uri.getHost());
            final int port = uri.getPort();
            return new NetworkEndpoint(receiver.selfId().id(), hostname, port);
        } catch (final UnknownHostException e) {
            // this should not happen as the host has just been set up
            throw new UncheckedIOException(e);
        }
    }
}

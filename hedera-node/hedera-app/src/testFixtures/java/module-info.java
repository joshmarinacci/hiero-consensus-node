// SPDX-License-Identifier: Apache-2.0
module com.hedera.node.app.test.fixtures {
    exports com.hedera.node.app.fixtures.state;

    requires transitive com.hedera.node.app.spi.test.fixtures;
    requires transitive com.hedera.node.app;
    requires transitive com.hedera.node.hapi;
    requires transitive com.swirlds.base;
    requires transitive com.swirlds.common;
    requires transitive com.swirlds.config.api;
    requires transitive com.swirlds.metrics.api;
    requires transitive com.swirlds.platform.core;
    requires transitive com.swirlds.state.api.test.fixtures;
    requires transitive com.swirlds.state.api;
    requires transitive org.hiero.base.utility;
    requires transitive org.hiero.consensus.model;
    requires com.hedera.node.app.hapi.utils;
    requires com.hedera.node.app.service.token;
    requires com.hedera.node.app.spi;
    requires com.hedera.node.config.test.fixtures;
    requires com.hedera.node.config;
    requires com.hedera.pbj.runtime;
    requires com.swirlds.config.extensions.test.fixtures;
    requires com.swirlds.platform.core.test.fixtures;
    requires com.swirlds.state.impl.test.fixtures;
    requires com.swirlds.state.impl;
    requires org.apache.logging.log4j;
    requires org.assertj.core;
    requires static transitive com.github.spotbugs.annotations;
}

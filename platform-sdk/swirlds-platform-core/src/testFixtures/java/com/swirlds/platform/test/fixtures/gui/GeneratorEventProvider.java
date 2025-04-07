// SPDX-License-Identifier: Apache-2.0
package com.swirlds.platform.test.fixtures.gui;

import com.swirlds.platform.internal.EventImpl;
import com.swirlds.platform.test.fixtures.event.generator.GraphGenerator;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import java.util.Objects;
import org.hiero.consensus.model.event.PlatformEvent;

/**
 * Provides events for the GUI by generating them using a {@link GraphGenerator}
 */
public class GeneratorEventProvider implements GuiEventProvider {
    private final GraphGenerator graphGenerator;

    /**
     * Constructor
     *
     * @param graphGenerator the graph generator
     */
    public GeneratorEventProvider(@NonNull final GraphGenerator graphGenerator) {
        Objects.requireNonNull(graphGenerator);
        this.graphGenerator = graphGenerator;
    }

    @Override
    public @NonNull List<PlatformEvent> provideEvents(final int numberOfEvents) {
        return graphGenerator.generateEvents(numberOfEvents).stream()
                .map(EventImpl::getBaseEvent)
                .toList();
    }

    @Override
    public void reset() {
        graphGenerator.reset();
    }
}

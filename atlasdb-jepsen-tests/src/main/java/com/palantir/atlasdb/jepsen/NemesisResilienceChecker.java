/*
 * Copyright 2016 Palantir Technologies
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.jepsen;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.palantir.atlasdb.jepsen.events.Checker;
import com.palantir.atlasdb.jepsen.events.Event;
import com.palantir.atlasdb.jepsen.events.EventVisitor;
import com.palantir.atlasdb.jepsen.events.InfoEvent;
import com.palantir.atlasdb.jepsen.events.InvokeEvent;
import com.palantir.atlasdb.jepsen.events.OkEvent;

public class NemesisResilienceChecker implements Checker {
    @Override
    public CheckerResult check(List<Event> events) {
        Visitor visitor = new NemesisResilienceChecker.Visitor();
        events.forEach(event -> event.accept(visitor));
        return ImmutableCheckerResult.builder()
                .valid(visitor.valid())
                .errors(visitor.errors())
                .build();
    }

    private static class Visitor implements EventVisitor {
        private final List<Event> unsurvivedEvents = Lists.newArrayList();
        private final Set<Integer> processesPendingReads = Sets.newHashSet();

        private Event startEvent;
        private boolean awaitingInvokeOkCycle;

        @Override
        public void visit(InfoEvent event) {
            if (isNemesisEvent(event)) {
                if (isStartEvent(event)) {
                    startAwaitingInvokeOkCycles(event);
                } else if (isStopEvent(event) && awaitingInvokeOkCycle) {
                    addUnsurvivedEvents(event);
                }
            }
        }

        @Override
        public void visit(InvokeEvent event) {
            if (awaitingInvokeOkCycle) {
                processesPendingReads.add(event.process());
            }
        }

        @Override
        public void visit(OkEvent event) {
            if (awaitingInvokeOkCycle && processesPendingReads.contains(event.process())) {
                awaitingInvokeOkCycle = false;
            }
        }

        public boolean valid() {
            return unsurvivedEvents.isEmpty();
        }

        public List<Event> errors() {
            return ImmutableList.copyOf(unsurvivedEvents);
        }

        private static boolean isNemesisEvent(InfoEvent event) {
            return Objects.equals(event.process(), JepsenConstants.NEMESIS_PROCESS);
        }

        private static boolean isStartEvent(InfoEvent event) {
            return Objects.equals(event.function(), JepsenConstants.START_FUNCTION);
        }

        private static boolean isStopEvent(InfoEvent event) {
            return Objects.equals(event.function(), JepsenConstants.STOP_FUNCTION);
        }

        private void startAwaitingInvokeOkCycles(InfoEvent event) {
            startEvent = event;
            awaitingInvokeOkCycle = true;
            processesPendingReads.clear();
        }

        private void addUnsurvivedEvents(InfoEvent event) {
            unsurvivedEvents.add(startEvent);
            unsurvivedEvents.add(event);
            awaitingInvokeOkCycle = false;
        }
    }
}

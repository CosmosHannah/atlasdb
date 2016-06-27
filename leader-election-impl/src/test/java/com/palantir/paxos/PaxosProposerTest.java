/**
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
package com.palantir.paxos;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class PaxosProposerTest {
    private static final PaxosProposalId PROPOSAL_ID = new PaxosProposalId(1, UUID.randomUUID().toString());
    private static final List<PaxosLearner> NO_LEARNERS = ImmutableList.of();
    private static final BooleanPaxosResponse SUCCESSFUL_ACCEPTANCE = new BooleanPaxosResponse(true);
    private static final byte[] VALUE = "hello".getBytes();
    private static final int KEY = 1;

    @Mock
    private PaxosLearner learner;
    @Mock
    private List<PaxosAcceptor> acceptor;
    @Mock
    private List<PaxosLearner> learners;
    @Mock
    private PaxosAcceptor acceptingAcceptor;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    PaxosProposer proposer;

    @Before
    public void setup() {
        when(acceptingAcceptor.prepare(Matchers.anyLong(), any(PaxosProposalId.class))).thenReturn(successfulPromise());
        when(acceptingAcceptor.accept(Matchers.anyLong(), any(PaxosProposal.class))).thenReturn(SUCCESSFUL_ACCEPTANCE);
    }

    @Test public void
    should_accept_a_proposal_if_there_is_only_one_acceptor_and_the_acceptor_accepts_the_proposal() throws PaxosRoundFailureException {
        proposer = PaxosProposerImpl.newProposer(learner, ImmutableList.of(acceptingAcceptor), NO_LEARNERS, 1, executor);

        assertThat(proposer.propose(KEY, VALUE), is(VALUE));
    }

    private PaxosPromise successfulPromise() {
        PaxosProposalId lastAcceptedId = null;
        PaxosValue value = null;
        return PaxosPromise.create(true, PROPOSAL_ID, lastAcceptedId, value);
    }

}

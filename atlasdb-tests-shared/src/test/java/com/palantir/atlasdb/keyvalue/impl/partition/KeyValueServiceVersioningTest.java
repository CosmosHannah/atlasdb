package com.palantir.atlasdb.keyvalue.impl.partition;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.primitives.UnsignedBytes;
import com.palantir.atlasdb.keyvalue.api.KeyValueService;
import com.palantir.atlasdb.keyvalue.impl.InMemoryKeyValueService;
import com.palantir.atlasdb.keyvalue.partition.KeyValueEndpoint;
import com.palantir.atlasdb.keyvalue.partition.PartitionMapService;
import com.palantir.atlasdb.keyvalue.partition.PartitionMapServiceImpl;
import com.palantir.atlasdb.keyvalue.partition.QuorumParameters;
import com.palantir.atlasdb.keyvalue.partition.SimpleKeyValueEndpoint;
import com.palantir.atlasdb.keyvalue.partition.VersionedPartitionedKeyValueService;
import com.palantir.atlasdb.keyvalue.remoting.InsufficientConsistencyExceptionMapper;
import com.palantir.atlasdb.keyvalue.remoting.KeyAlreadyExistsExceptionMapper;
import com.palantir.atlasdb.keyvalue.remoting.RemotingKeyValueService;
import com.palantir.atlasdb.keyvalue.remoting.VersionTooOldExceptionMapper;
import com.palantir.atlasdb.server.InboxPopulatingContainerRequestFilter;
import com.palantir.common.concurrent.PTExecutors;
import com.palantir.util.Mutable;
import com.palantir.util.Mutables;

import io.dropwizard.testing.junit.DropwizardClientRule;

public class KeyValueServiceVersioningTest {

    private final SimpleModule module = RemotingKeyValueService.kvsModule();
    private final ObjectMapper mapper = RemotingKeyValueService.kvsMapper();

    private final QuorumParameters parameters = new QuorumParameters(3, 2, 2);
    private final Mutable<Long> serverVersion = Mutables.newMutable();
    private final Mutable<Long> clientVersion = Mutables.newMutable();

    private final Supplier<Long> serverVersionSupplier = new Supplier<Long>() {
        @Override
        public Long get() {
            return serverVersion.get();
        }
    };

    private final Supplier<Long> clientVersionSupplier = new Supplier<Long>() {
        @Override
        public Long get() {
            return clientVersion.get();
        }
    };

    final KeyValueService remoteEndpoint = RemotingKeyValueService.createServerSide(new InMemoryKeyValueService(false), serverVersionSupplier);
    // This service has no map initially stored.
    final PartitionMapService remotePartitionMap = new PartitionMapServiceImpl();

    @Rule
    public final DropwizardClientRule remoteEndpointService = new DropwizardClientRule(
            remoteEndpoint,
            KeyAlreadyExistsExceptionMapper.instance(),
            InsufficientConsistencyExceptionMapper.instance(),
            VersionTooOldExceptionMapper.instance(),
            new InboxPopulatingContainerRequestFilter(mapper));

    @Rule
    public final DropwizardClientRule remotePartitionMapService = new DropwizardClientRule(remotePartitionMap);

    VersionedPartitionedKeyValueService vpkvs;

    @Before
    public void setUp() {

        KeyValueEndpoint vkve = new SimpleKeyValueEndpoint(
                remoteEndpointService.baseUri().toString(),
                remotePartitionMapService.baseUri().toString());

        ImmutableSortedMap<byte[], KeyValueEndpoint> ring =
            ImmutableSortedMap
                    .<byte[], KeyValueEndpoint>orderedBy(UnsignedBytes.lexicographicalComparator())
                    .put(new byte[] {0}, vkve)
                    .put(new byte[] {0, 0}, vkve)
                    .put(new byte[] {0, 0, 0}, vkve)
                    .build();

        vpkvs = VersionedPartitionedKeyValueService.create(ring, PTExecutors.newCachedThreadPool(), parameters);
    }

    @Test
    public void test() {
        vpkvs.createTable("whatever", 15);
    }

    @Test
    public void testPartitionMapOutOfDate() {

//        clientVersion.set(123L);
//        serverVersion.set(123L);
//        kve.run(new Function<KeyValueService, Void>() {
//            @Override @Nullable
//            public Void apply(@Nullable KeyValueService input) {
//                input.getAllTableNames();
//                return null;
//            }
//        });
//
//        serverVersion.set(124L);
//        try {
//            kve.run(new Function<KeyValueService, Void>() {
//                @Override @Nullable
//                public Void apply(@Nullable KeyValueService input) {
//                    input.getAllTableNames();
//                    return null;
//                }
//            });
//            Assert.fail();
//        } catch (UpdatePartitionMapException e) {
//            assertEquals(124L, e.getNewVersion());
//        }
    }

}

/*
 * Copyright 2015 Palantir Technologies
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
package com.palantir.atlasdb.cleaner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.palantir.atlasdb.keyvalue.api.Cell;
import com.palantir.atlasdb.keyvalue.api.TableReference;
import com.palantir.atlasdb.transaction.api.TransactionManager;

/**
 * The SimpleCleaner simply delegates to the various specialized objects that do the real work.
 *
 * @author jweel
 */
public class SimpleCleaner implements Cleaner {
    private static final Logger log = LoggerFactory.getLogger(SimpleCleaner.class);

    private final Scrubber scrubber;
    private final Puncher puncher;
    private final Supplier<Long> transactionReadTimeoutMillisSupplier;

    public SimpleCleaner(Scrubber scrubber,
                         Puncher puncher,
                         Supplier<Long> transactionReadTimeoutMillisSupplier) {
        this.scrubber = scrubber;
        this.puncher = puncher;
        this.transactionReadTimeoutMillisSupplier = transactionReadTimeoutMillisSupplier;
    }

    @Override
    public void queueCellsForScrubbing(Multimap<Cell, TableReference> cellToTableRefs,
                                       long scrubTimestamp) {
        scrubber.queueCellsForScrubbing(cellToTableRefs, scrubTimestamp);
    }

    @Override
    public void scrubImmediately(TransactionManager txManager,
                                 Multimap<TableReference, Cell> tableRefToCell,
                                 long scrubTimestamp,
                                 long commitTimestamp) {
        try {
            scrubber.scrubImmediately(txManager, tableRefToCell, scrubTimestamp, commitTimestamp);
        } catch (RuntimeException e) {
            String message = "Scrubbing has failed during aggressive hard delete.  "
                    + "Deleted values will no longer be visible to any Palantir clients, but the deleted values"
                    + " will still remain in the underlying KVS until the background scrub task has finished"
                    + " scrubbing.";
            log.error(message, e);
            // QA-85267 We cannot propagate the exception because the client doesn't have the exception type
            // that cassandra throws thrift.UnavailableException.
            // Instead we log so we won't lose the error and throw a generic exception back to the user.
            throw new RuntimeException(message);
        }
    }

    @Override
    public void punch(long timestamp) {
        puncher.punch(timestamp);
    }

    @Override
    public long getTransactionReadTimeoutMillis() {
        return transactionReadTimeoutMillisSupplier.get();
    }

    @Override
    public long getUnreadableTimestamp() {
        return scrubber.getUnreadableTimestamp();
    }

    @Override
    public void close() {
        scrubber.shutdown();
        puncher.shutdown();
    }

    @Override
    public void start(TransactionManager txManager) {
        scrubber.start(txManager);
    }
}

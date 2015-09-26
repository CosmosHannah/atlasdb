/**
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
package com.palantir.atlasdb.keyvalue.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedBytes;
import com.palantir.atlasdb.transaction.api.Transaction;
import com.palantir.common.annotation.Immutable;
import com.palantir.common.persist.Persistable;
import com.palantir.util.Pair;

/**
 * Allows you to restrict a call on the database
 * to specific rows and columns.
 * By default, calls on a table in the key-value store
 * cover all rows and columns in the table.
 * To restrict the rows or columns,  call
 * the methods on the <code>RangeRequest</code> class.
 */
@Immutable public final class RangeRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private final byte[] startInclusive;
    private final byte[] endExclusive;
    private final ImmutableSortedSet<byte[]> columns;
    private final Integer batchHint;
    private final boolean reverse;

    /**
     * Returns a {@link Builder} instance, a helper class
     * for instantiating immutable <code>RangeRequest</code>
     * objects.
     */
    public static Builder builder() {
        return new Builder(false);
    }

    public static Builder reverseBuilder() {
        return new Builder(true);
    }

    public static Builder builder(boolean reverse) {
        return new Builder(reverse);
    }

    public static RangeRequest all() {
        return builder().build();
    }

    private RangeRequest(byte[] startInclusive,
                         byte[] endExclusive,
                         Iterable<byte[]> cols,
                         Integer batchHint,
                         boolean reverse) {
        this.startInclusive = startInclusive;
        this.endExclusive = endExclusive;
        this.columns = cloneSet(cols);
        this.batchHint = batchHint;
        this.reverse = reverse;
    }

    /**
     * Start is inclusive.  If this range is reversed, then the start will
     * be after the end.
     * <p>
     * This array may be empty if the start is unbounded.
     */
    @Nonnull public byte[] getStartInclusive() {
        return startInclusive.clone();
    }

    /**
     * End is exclusive.  If this range is reversed, then the end will
     * be before the start.
     * <p>
     * This array may be empty if the end doens't have a bound.
     */
    @Nonnull public byte[] getEndExclusive() {
        return endExclusive.clone();
    }

    /**
     * An empty set of column names means that all columns are selected.
     */
    @Nonnull public SortedSet<byte[]> getColumnNames() {
        return cloneSet(columns);
    }

    public boolean containsColumn(byte[] col) {
        return columns.isEmpty() || columns.contains(col);
    }

    public boolean isReverse() {
        return reverse;
    }

    public boolean isEmptyRange() {
        if (startInclusive.length == 0 && RangeRequests.isFirstRowName(reverse, endExclusive)) {
            return true;
        }
        if (startInclusive.length == 0 || endExclusive.length == 0) {
            return false;
        }
        if (reverse) {
            return UnsignedBytes.lexicographicalComparator().compare(startInclusive, endExclusive) <= 0;
        } else {
            return UnsignedBytes.lexicographicalComparator().compare(startInclusive, endExclusive) >= 0;
        }
    }

    public boolean inRange(byte[] position) {
        Preconditions.checkArgument(Cell.isNameValid(position));
        final boolean afterStart;
        final boolean afterEnd;
        if (reverse) {
            afterStart = getStartInclusive().length == 0 || UnsignedBytes.lexicographicalComparator().compare(getStartInclusive(), position) >= 0;
            afterEnd = getEndExclusive().length == 0 || UnsignedBytes.lexicographicalComparator().compare(getEndExclusive(), position) < 0;
        } else {
            afterStart = getStartInclusive().length == 0 || UnsignedBytes.lexicographicalComparator().compare(getStartInclusive(), position) <= 0;
            afterEnd = getEndExclusive().length == 0 || UnsignedBytes.lexicographicalComparator().compare(getEndExclusive(), position) > 0;
        }

        return afterStart && afterEnd;
    }

    @Nullable
    public Integer getBatchHint() {
        return batchHint;
    }

    public RangeRequest withBatchHint(int hint) {
        return new RangeRequest(startInclusive, endExclusive, columns, hint, reverse);
    }

    public Builder getBuilder() {
        return new Builder(reverse)
                .endRowExclusive(endExclusive)
                .startRowInclusive(startInclusive)
                .batchHint(batchHint)
                .retainColumns(columns);
    }

    @Override
    public String toString() {
        return "RangeRequest [startInclusive=" + Arrays.toString(startInclusive)
                + ", endExclusive=" + Arrays.toString(endExclusive)
                + ", batchHint=" + batchHint
                + ", reverse=" + reverse
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((batchHint == null) ? 0 : batchHint.hashCode());
        result = prime * result + ((columns == null) ? 0 : columns.hashCode());
        result = prime * result + Arrays.hashCode(endExclusive);
        result = prime * result + (reverse ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(startInclusive);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RangeRequest other = (RangeRequest) obj;
        if (batchHint == null) {
            if (other.batchHint != null)
                return false;
        } else if (!batchHint.equals(other.batchHint))
            return false;
        if (columns == null) {
            if (other.columns != null)
                return false;
        } else if (!columns.equals(other.columns))
            return false;
        if (!Arrays.equals(endExclusive, other.endExclusive))
            return false;
        if (reverse != other.reverse)
            return false;
        if (!Arrays.equals(startInclusive, other.startInclusive))
            return false;
        return true;
    }

    private static ImmutableSortedSet<byte[]> cloneSet(Iterable<byte[]> set) {
        ImmutableSortedSet.Builder<byte[]> builder = ImmutableSortedSet.orderedBy(UnsignedBytes.lexicographicalComparator());
        for (byte[] col : set) {
            builder.add(col.clone());
        }
        return builder.build();
    }

    /**
     * This will return a start and end row that will exactly contain all rows for this prefix in
     * reverse.
     * <p>
     * start will be on the left hand side and will be greater lexicographically
     */
    private static Pair<byte[], byte[]> createNamesForReversePrefixScan(@Nonnull byte[] name) {
        Preconditions.checkArgument(Preconditions.checkNotNull(name).length <= Cell.MAX_NAME_LENGTH, "name is too long");
        if (name.length == 0) {
            return Pair.create(name, name);
        }
        byte[] startName = new byte[Cell.MAX_NAME_LENGTH];
        System.arraycopy(name, 0, startName, 0, name.length);
        for (int i = name.length ; i < startName.length ; i++) {
            startName[i] = (byte) 0xff;
        }
        byte[] endName = RangeRequests.previousLexicographicName(name);
        return Pair.create(startName, endName);
    }

    /**
     * A helper class used to construct an immutable {@link RangeRequest}
     * instance.
     * <p>
     * By default, the range covers all rows and columns. To restrict the rows or columns,
     * call      * the methods on the <code>RangeRequest</code> class.
     */
    @NotThreadSafe public static final class Builder {
        private byte[] startInclusive = EMPTY_BYTE_ARRAY;
        private byte[] endExclusive = EMPTY_BYTE_ARRAY;
        private Set<byte[]> columns = Sets.newTreeSet(UnsignedBytes.lexicographicalComparator());
        private Integer batchHint = null;
        private final boolean reverse;

        Builder(boolean reverse) {
            this.reverse = reverse;
        }

        public boolean isReverse() {
            return reverse;
        }

        /**
         * This will set the start and the end to get all rows that have a given prefix.
         */
        public Builder prefixRange(byte[] prefix) {
            if (reverse) {
                Pair<byte[], byte[]> pair = createNamesForReversePrefixScan(prefix);
                this.startInclusive = pair.lhSide;
                this.endExclusive = pair.rhSide;
            } else {
                this.startInclusive = Preconditions.checkNotNull(prefix).clone();
                this.endExclusive = RangeRequests.createEndNameForPrefixScan(prefix);
            }
            return this;
        }

        public Builder startRowInclusive(byte[] start) {
            this.startInclusive = Preconditions.checkNotNull(start).clone();
            return this;
        }

        public Builder startRowInclusive(Prefix start) {
            return startRowInclusive(start.getBytes());
        }

        public Builder startRowInclusive(Persistable start) {
            return startRowInclusive(start.persistToBytes());
        }

        public Builder endRowExclusive(byte[] end) {
            this.endExclusive = Preconditions.checkNotNull(end).clone();
            return this;
        }

        public Builder endRowExclusive(Prefix end) {
            return endRowExclusive(end.getBytes());
        }

        public Builder endRowExclusive(Persistable end) {
            return endRowExclusive(end.persistToBytes());
        }

        public Builder retainColumns(Iterable<byte[]> colsToRetain) {
            Iterables.addAll(columns, colsToRetain);
            return this;
        }

        public Builder retainColumns(ColumnSelection selection) {
            if (!selection.allColumnsSelected()) {
                Iterables.addAll(columns, selection.getSelectedColumns());
            }
            return this;
        }

        /**
         * This is a hint for how much data the underlying system should process at a time.  If we are expecting to
         * read a lot from this range, then this should be pretty large for performance.  If we are only going to read
         * the first thing in a range, then this should be set to 1.
         * <p>
         * If hint is null then the range will use the default. Usually for {@link Transaction#getRange(String, RangeRequest)}
         * this means the batch size will be whatever is passed as the batch size to
         * BatchingVisitable#batchAccept(int, com.palantir.common.base.AbortingVisitor)
         */
        public Builder batchHint(Integer hint) {
            Preconditions.checkArgument(hint == null || hint > 0);
            batchHint = hint;
            return this;
        }

        public boolean isInvalidRange() {
            if (startInclusive.length == 0 || endExclusive.length == 0) {
                return false;
            }
            if (reverse) {
                return UnsignedBytes.lexicographicalComparator().compare(startInclusive, endExclusive) < 0;
            } else {
                return UnsignedBytes.lexicographicalComparator().compare(startInclusive, endExclusive) > 0;
            }
        }

        public RangeRequest build() {
            RangeRequest rangeRequest = new RangeRequest(startInclusive, endExclusive, columns, batchHint, reverse);
            if (isInvalidRange()) {
                throw new IllegalArgumentException("Invalid range request, check row byte ordering for reverse ordered values: " + rangeRequest);
            }
            return rangeRequest;
        }
    }
}

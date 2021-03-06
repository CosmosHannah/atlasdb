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
package com.palantir.atlasdb.sweep;

import java.util.Iterator;

public class CountingIterator<T>  implements Iterator<T> {
    protected final Iterator<T> delegate;
    protected T lastItem = null;
    protected int totalItems = 0;

    public CountingIterator(Iterator<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public T next() {
        T next = delegate.next();
        lastItem = next;
        totalItems++;
        return next;
    }

    public T lastItem() {
        if (hasNext()) {
            throw new IllegalStateException("Cannot call lastItem if the iterator has more items");
        }
        return lastItem;
    }

    public int size() {
        if (hasNext()) {
            throw new IllegalStateException("Cannot call lastItem if the iterator has more items");
        }
        return totalItems;
    }
}

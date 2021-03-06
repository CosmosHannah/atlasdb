/*
 * Copyright 2017 Palantir Technologies
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
package com.palantir.common.remoting;

import java.util.Collection;
import java.util.Map;

/**
 * This class is useful for accessing HTTP headers in a case-insensitive manner.
 * This is necessary for compatibility with OkHttp 3.3.0+, as that lower-cases header names whereas we use constants
 * from {@link com.google.common.net.HttpHeaders} where header names are in Train-Case.
 * Note that the HTTP specification does not place restrictions on casing of headers.
 */
public final class HeaderAccessUtils {
    private HeaderAccessUtils() {
        // utility
    }

    /**
     * Compares the keys of the map to the header in a case-insensitive manner; upon finding a match, compares
     * the associated collection of strings from the map with the value, returning true iff this contains a match.
     * If no key matches, this method returns false.
     *
     * As a precondition: the headers map should NOT contain distinct keys differing only in case.
     * (This is true as far as our use-case is concerned.)
     */
    public static boolean shortcircuitingCaseInsensitiveContainsEntry(
            Map<String, Collection<String>> headers,
            String header,
            String value) {
        for (Map.Entry<String, Collection<String>> entry : headers.entrySet()) {
            if (header.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue().contains(value);
            }
        }
        return false;
    }
}

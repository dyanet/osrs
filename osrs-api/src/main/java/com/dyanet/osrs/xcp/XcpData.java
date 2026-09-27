/*
 * Copyright 2012-2026, Dyanet Inc., Akber A. Choudhry,
 *   and other individual contributors identified by the
 *   @authors tag in each source artefact.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   You may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *   either express or implied.
 *   See the License for the specific language governing permissions
 *   and limitations under the License.
 */

package com.dyanet.osrs.xcp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only view of a decoded {@code dt_assoc}, with typed accessors.
 *
 * <p>Decoded values are {@code String} (scalars), {@code Map<String, Object>} ({@code dt_assoc})
 * and {@code List<Object>} ({@code dt_array}). Accessors take a path of keys; a path element
 * that is a number indexes into a {@code dt_array}. Missing keys and type mismatches give an
 * empty result instead of throwing.
 */
public final class XcpData {

    private static final XcpData EMPTY = new XcpData(Collections.emptyMap());

    private final Map<String, Object> values;

    private XcpData(Map<String, Object> values) {
        this.values = values;
    }

    /**
     * @param values a decoded {@code dt_assoc} (not copied; decoded maps are already unmodifiable)
     * @return the view
     */
    public static XcpData of(Map<String, Object> values) {
        return values == null || values.isEmpty() ? EMPTY : new XcpData(values);
    }

    /**
     * @return an empty view
     */
    public static XcpData empty() {
        return EMPTY;
    }

    /**
     * @return the underlying decoded map
     */
    public Map<String, Object> asMap() {
        return values;
    }

    /**
     * @return whether there are no values
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * @param path keys (and array indexes) to follow
     * @return the raw decoded value at the path
     */
    public Optional<Object> find(String... path) {
        Object cur = values;
        for (String key : path) {
            if (cur instanceof Map<?, ?> m) {
                cur = m.get(key);
            } else if (cur instanceof List<?> l) {
                try {
                    int i = Integer.parseInt(key);
                    cur = i >= 0 && i < l.size() ? l.get(i) : null;
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
            if (cur == null) {
                return Optional.empty();
            }
        }
        return Optional.of(cur);
    }

    /**
     * @param path keys to follow
     * @return the scalar at the path
     */
    public Optional<String> getString(String... path) {
        return find(path).filter(String.class::isInstance).map(String.class::cast);
    }

    /**
     * @param path keys to follow
     * @return the scalar at the path parsed as an integer; empty if absent or not a number
     */
    public Optional<Integer> getInt(String... path) {
        return getString(path).map(String::trim).flatMap(s -> {
            try {
                return Optional.of(Integer.valueOf(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
    }

    /**
     * @param path keys to follow
     * @return the scalar at the path parsed as a long; empty if absent or not a number
     */
    public Optional<Long> getLong(String... path) {
        return getString(path).map(String::trim).flatMap(s -> {
            try {
                return Optional.of(Long.valueOf(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
    }

    /**
     * @param path keys to follow
     * @return the scalar at the path as an exact decimal (for money); empty if absent or not a number
     */
    public Optional<BigDecimal> getDecimal(String... path) {
        return getString(path).map(String::trim).flatMap(s -> {
            try {
                return Optional.of(new BigDecimal(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
    }

    /**
     * OpenSRS flags are {@code 1}/{@code 0}; {@code Y}/{@code N} and {@code true}/{@code false}
     * are accepted too.
     *
     * @param path keys to follow
     * @return the flag at the path; empty if absent or unrecognised
     */
    public Optional<Boolean> getFlag(String... path) {
        return getString(path).map(s -> s.trim().toLowerCase()).flatMap(s -> switch (s) {
            case "1", "y", "yes", "true" -> Optional.of(Boolean.TRUE);
            case "0", "n", "no", "false" -> Optional.of(Boolean.FALSE);
            default -> Optional.empty();
        });
    }

    /**
     * @param path keys to follow
     * @return the {@code dt_assoc} at the path; empty view if absent
     */
    @SuppressWarnings("unchecked")
    public XcpData getData(String... path) {
        return find(path).filter(Map.class::isInstance)
            .map(m -> of((Map<String, Object>) m)).orElse(EMPTY);
    }

    /**
     * @param path keys to follow
     * @return the {@code dt_array} at the path; empty list if absent
     */
    @SuppressWarnings("unchecked")
    public List<Object> getList(String... path) {
        return find(path).filter(List.class::isInstance)
            .map(l -> (List<Object>) l).orElse(Collections.emptyList());
    }

    /**
     * The common "array of records" shape: a {@code dt_array} whose items are {@code dt_assoc}.
     * Items that aren't {@code dt_assoc} are skipped.
     *
     * @param path keys to follow
     * @return one view per record, in array order
     */
    @SuppressWarnings("unchecked")
    public List<XcpData> getDataList(String... path) {
        List<XcpData> out = new ArrayList<>();
        for (Object o : getList(path)) {
            if (o instanceof Map<?, ?> m) {
                out.add(of((Map<String, Object>) m));
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** Lists the keys only: values may hold personal data or authorisation codes. */
    @Override
    public String toString() {
        return "XcpData" + values.keySet();
    }
}

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

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.dyanet.osrs.OsrsProtocolException;

/**
 * Encodes requests to, and decodes replies from, the XCP {@code OPS_envelope} format.
 *
 * <p>Encoding maps Java values onto the XCP data types:
 * <table>
 *   <caption>Encoding</caption>
 *   <tr><th>Java</th><th>XCP</th></tr>
 *   <tr><td>{@code CharSequence}</td><td>scalar text</td></tr>
 *   <tr><td>{@code Number}</td><td>scalar ({@code BigDecimal} in plain notation)</td></tr>
 *   <tr><td>{@code Boolean}</td><td>{@code 1} / {@code 0}</td></tr>
 *   <tr><td>{@code Enum}</td><td>its {@code name()}</td></tr>
 *   <tr><td>{@code LocalDate}</td><td>{@code YYYY-MM-DD}</td></tr>
 *   <tr><td>{@code Map<String, ?>}</td><td>{@code dt_assoc} ({@code null} values left out)</td></tr>
 *   <tr><td>{@code Iterable}, {@code Object[]}</td><td>{@code dt_array} with keys {@code 0..n-1}</td></tr>
 * </table>
 *
 * <p>Decoding gives {@code String}, {@code Map<String, Object>} and {@code List<Object>}, all
 * unmodifiable. The parser never resolves DTDs or external entities, so replies can't trigger
 * XXE or entity-expansion attacks.
 */
public final class XcpCodec {

    /** The {@code OPS_envelope} header version this client speaks. */
    public static final String ENVELOPE_VERSION = "0.9";
    /** The only protocol value OpenSRS accepts on this interface. */
    public static final String PROTOCOL = "XCP";

    private static final String PROLOGUE =
        "<?xml version='1.0' encoding='UTF-8' standalone='no' ?>\n"
        + "<!DOCTYPE OPS_envelope SYSTEM 'ops.dtd'>\n";

    private static final XMLInputFactory INPUT = createInputFactory();

    private XcpCodec() {
    }

    // ---------------------------------------------------------------- encoding

    /**
     * @param request the command
     * @return the XML body to send
     * @throws IllegalArgumentException if an attribute has an unsupported type or a character
     *     XML can't carry
     */
    public static String encodeRequest(XcpRequest request) {
        Map<String, Object> top = new LinkedHashMap<>();
        top.put("protocol", PROTOCOL);
        top.put("action", request.getAction());
        top.put("object", request.getObject());
        if (request.getRegistrantIp() != null) {
            top.put("registrant_ip", request.getRegistrantIp());
        }
        if (!request.getAttributes().isEmpty()) {
            top.put("attributes", request.getAttributes());
        }
        return encodeEnvelope(top);
    }

    /**
     * Encodes any top-level {@code dt_assoc}, e.g. to build a reply for tests.
     *
     * @param topLevel the items of the envelope's {@code data_block}
     * @return a complete {@code OPS_envelope} document
     */
    public static String encodeEnvelope(Map<String, ?> topLevel) {
        StringBuilder sb = new StringBuilder(512).append(PROLOGUE)
            .append("<OPS_envelope><header><version>").append(ENVELOPE_VERSION)
            .append("</version></header><body><data_block>");
        writeAssoc(sb, topLevel, "");
        return sb.append("</data_block></body></OPS_envelope>").toString();
    }

    private static void writeValue(StringBuilder sb, Object v, String path) {
        if (v instanceof Map<?, ?> m) {
            writeAssoc(sb, m, path);
        } else if (v instanceof Iterable<?> it) {
            writeArray(sb, it, path);
        } else if (v instanceof Object[] arr) {
            writeArray(sb, java.util.Arrays.asList(arr), path);
        } else {
            escape(sb, scalar(v, path), path);
        }
    }

    private static void writeAssoc(StringBuilder sb, Map<?, ?> m, String path) {
        sb.append("<dt_assoc>");
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            String key = String.valueOf(e.getKey());
            String p = path.isEmpty() ? key : path + "." + key;
            sb.append("<item key=\"");
            escape(sb, key, p);
            sb.append("\">");
            writeValue(sb, e.getValue(), p);
            sb.append("</item>");
        }
        sb.append("</dt_assoc>");
    }

    private static void writeArray(StringBuilder sb, Iterable<?> items, String path) {
        sb.append("<dt_array>");
        int i = 0;
        for (Object o : items) {
            String p = path + "[" + i + "]";
            if (o == null) {
                throw new IllegalArgumentException("null element in dt_array at " + p);
            }
            sb.append("<item key=\"").append(i).append("\">");
            writeValue(sb, o, p);
            sb.append("</item>");
            i++;
        }
        sb.append("</dt_array>");
    }

    private static String scalar(Object v, String path) {
        if (v instanceof CharSequence cs) {
            return cs.toString();
        } else if (v instanceof BigDecimal bd) {
            return bd.toPlainString();
        } else if (v instanceof Number n) {
            return n.toString();
        } else if (v instanceof Boolean b) {
            return b ? "1" : "0";
        } else if (v instanceof Enum<?> e) {
            return e.name();
        } else if (v instanceof LocalDate d) {
            return d.toString();
        } else if (v instanceof Character c) {
            return c.toString();
        }
        throw new IllegalArgumentException("Unsupported XCP value type " + v.getClass().getName()
            + " at " + path);
    }

    private static void escape(StringBuilder sb, String s, String path) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> {
                    if (Character.isHighSurrogate(c) && i + 1 < s.length()
                            && Character.isLowSurrogate(s.charAt(i + 1))) {
                        sb.append(c).append(s.charAt(++i));
                    } else if ((c < 0x20 && c != '\t' && c != '\n' && c != '\r')
                            || Character.isSurrogate(c) || c == 0xFFFE || c == 0xFFFF) {
                        throw new IllegalArgumentException(
                            "Character U+" + String.format("%04X", (int) c)
                            + " can't be sent in XML, at " + path);
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- decoding

    /**
     * @param request the request the reply answers, or {@code null}
     * @param xml     the reply body
     * @return the decoded reply
     * @throws OsrsProtocolException if the body isn't an {@code OPS_envelope}
     */
    public static XcpResponse decodeResponse(XcpRequest request, String xml) {
        return new XcpResponse(request, decode(xml), xml);
    }

    /**
     * @param xml an {@code OPS_envelope} document
     * @return the top-level {@code dt_assoc} of its {@code data_block}
     * @throws OsrsProtocolException if the body isn't an {@code OPS_envelope}
     */
    public static Map<String, Object> decode(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new OsrsProtocolException("Empty reply from OpenSRS", null);
        }
        XMLStreamReader r = null;
        try {
            r = INPUT.createXMLStreamReader(new StringReader(xml));
            boolean inEnvelope = false;
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    String name = r.getLocalName();
                    if (!inEnvelope) {
                        if (!"OPS_envelope".equals(name)) {
                            break;
                        }
                        inEnvelope = true;
                    } else if ("data_block".equals(name)) {
                        Object v = readContainerIn(r, "data_block");
                        if (v instanceof Map<?, ?>) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> m = (Map<String, Object>) v;
                            return m;
                        }
                        throw new OsrsProtocolException(
                            "OPS_envelope data_block does not hold a dt_assoc", null);
                    }
                }
            }
            throw new OsrsProtocolException("Reply is not an OPS_envelope: " + snippet(xml), null);
        } catch (XMLStreamException e) {
            throw new OsrsProtocolException("Malformed reply from OpenSRS: " + snippet(xml), e);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (XMLStreamException ignored) {
                    // nothing useful to do
                }
            }
        }
    }

    /** Reads the first dt_* inside the current element, then consumes up to its end tag. */
    private static Object readContainerIn(XMLStreamReader r, String element) throws XMLStreamException {
        Object value = null;
        StringBuilder text = new StringBuilder();
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                Object v = readTyped(r);
                if (value == null) {
                    value = v;
                }
            } else if (ev == XMLStreamConstants.CHARACTERS || ev == XMLStreamConstants.CDATA
                    || ev == XMLStreamConstants.SPACE) {
                text.append(r.getText());
            } else if (ev == XMLStreamConstants.END_ELEMENT && element.equals(r.getLocalName())) {
                return value != null ? value : text.toString();
            }
        }
        throw new XMLStreamException("Unexpected end of document inside <" + element + ">");
    }

    /** Called at a START_ELEMENT; returns the decoded value and leaves r at its END_ELEMENT. */
    private static Object readTyped(XMLStreamReader r) throws XMLStreamException {
        String name = r.getLocalName();
        switch (name) {
            case "dt_assoc": {
                Map<String, Object> m = new LinkedHashMap<>();
                forEachItem(r, name, (k, v) -> m.put(k, v));
                return Collections.unmodifiableMap(m);
            }
            case "dt_array": {
                List<String> keys = new ArrayList<>();
                List<Object> vals = new ArrayList<>();
                forEachItem(r, name, (k, v) -> {
                    keys.add(k);
                    vals.add(v);
                });
                return Collections.unmodifiableList(orderByIndex(keys, vals));
            }
            case "dt_scalar":
            case "dt_scalarref":
                return r.getElementText();
            default:
                skip(r);
                return null;
        }
    }

    private interface ItemSink {
        void accept(String key, Object value);
    }

    private static void forEachItem(XMLStreamReader r, String container, ItemSink sink)
            throws XMLStreamException {
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                if ("item".equals(r.getLocalName())) {
                    String key = r.getAttributeValue(null, "key");
                    Object v = readContainerIn(r, "item");
                    sink.accept(key == null ? "" : key, v);
                } else {
                    skip(r);
                }
            } else if (ev == XMLStreamConstants.END_ELEMENT && container.equals(r.getLocalName())) {
                return;
            }
        }
        throw new XMLStreamException("Unexpected end of document inside <" + container + ">");
    }

    /** dt_array items carry their index as the key; order by it when every key is numeric. */
    private static List<Object> orderByIndex(List<String> keys, List<Object> vals) {
        Integer[] idx = new Integer[keys.size()];
        for (int i = 0; i < idx.length; i++) {
            try {
                idx[i] = Integer.valueOf(keys.get(i).trim());
            } catch (NumberFormatException e) {
                return vals;
            }
        }
        Integer[] order = new Integer[idx.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, (a, b) -> Integer.compare(idx[a], idx[b]));
        List<Object> out = new ArrayList<>(vals.size());
        for (Integer i : order) {
            out.add(vals.get(i));
        }
        return out;
    }

    private static void skip(XMLStreamReader r) throws XMLStreamException {
        int depth = 1;
        while (depth > 0 && r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                depth++;
            } else if (ev == XMLStreamConstants.END_ELEMENT) {
                depth--;
            }
        }
    }

    private static String snippet(String s) {
        String flat = s.strip().replaceAll("\\s+", " ");
        return flat.length() <= 120 ? flat : flat.substring(0, 120) + "...";
    }

    private static XMLInputFactory createInputFactory() {
        XMLInputFactory f = XMLInputFactory.newFactory();
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        f.setProperty(XMLInputFactory.IS_COALESCING, true);
        return f;
    }
}

package com.sedmelluq.discord.lavaplayer.tools;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Helper methods related to strings and maps.
 */
public class DataFormatTools {
    private static final Pattern lineSplitPattern = Pattern.compile("[\\r\\n\\s]*\\n[\\r\\n\\s]*");

    /**
     * Extract text between the first subsequent occurrences of start and end in haystack
     *
     * @param haystack The text to search from
     * @param start    The text after which to start extracting
     * @param end      The text before which to stop extracting
     * @return The extracted string
     */
    public static String extractBetween(String haystack, String start, String end) {
        int startMatch = haystack.indexOf(start);

        if (startMatch >= 0) {
            int startPosition = startMatch + start.length();
            int endPosition = haystack.indexOf(end, startPosition);

            if (endPosition >= 0) {
                return haystack.substring(startPosition, endPosition);
            }
        }

        return null;
    }

    public static String extractBetween(String haystack, TextRange[] candidates) {
        for (TextRange candidate : candidates) {
            String result = extractBetween(haystack, candidate.start, candidate.end);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    public static String extractAfter(String haystack, String start) {
        int startMatch = haystack.indexOf(start);

        if (startMatch >= 0) {
            return haystack.substring(startMatch + start.length());
        }

        return null;
    }

    public static String extractAfter(String haystack, String[] candidates) {
        for (String candidate : candidates) {
            String result = extractAfter(haystack, candidate);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    public static boolean isNullOrEmpty(String text) {
        return text == null || text.isEmpty();
    }

    /**
     * Converts name value pairs to a map, with the last entry for each name being present.
     *
     * @param pairs Name value pairs to convert
     * @return The resulting map
     */
    public static Map<String, String> convertToMapLayout(Collection<NameValuePair> pairs) {
        Map<String, String> map = new HashMap<>();
        for (NameValuePair pair : pairs) {
            map.put(pair.getName(), pair.getValue());
        }
        return map;
    }

    public static Map<String, String> convertToMapLayout(String response) {
        Map<String, String> map = new HashMap<>();
        StringTokenizer st = new StringTokenizer(response, "\n\r");
        while (st.hasMoreTokens()) {
            String[] keyValue = st.nextToken().split("=", 2);
            if (keyValue.length >= 2) {
                map.put(keyValue[0], keyValue[1]);
            }
        }
        return map;
    }

    public static Map<String, String> decodeUrlEncodedItems(String input, boolean escapedSeparator) {
        if (escapedSeparator) {
            input = input.replace("\\\\u0026", "&");
        }

        return convertToMapLayout(URLEncodedUtils.parse(input, StandardCharsets.UTF_8));
    }

    /**
     * Returns the specified default value if the value itself is null.
     *
     * @param value        Value to check
     * @param defaultValue Default value to return if value is null
     * @param <T>          The type of the value
     * @return Value or default value
     */
    public static <T> T defaultOnNull(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * Consumes a stream and returns it as lines.
     *
     * @param inputStream Input stream to consume.
     * @param charset     Character set of the stream
     * @return Lines from the stream
     * @throws IOException On read error
     */
    public static String[] streamToLines(InputStream inputStream, Charset charset) throws IOException {
        String text = IOUtils.toString(inputStream, charset);
        return lineSplitPattern.split(text);
    }

    /**
     * Converts duration in the format HH:mm:ss (or mm:ss or ss) to milliseconds. Does not support day count.
     *
     * @param durationText Duration in text format.
     * @return Duration in milliseconds.
     */
    public static long durationTextToMillis(String durationText) {
        int length = 0;

        for (String part : durationText.split("[:.]")) {
            length = length * 60 + Integer.parseInt(part);
        }

        return length * 1000L;
    }

    /**
     * Writes a string to output with the additional information whether it is <code>null</code> or not. Compatible with
     * {@link #readNullableText(DataInput)}.
     *
     * @param output Output to write to.
     * @param text   Text to write.
     * @throws IOException On write error.
     */
    public static void writeNullableText(DataOutput output, String text) throws IOException {
        output.writeBoolean(text != null);

        if (text != null) {
            output.writeUTF(text);
        }
    }

    /**
     * Reads a string from input which may be <code>null</code>. Compatible with
     * {@link #writeNullableText(DataOutput, String)}.
     *
     * @param input Input to read from.
     * @return The string that was read, or <code>null</code>.
     * @throws IOException On read error.
     */
    public static String readNullableText(DataInput input) throws IOException {
        boolean exists = input.readBoolean();
        return exists ? input.readUTF() : null;
    }

    public static boolean arrayRangeEquals(byte[] array, int offset, byte[] segment) {
        if (array.length < offset + segment.length) {
            return false;
        }

        for (int i = 0; i < segment.length; i++) {
            if (segment[i] != array[i + offset]) {
                return false;
            }
        }

        return true;
    }

    public static class TextRange {
        public final String start;
        public final String end;

        public TextRange(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }
}

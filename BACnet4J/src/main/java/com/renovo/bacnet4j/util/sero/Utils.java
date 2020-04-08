
package com.renovo.bacnet4j.util.sero;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static byte[] commaSeparatedHex(final String s) {
        final String[] parts = s.split(",");
        final byte[] result = new byte[parts.length];
        for (int i = 0; i < parts.length; i++)
            result[i] = (byte) Integer.parseInt(parts[i], 16);
        return result;
    }

    @SafeVarargs
    public static <T> List<T> toList(final T... elements) {
        final List<T> list = new ArrayList<>();
        for (final T e : elements)
            list.add(e);
        return list;
    }
}

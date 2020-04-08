
package com.renovo.bacnet4j.enums;

import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public enum MaxApduLength {
    UP_TO_50(0, 50), // MinimumMessageSize
    UP_TO_128(1, 128), //
    UP_TO_206(2, 206), // Fits in a LonTalk frame
    UP_TO_480(3, 480), // Fits in an ARCNET frame
    UP_TO_1024(4, 1024), //
    UP_TO_1476(5, 1476); // Fits in an ISO 8802-3 frame

    private byte id;
    private UnsignedInteger maxLength;

    MaxApduLength(final int id, final int maxLength) {
        this.id = (byte) id;
        this.maxLength = new UnsignedInteger(maxLength);
    }

    public byte getId() {
        return id;
    }

    public int getMaxLengthInt() {
        return maxLength.intValue();
    }

    public UnsignedInteger getMaxLength() {
        return maxLength;
    }

    public static MaxApduLength valueOf(final byte id) {
        if (id == UP_TO_50.id)
            return UP_TO_50;
        if (id == UP_TO_128.id)
            return UP_TO_128;
        if (id == UP_TO_206.id)
            return UP_TO_206;
        if (id == UP_TO_480.id)
            return UP_TO_480;
        if (id == UP_TO_1024.id)
            return UP_TO_1024;
        if (id == UP_TO_1476.id)
            return UP_TO_1476;

        throw new IllegalArgumentException("Unknown id: " + id);
    }
}

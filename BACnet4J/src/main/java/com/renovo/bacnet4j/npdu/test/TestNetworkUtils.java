package com.renovo.bacnet4j.npdu.test;

import com.renovo.bacnet4j.type.constructed.Address;

public class TestNetworkUtils {
    public static Address toAddress(final int id) {
        return new Address(new byte[] { (byte) id });
    }
}

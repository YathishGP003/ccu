
package com.renovo.bacnet4j.service;

import com.renovo.bacnet4j.npdu.NPCI.NetworkPriority;
import com.renovo.bacnet4j.type.constructed.BaseType;

abstract public class Service extends BaseType {
    abstract public byte getChoiceId();

    public NetworkPriority getNetworkPriority() {
        return null;
    }
}


package com.renovo.bacnet4j.transport;

import java.util.Map;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.ResponseConsumer;
import com.renovo.bacnet4j.ServiceFuture;
import com.renovo.bacnet4j.npdu.NPDU;
import com.renovo.bacnet4j.npdu.Network;
import com.renovo.bacnet4j.npdu.NetworkIdentifier;
import com.renovo.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.renovo.bacnet4j.service.unconfirmed.UnconfirmedRequestService;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.enumerated.Segmentation;
import com.renovo.bacnet4j.type.primitive.OctetString;

/**
 * Provides segmentation support for all data link types.
 *
 * @author Matthew
 */
public interface Transport {
    public static final int DEFAULT_TIMEOUT = 6000;
    public static final int DEFAULT_SEG_TIMEOUT = 5000;
    public static final int DEFAULT_SEG_WINDOW = 5;
    public static final int DEFAULT_RETRIES = 3;

    NetworkIdentifier getNetworkIdentifier();

    Network getNetwork();

    LocalDevice getLocalDevice();

    void setLocalDevice(LocalDevice localDevice);

    public void setTimeout(int timeout);

    public int getTimeout();

    public void setSegTimeout(int segTimeout);

    public int getSegTimeout();

    public void setRetries(int retries);

    public int getRetries();

    public void setSegWindow(int segWindow);

    public int getSegWindow();

    void initialize() throws Exception;

    void terminate();

    long getBytesOut();

    long getBytesIn();

    Address getLocalBroadcastAddress();

    void addNetworkRouter(int networkNumber, OctetString mac);

    Map<Integer, OctetString> getNetworkRouters();

    void send(Address address, UnconfirmedRequestService service);

    ServiceFuture send(Address address, int maxAPDULengthAccepted, Segmentation segmentationSupported,
            ConfirmedRequestService service);

    void send(Address address, int maxAPDULengthAccepted, Segmentation segmentationSupported,
            ConfirmedRequestService service, ResponseConsumer consumer);

    void incoming(NPDU npdu);
}

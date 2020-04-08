
package com.renovo.bacnet4j.npdu.test;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.renovo.bacnet4j.enums.MaxApduLength;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.npdu.MessageValidationException;
import com.renovo.bacnet4j.npdu.NPDU;
import com.renovo.bacnet4j.npdu.Network;
import com.renovo.bacnet4j.npdu.NetworkIdentifier;
import com.renovo.bacnet4j.transport.Transport;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.primitive.OctetString;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import com.renovo.bacnet4j.util.sero.ThreadUtils;

/**
 * A network that is useful for unit tests as it simulates a BACnet network within
 * a single process. The static <code>instances</code> field keeps track of all of
 * the currently available networks,
 */
public class TestNetwork extends Network implements Runnable {
    static final Logger LOG = LoggerFactory.getLogger(TestNetwork.class);

    public static final OctetString BROADCAST = new OctetString(new byte[0]);

    private final TestNetworkMap networkMap;
    private final Address address;
    private final int sendDelay;
    private int timeout = 6000;
    private int segTimeout = 1000;

    private volatile boolean running = true;
    private Thread thread;

    /**
     * This is the list of outgoing messages queued up for sending.
     */
    private final Queue<SendData> queue = new ConcurrentLinkedQueue<>();
    private long bytesOut;
    private long bytesIn;

    public TestNetwork(final TestNetworkMap map, final int address, final int sendDelay) {
        this(map, new Address(new byte[] { (byte) address }), sendDelay);
    }

    public TestNetwork(final TestNetworkMap map, final Address address, final int sendDelay) {
        this.networkMap = map;
        this.address = address;
        this.sendDelay = sendDelay;
    }

    public TestNetwork withTimeout(final int timeout) {
        this.timeout = timeout;
        return this;
    }

    public TestNetwork withSegTimeout(final int segTimeout) {
        this.segTimeout = segTimeout;
        return this;
    }

    @Override
    public NetworkIdentifier getNetworkIdentifier() {
        return new TestNetworkIdentifier();
    }

    @Override
    public MaxApduLength getMaxApduLength() {
        return MaxApduLength.UP_TO_1476;
    }

    @Override
    public long getBytesOut() {
        return bytesOut;
    }

    @Override
    public long getBytesIn() {
        return bytesIn;
    }

    @Override
    public void initialize(final Transport transport) throws Exception {
        super.initialize(transport);

        running = true;
        transport.setTimeout(timeout);
        transport.setRetries(0); // no retries, there's no network here after all
        transport.setSegTimeout(segTimeout);

        thread = new Thread(this,
                "BACnet4J test network for address " + (address.getMacAddress().getBytes()[0] & 0xff));
        thread.start();

        networkMap.add(address, this);
    }

    @Override
    public void terminate() {
        networkMap.remove(address);

        running = false;
        ThreadUtils.notifySync(queue);
        if (thread != null)
            ThreadUtils.join(thread);
    }

    @Override
    protected OctetString getBroadcastMAC() {
        return BROADCAST;
    }

    @Override
    public Address[] getAllLocalAddresses() {
        return new Address[] { address };
    }

    @Override
    public Address getLoopbackAddress() {
        return address;
    }

    @Override
    public void sendNPDU(final Address recipient, final OctetString router, final ByteQueue npdu,
            final boolean broadcast, final boolean expectsReply) throws BACnetException {
        final SendData d = new SendData();
        d.recipient = recipient;
        d.data = npdu.popAll();

        queue.add(d);
        ThreadUtils.notifySync(queue);
    }

    @Override
    public void run() {
        while (running) {
            // Check for a message to send.
            final SendData d = queue.poll();

            if (d == null)
                ThreadUtils.waitSync(queue, 2);
            else {
                // Pause before handing off the message.
                ThreadUtils.sleep(sendDelay);

                if (d.recipient.equals(getLocalBroadcastAddress()) || d.recipient.equals(Address.GLOBAL)) {
                    // A broadcast. Send to everyone.
                    for (final TestNetwork network : networkMap)
                        receive(network, d.data);
                } else {
                    // A directed message. Find the network to pass it to.
                    final TestNetwork network = networkMap.get(d.recipient);
                    if (network != null)
                        receive(network, d.data);
                }
            }
        }
    }

    /**
     * Passes the the data over to the given network instance.
     *
     * @param recipient
     * @param data
     */
    private void receive(final TestNetwork recipient, final byte[] data) {
        LOG.debug("Sending data from {} to {}", address, recipient.address);
        recipient.handleIncomingData(new ByteQueue(data), address.getMacAddress());
    }

    @Override
    protected NPDU handleIncomingDataImpl(final ByteQueue queue, final OctetString linkService)
            throws MessageValidationException {
        return parseNpduData(queue, linkService);
    }

    static class SendData {
        Address recipient;
        byte[] data;
    }
}

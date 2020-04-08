
package com.renovo.bacnet4j.npdu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.renovo.bacnet4j.apdu.APDU;
import com.renovo.bacnet4j.enums.MaxApduLength;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.transport.Transport;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.primitive.OctetString;
import com.renovo.bacnet4j.util.sero.ByteQueue;

abstract public class Network {
    static final Logger LOG = LoggerFactory.getLogger(Network.class);

    private final int localNetworkNumber;
    private Transport transport;

    public Network() {
        this(0);
    }

    public Network(final int localNetworkNumber) {
        this.localNetworkNumber = localNetworkNumber;
    }

    public int getLocalNetworkNumber() {
        return localNetworkNumber;
    }

    public void setTransport(final Transport transport) {
        this.transport = transport;
    }

    public Transport getTransport() {
        return transport;
    }

    abstract public long getBytesOut();

    abstract public long getBytesIn();

    abstract public NetworkIdentifier getNetworkIdentifier();

    abstract public MaxApduLength getMaxApduLength();

    public void initialize(final Transport transport) throws Exception {
        this.transport = transport;
    }

    abstract public void terminate();

    public final Address getLocalBroadcastAddress() {
        return new Address(localNetworkNumber, getBroadcastMAC());
    }

    abstract protected OctetString getBroadcastMAC();

    abstract public Address[] getAllLocalAddresses();

    abstract public Address getLoopbackAddress();

    public final void sendAPDU(final Address recipient, final OctetString router, final APDU apdu,
            final boolean broadcast) throws BACnetException {
        final ByteQueue npdu = new ByteQueue();

        NPCI npci;
        if (recipient.isGlobal())
            npci = new NPCI((Address) null);
        else if (isThisNetwork(recipient)) {
            if (router != null)
                throw new RuntimeException(
                        "Invalid arguments: router address provided for local recipient " + recipient);
            npci = new NPCI(null, null, apdu.expectsReply());
        } else {
            if (router == null)
                throw new RuntimeException(
                        "Invalid arguments: router address not provided for remote recipient " + recipient);
            npci = new NPCI(recipient, null, apdu.expectsReply());
        }

        if (apdu.getNetworkPriority() != null)
            npci.priority(apdu.getNetworkPriority());

        npci.write(npdu);

        apdu.write(npdu);

        sendNPDU(recipient, router, npdu, broadcast, apdu.expectsReply());
    }

    public final void sendNetworkMessage(final Address recipient, final OctetString router, final int messageType,
            final byte[] msg, final boolean broadcast, final boolean expectsReply) throws BACnetException {
        final ByteQueue npdu = new ByteQueue();

        NPCI npci;
        if (recipient.isGlobal())
            npci = new NPCI(null, null, expectsReply, messageType, 0);
        else if (isThisNetwork(recipient)) {
            if (router != null)
                throw new RuntimeException("Invalid arguments: router address provided for a local recipient");
            npci = new NPCI(null, null, expectsReply, messageType, 0);
        } else {
            if (router == null)
                throw new RuntimeException("Invalid arguments: router address not provided for a remote recipient");
            npci = new NPCI(recipient, null, expectsReply, messageType, 0);
        }
        npci.write(npdu);

        // Network message
        if (msg != null)
            npdu.push(msg);

        sendNPDU(recipient, router, npdu, broadcast, expectsReply);
    }

    abstract public void sendNPDU(Address recipient, OctetString router, ByteQueue npdu, boolean broadcast,
            boolean expectsReply) throws BACnetException;

    protected OctetString getDestination(final Address recipient, final OctetString link) {
        if (recipient.isGlobal())
            return getLocalBroadcastAddress().getMacAddress();
        if (link != null)
            return link;
        return recipient.getMacAddress();
    }

    public boolean isThisNetwork(final Address address) {
        final int nn = address.getNetworkNumber().intValue();
        return nn == Address.LOCAL_NETWORK || nn == localNetworkNumber;
    }

    protected void handleIncomingData(final ByteQueue queue, final OctetString linkService) {
        try {
            final NPDU npdu = handleIncomingDataImpl(queue, linkService);
            if (npdu != null) {
                LOG.debug("Received NPDU from {}: {}", linkService, npdu);
                getTransport().incoming(npdu);
            }
        } catch (final Exception e) {
            transport.getLocalDevice().getExceptionDispatcher().fireReceivedException(e);
        } catch (final Throwable t) {
            transport.getLocalDevice().getExceptionDispatcher().fireReceivedThrowable(t);
        }
    }

    abstract protected NPDU handleIncomingDataImpl(ByteQueue queue, OctetString linkService) throws Exception;

    public NPDU parseNpduData(final ByteQueue queue, final OctetString linkService) throws MessageValidationException {
        // Network layer protocol control information. See 6.2.2
        final NPCI npci = new NPCI(queue);
        if (npci.getVersion() != 1)
            throw new MessageValidationException("Invalid protocol version: " + npci.getVersion());

        // Check the destination network number and ignore foreign networks requests
        if (npci.hasDestinationInfo()) {
            final int destNet = npci.getDestinationNetwork();
            if (destNet > 0 && destNet != 0xffff && getLocalNetworkNumber() > 0 && getLocalNetworkNumber() != destNet)
                return null;
        }

        Address from;
        if (npci.hasSourceInfo())
            from = new Address(npci.getSourceNetwork(), npci.getSourceAddress());
        else
            from = new Address(linkService);

        OctetString ls = linkService;
        if (isThisNetwork(from)) {
            LOG.debug("Received NPDU from local network. From={}, local={}", from, localNetworkNumber);
            ls = null;
        } else {
            // Remember the network router in case we haven't heard from it before. This may happen if the router did
            // not respond to a WhoIsRouterToNetwork request.
            final int nn = from.getNetworkNumber().intValue();
            if (!transport.getNetworkRouters().containsKey(nn)) {
                LOG.debug("Network router {} to {} is not currently known. Adding to transport's list", linkService,
                        nn);
                transport.addNetworkRouter(nn, linkService);
            }
            LOG.debug("Received NPDU from remote network. From={}, local={}", from, localNetworkNumber);
        }

        if (npci.isNetworkMessage())
            // Network message
            return new NPDU(from, ls, npci.getMessageType(), queue);

        // APDU message
        return new NPDU(from, ls, queue);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + localNetworkNumber;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Network other = (Network) obj;
        if (localNetworkNumber != other.localNetworkNumber)
            return false;
        return true;
    }
}

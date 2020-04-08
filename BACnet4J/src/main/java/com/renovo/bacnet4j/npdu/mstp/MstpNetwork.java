
package com.renovo.bacnet4j.npdu.mstp;

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

public class MstpNetwork extends Network {
    private final MstpNode node;

    public MstpNetwork(final MstpNode node) {
        this(node, 0);
    }

    public MstpNetwork(final MstpNode node, final int localNetworkNumber) {
        super(localNetworkNumber);
        this.node = node;
        node.setNetwork(this);
    }

    public MstpNode getNode() {
        return node;
    }

    @Override
    public MaxApduLength getMaxApduLength() {
        return MaxApduLength.UP_TO_480;
    }

    @Override
    public void initialize(final Transport transport) throws Exception {
        super.initialize(transport);
        node.initialize(transport);
    }

    @Override
    public void terminate() {
        node.terminate();
    }

    @Override
    public NetworkIdentifier getNetworkIdentifier() {
        return new MstpNetworkIdentifier(node.getCommPortId());
    }

    @Override
    protected OctetString getBroadcastMAC() {
        return MstpNetworkUtils.toOctetString((byte) 0xFF);
    }

    @Override
    public Address[] getAllLocalAddresses() {
        return new Address[] { MstpNetworkUtils.toAddress(getLocalNetworkNumber(), node.getThisStation()) };
    }

    @Override
    public Address getLoopbackAddress() {
        return MstpNetworkUtils.toAddress(getLocalNetworkNumber(), node.getThisStation());
    }

    @Override
    public long getBytesOut() {
        return node.getBytesOut();
    }

    @Override
    public long getBytesIn() {
        return node.getBytesIn();
    }

    @Override
    public void sendNPDU(final Address recipient, final OctetString router, final ByteQueue npdu,
            final boolean broadcast, final boolean expectsReply) throws BACnetException {
        final byte[] data = npdu.popAll();

        final OctetString dest = getDestination(recipient, router);
        final byte mstpAddress = MstpNetworkUtils.getMstpAddress(dest);

        if (expectsReply) {
            if (node instanceof SlaveNode)
                throw new RuntimeException("Cannot originate a request from a slave node");

            ((MasterNode) node).queueFrame(FrameType.bacnetDataExpectingReply, mstpAddress, data);
        } else
            node.setReplyFrame(FrameType.bacnetDataNotExpectingReply, mstpAddress, data);
    }

    public void sendTestRequest(final byte destination) {
        if (!(node instanceof MasterNode))
            throw new RuntimeException("Only master nodes can send test requests");
        ((MasterNode) node).queueFrame(FrameType.testRequest, destination, null);
    }

    //
    //
    //
    // Incoming frames
    //
    void receivedFrame(final Frame frame) {
        handleIncomingData(new ByteQueue(frame.getData()), MstpNetworkUtils.toOctetString(frame.getSourceAddress()));
    }

    @Override
    protected NPDU handleIncomingDataImpl(final ByteQueue queue, final OctetString linkService)
            throws MessageValidationException {
        return parseNpduData(queue, linkService);
    }

    //
    //
    // Convenience methods
    //
    public Address getAddress(final byte station) {
        return MstpNetworkUtils.toAddress(getLocalNetworkNumber(), station);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (node == null ? 0 : node.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        final MstpNetwork other = (MstpNetwork) obj;
        if (node == null) {
            if (other.node != null)
                return false;
        } else if (!node.equals(other.node))
            return false;
        return true;
    }
}

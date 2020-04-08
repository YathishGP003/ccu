
package com.renovo.bacnet4j.apdu;

import com.renovo.bacnet4j.type.enumerated.AbortReason;
import com.renovo.bacnet4j.util.sero.ByteQueue;

/**
 * The BACnet-Abort-PDU is used to terminate a transaction between two peers.
 *
 * @author mlohbihler
 */
public class Abort extends AckAPDU {
    public static final byte TYPE_ID = 7;

    /**
     * This parameter shall be TRUE when the Abort PDU is sent by a server. This parameter shall be FALSE when the Abort
     * PDU is sent by a client.
     */
    private final boolean server;

    /**
     * This parameter, of type BACnetAbortReason, contains the reason the transaction with the indicated invoke ID is
     * being aborted.
     */
    private final AbortReason abortReason;

    public Abort(final boolean server, final byte originalInvokeId, final int abortReason) {
        this.server = server;
        this.originalInvokeId = originalInvokeId;
        this.abortReason = AbortReason.forId(abortReason);
    }

    public Abort(final boolean server, final byte originalInvokeId, final AbortReason abortReason) {
        this.server = server;
        this.originalInvokeId = originalInvokeId;
        this.abortReason = abortReason;
    }
        
    @Override
    public byte getPduType() {
        return TYPE_ID;
    }

    @Override
    public boolean isServer() {
        return server;
    }

    public AbortReason getAbortReason() {
        return abortReason;
    }

    @Override
    public void write(final ByteQueue queue) {
        final int data = getShiftedTypeId(TYPE_ID) | (server ? 1 : 0);
        queue.push(data);
        queue.push(originalInvokeId);
        queue.push(abortReason.byteValue());
    }

    Abort(final ByteQueue queue) {
        server = (queue.popU1B() & 1) == 1;
        originalInvokeId = queue.pop();
        abortReason = AbortReason.forId(queue.popU1B());
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (abortReason == null ? 0 : abortReason.hashCode());
        result = PRIME * result + originalInvokeId;
        result = PRIME * result + (server ? 1231 : 1237);
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
        final Abort other = (Abort) obj;        
        if (abortReason == null) {
            if (other.abortReason != null) {
                return false;
            }
        } else if (!abortReason.equals(other.abortReason)) {
            return false;
        }       
        if (originalInvokeId != other.originalInvokeId)
            return false;
        if (server != other.server)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Abort(server=" + server + ", originalInvokeId=" + originalInvokeId + ", abortReason="
                + abortReason + ")";
    }

    @Override
    public boolean expectsReply() {
        return false;
    }
}


package com.renovo.bacnet4j.transport;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.renovo.bacnet4j.exception.BACnetRecoverableException;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.primitive.OctetString;

/**
 * This is a non-thread safe class for maintaining the list of pending requests at a local device. Access to this is
 * exclusively from Transport, which uses a single management thread.
 *
 * @author Matthew
 */
public class UnackedMessages {
    static final Logger LOG = LoggerFactory.getLogger(UnackedMessages.class);

    private final Map<UnackedMessageKey, UnackedMessageContext> requests = new HashMap<>();
    private byte nextInvokeId;

    /**
     * Add a new client-based request to the list of pending requests.
     */
    public UnackedMessageKey addClient(final Address address, final OctetString linkService,
            final UnackedMessageContext ctx) throws BACnetRecoverableException {
        UnackedMessageKey key;

        // Loop until we find a key that is available.
        int attempts = 256;
        while (true) {
            // We set the server value in the key to true so that it matches with the message from the server.
            key = new UnackedMessageKey(address, linkService, nextInvokeId++, true);

            if (requests.containsKey(key)) {
                // Key collision. Try again unless we've tried too many times.
                if (--attempts > 0)
                    continue;
                throw new BACnetRecoverableException(
                        "Cannot enter a client into the un-acked messages list. key=" + key);
            }

            // Found a good id. Use it and exit.
            requests.put(key, ctx);
            break;
        }

        return key;
    }

    /**
     * Add a new server-based request to the list of pending requests. This is used for segmented responses.
     */
    public UnackedMessageKey addServer(final Address address, final OctetString linkService, final byte id,
            final UnackedMessageContext ctx) throws BACnetRecoverableException {
        // We set the server value in the key to false so that it matches with the message from the client.
        final UnackedMessageKey key = new UnackedMessageKey(address, linkService, id, false);

        if (requests.containsKey(key))
            throw new BACnetRecoverableException("Cannot enter a server into the un-acked messages list. key=" + key);
        requests.put(key, ctx);

        return key;
    }

    public void add(final UnackedMessageKey key, final UnackedMessageContext value) {
        requests.put(key, value);
    }

    public UnackedMessageContext remove(final UnackedMessageKey key) {
        return requests.remove(key);
    }

    public Map<UnackedMessageKey, UnackedMessageContext> getRequests() {
        return requests;
    }

    @Override
    public String toString() {
        return "UnackedMessages [requests=" + requests + ", nextInvokeId=" + nextInvokeId + "]";
    }
}

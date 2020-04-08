
package com.renovo.bacnet4j.apdu;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.npdu.NPCI.NetworkPriority;
import com.renovo.bacnet4j.service.unconfirmed.UnconfirmedRequestService;
import com.renovo.bacnet4j.type.constructed.ServicesSupported;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class UnconfirmedRequest extends APDU {
    public static final byte TYPE_ID = 1;

    private byte serviceChoice;

    /**
     * This field is used to allow parsing of only the APDU so that those fields are available in case there is a
     * problem parsing the service request.
     */
    private ByteQueue serviceData;

    /**
     * This parameter shall contain the parameters of the specific service that is being requested, encoded according to
     * the rules of 20.2. These parameters are defined in the individual service descriptions in this standard and are
     * represented in Clause 21 in accordance with the rules of ASN.1.
     */
    private UnconfirmedRequestService service;

    public UnconfirmedRequest(final UnconfirmedRequestService service) {
        this.service = service;
    }

    @Override
    public byte getPduType() {
        return TYPE_ID;
    }

    public UnconfirmedRequestService getService() {
        return service;
    }

    @Override
    public NetworkPriority getNetworkPriority() {
        return service.getNetworkPriority();
    }

    @Override
    public void write(final ByteQueue queue) {
        queue.push(getShiftedTypeId(TYPE_ID));
        queue.push(service.getChoiceId());
        service.write(queue);
    }

    UnconfirmedRequest(final ServicesSupported services, final ByteQueue queue) throws BACnetException {
        queue.pop();
        serviceChoice = queue.pop();
        serviceData = new ByteQueue(queue.popAll());
        UnconfirmedRequestService.checkUnconfirmedRequestService(services, serviceChoice);
    }

    public void parseServiceData() throws BACnetException {
        if (serviceData != null) {
            service = UnconfirmedRequestService.createUnconfirmedRequestService(serviceChoice, serviceData);
            serviceData = null;
        }
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (service == null ? 0 : service.hashCode());
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
        final UnconfirmedRequest other = (UnconfirmedRequest) obj;
        if (service == null) {
            if (other.service != null)
                return false;
        } else if (!service.equals(other.service))
            return false;
        return true;
    }

    @Override
    public boolean expectsReply() {
        return false;
    }
}

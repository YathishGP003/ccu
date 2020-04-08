
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class AuthenticationPolicy extends BaseType {
    private final SequenceOf<Policy> policy;
    private final Boolean orderEnforced;
    private final UnsignedInteger timeout;

    public AuthenticationPolicy(final SequenceOf<Policy> policy, final Boolean orderEnforced,
            final UnsignedInteger timeout) {
        this.policy = policy;
        this.orderEnforced = orderEnforced;
        this.timeout = timeout;
    }

    public SequenceOf<Policy> getPolicy() {
        return policy;
    }

    public Boolean getOrderEnforced() {
        return orderEnforced;
    }

    public UnsignedInteger getTimeout() {
        return timeout;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, policy, 0);
        write(queue, orderEnforced, 1);
        write(queue, timeout, 2);
    }

    @Override
    public String toString() {
        return "AuthenticationPolicy [policy=" + policy + ", orderEnforced=" + orderEnforced + ", timeout=" + timeout
                + "]";
    }

    public AuthenticationPolicy(final ByteQueue queue) throws BACnetException {
        policy = readSequenceOf(queue, Policy.class, 0);
        orderEnforced = read(queue, Boolean.class, 1);
        timeout = read(queue, UnsignedInteger.class, 2);
    }

    public static class Policy extends BaseType {
        private final DeviceObjectReference credentialDataInput;
        private final UnsignedInteger index;

        public Policy(final DeviceObjectReference credentialDataInput, final UnsignedInteger index) {
            this.credentialDataInput = credentialDataInput;
            this.index = index;
        }

        public DeviceObjectReference getCredentialDataInput() {
            return credentialDataInput;
        }

        public UnsignedInteger getIndex() {
            return index;
        }

        @Override
        public String toString() {
            return "Policy [credentialDataInput=" + credentialDataInput + ", index=" + index + "]";
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, credentialDataInput, 0);
            write(queue, index, 1);
        }

        public Policy(final ByteQueue queue) throws BACnetException {
            credentialDataInput = read(queue, DeviceObjectReference.class, 0);
            index = read(queue, UnsignedInteger.class, 1);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (credentialDataInput == null ? 0 : credentialDataInput.hashCode());
            result = prime * result + (index == null ? 0 : index.hashCode());
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
            final Policy other = (Policy) obj;
            if (credentialDataInput == null) {
                if (other.credentialDataInput != null)
                    return false;
            } else if (!credentialDataInput.equals(other.credentialDataInput))
                return false;
            if (index == null) {
                if (other.index != null)
                    return false;
            } else if (!index.equals(other.index))
                return false;
            return true;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (orderEnforced == null ? 0 : orderEnforced.hashCode());
        result = prime * result + (policy == null ? 0 : policy.hashCode());
        result = prime * result + (timeout == null ? 0 : timeout.hashCode());
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
        final AuthenticationPolicy other = (AuthenticationPolicy) obj;
        if (orderEnforced == null) {
            if (other.orderEnforced != null)
                return false;
        } else if (!orderEnforced.equals(other.orderEnforced))
            return false;
        if (policy == null) {
            if (other.policy != null)
                return false;
        } else if (!policy.equals(other.policy))
            return false;
        if (timeout == null) {
            if (other.timeout != null)
                return false;
        } else if (!timeout.equals(other.timeout))
            return false;
        return true;
    }
}

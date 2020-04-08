
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.enumerated.AccessAuthenticationFactorDisable;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class CredentialAuthenticationFactor extends BaseType {
    private final AccessAuthenticationFactorDisable disable;
    private final AuthenticationFactor authenticationFactor;

    public CredentialAuthenticationFactor(final AccessAuthenticationFactorDisable disable,
            final AuthenticationFactor authenticationFactor) {
        this.disable = disable;
        this.authenticationFactor = authenticationFactor;
    }

    public AccessAuthenticationFactorDisable getDisable() {
        return disable;
    }

    public AuthenticationFactor getAuthenticationFactor() {
        return authenticationFactor;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, disable);
        write(queue, authenticationFactor);
    }

    public CredentialAuthenticationFactor(final ByteQueue queue) throws BACnetException {
        disable = read(queue, AccessAuthenticationFactorDisable.class);
        authenticationFactor = read(queue, AuthenticationFactor.class);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (authenticationFactor == null ? 0 : authenticationFactor.hashCode());
        result = prime * result + (disable == null ? 0 : disable.hashCode());
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
        final CredentialAuthenticationFactor other = (CredentialAuthenticationFactor) obj;
        if (authenticationFactor == null) {
            if (other.authenticationFactor != null)
                return false;
        } else if (!authenticationFactor.equals(other.authenticationFactor))
            return false;
        if (disable == null) {
            if (other.disable != null)
                return false;
        } else if (!disable.equals(other.disable))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CredentialAuthenticationFactor [disable=" + disable + ", authenticationFactor=" + authenticationFactor + ']';
    }    
}

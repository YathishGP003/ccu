
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.Unsigned16;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class HostNPort extends BaseType {
    private final HostAddress host;
    private final Unsigned16 port;

    public HostNPort(final HostAddress host, final Unsigned16 port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, host, 0);
        writeOptional(queue, port, 1);
    }

    public HostNPort(final ByteQueue queue) throws BACnetException {
        host = read(queue, HostAddress.class, 0);
        port = read(queue, Unsigned16.class, 1);
    }

    public HostAddress getHost() {
        return host;
    }

    public Unsigned16 getPort() {
        return port;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (host == null ? 0 : host.hashCode());
        result = prime * result + (port == null ? 0 : port.hashCode());
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
        final HostNPort other = (HostNPort) obj;
        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;
        if (port == null) {
            if (other.port != null)
                return false;
        } else if (!port.equals(other.port))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "HostNPort [host=" + host + ", port=" + port + ']';
    }  
}

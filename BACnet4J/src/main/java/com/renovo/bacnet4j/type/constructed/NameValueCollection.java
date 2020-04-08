
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class NameValueCollection extends BaseType {
    private final SequenceOf<NameValue> members;

    public NameValueCollection(final SequenceOf<NameValue> members) {
        this.members = members;
    }

    public NameValueCollection(final ByteQueue queue) throws BACnetException {
        members = readSequenceOf(queue, NameValue.class, 0);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, members, 0);
    }

    public SequenceOf<NameValue> getMembers() {
        return members;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (members == null ? 0 : members.hashCode());
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
        final NameValueCollection other = (NameValueCollection) obj;
        if (members == null) {
            if (other.members != null)
                return false;
        } else if (!members.equals(other.members))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "NameValueCollection [members=" + members + ']';
    }  
}

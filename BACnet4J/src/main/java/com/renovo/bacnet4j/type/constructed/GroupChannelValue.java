
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.Unsigned16;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class GroupChannelValue extends BaseType {
    private final Unsigned16 channel; // 0
    private final UnsignedInteger overridingPriority; // 1 optional
    private final ChannelValue value; //

    public GroupChannelValue(final Unsigned16 channel, final UnsignedInteger overridingPriority,
            final ChannelValue value) {
        this.channel = channel;
        this.overridingPriority = overridingPriority;
        this.value = value;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, channel, 0);
        writeOptional(queue, overridingPriority, 1);
        write(queue, value);
    }

    @Override
    public String toString() {
        return "GroupChannelValue [channel=" + channel + ", overridingPriority=" + overridingPriority + ", value="
                + value + "]";
    }

    public Unsigned16 getChannel() {
        return channel;
    }

    public UnsignedInteger getOverridingPriority() {
        return overridingPriority;
    }

    public ChannelValue getValue() {
        return value;
    }

    public GroupChannelValue(final ByteQueue queue) throws BACnetException {
        channel = read(queue, Unsigned16.class, 0);
        overridingPriority = readOptional(queue, UnsignedInteger.class, 1);
        value = read(queue, ChannelValue.class);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (channel == null ? 0 : channel.hashCode());
        result = prime * result + (overridingPriority == null ? 0 : overridingPriority.hashCode());
        result = prime * result + (value == null ? 0 : value.hashCode());
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
        final GroupChannelValue other = (GroupChannelValue) obj;
        if (channel == null) {
            if (other.channel != null)
                return false;
        } else if (!channel.equals(other.channel))
            return false;
        if (overridingPriority == null) {
            if (other.overridingPriority != null)
                return false;
        } else if (!overridingPriority.equals(other.overridingPriority))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
}

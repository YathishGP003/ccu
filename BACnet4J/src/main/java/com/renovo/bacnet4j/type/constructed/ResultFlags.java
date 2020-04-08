
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.type.primitive.BitString;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ResultFlags extends BitString {
    public ResultFlags(final boolean firstItem, final boolean lastItem, final boolean moreItems) {
        super(new boolean[] { firstItem, lastItem, moreItems });
    }

    public ResultFlags(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    public boolean isFirstItem() {
        return getValue()[0];
    }

    public boolean isLastItem() {
        return getValue()[1];
    }

    public boolean isMoreItems() {
        return getValue()[2];
    }
    
    @Override
    public String toString() {
        return "ResultFlags [first-item=" + isFirstItem() + ", last-item=" + isLastItem() + ", more-items=" + isMoreItems() + "]";
    }   
}

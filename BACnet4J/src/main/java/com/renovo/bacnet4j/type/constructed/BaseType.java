
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.util.sero.ByteQueue;

/**
 * Base type for constructed data types, as opposed to primitives.
 *
 * @author Matthew
 */
abstract public class BaseType extends Encodable {
    @Override
    public void write(final ByteQueue queue, final int contextId) {
        // Write a start tag
        writeContextTag(queue, contextId, true);
        write(queue);
        // Write an end tag
        writeContextTag(queue, contextId, false);
    }
}

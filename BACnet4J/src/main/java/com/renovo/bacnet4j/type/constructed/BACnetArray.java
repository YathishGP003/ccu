
package com.renovo.bacnet4j.type.constructed;

import java.util.ArrayList;
import java.util.List;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.exception.BACnetRuntimeException;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class BACnetArray<E extends Encodable> extends SequenceOf<E> {
    public BACnetArray(final int size, final E defaultValue) {
        super(newList(size, defaultValue));
    }

    private static final <T extends Encodable> List<T> newList(final int size, final T defaultValue) {
        final List<T> list = new ArrayList<>();
        for (int i = 0; i < size; i++)
            list.add(defaultValue);
        return list;
    }

    @SafeVarargs
    public BACnetArray(final E... values) {
        super(values);
    }

    public BACnetArray(final List<E> values) {
        super(values);
    }

    public BACnetArray(final BACnetArray<E> that) {
        super(that.values);
    }

    public BACnetArray(final ByteQueue queue, final int count, final Class<E> clazz) throws BACnetException {
        super(queue, count, clazz);
    }

    public BACnetArray(final ByteQueue queue, final Class<E> clazz, final int contextId) throws BACnetException {
        super(queue, clazz, contextId);
    }

    public BACnetArray<E> putBase1(final int indexBase1, final E value) {
        setBase1(indexBase1, value);
        return this;
    }

    @Override
    public void setBase1(final int indexBase1, final E value) {
        values.set(indexBase1 - 1, value);
    }

    @Override
    public void add(final E value) {
        throw new BACnetRuntimeException("Illegal operation");
    }

    @Override
    public Encodable remove(final int indexBase1) {
        throw new BACnetRuntimeException("Illegal operation");
    }

    @Override
    public void remove(final E value) {
        throw new BACnetRuntimeException("Illegal operation");
    }

    @Override
    public void removeAll(final E value) {
        throw new BACnetRuntimeException("Illegal operation");
    }
}

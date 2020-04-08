
package com.renovo.bacnet4j.type.constructed;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.service.confirmed.ReadRangeRequest.RangeReadable;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class SequenceOf<E extends Encodable> extends BaseType implements Iterable<E>, RangeReadable<E> {
    protected final List<E> values;

    public SequenceOf() {
        values = new ArrayList<>();
    }

    public SequenceOf(final int capacity) {
        values = new ArrayList<>(capacity);
    }

    public SequenceOf(final List<E> values) {
        this.values = values;
    }

    @SafeVarargs
    public SequenceOf(final E... values) {
        this();
        for (final E value : values)
            this.values.add(value);
    }

    @Override
    public void write(final ByteQueue queue) {
        for (final Encodable value : values)
            value.write(queue);
    }

    public SequenceOf(final ByteQueue queue, final Class<E> clazz) throws BACnetException {
        values = new ArrayList<>();
        while (peekTagNumber(queue) != -1)
            values.add(read(queue, clazz));
    }

    public SequenceOf(final ByteQueue queue, final int count, final Class<E> clazz) throws BACnetException {
        values = new ArrayList<>();
        int _count = count;
        while (_count-- > 0)
            values.add(read(queue, clazz));
    }

    public SequenceOf(final ByteQueue queue, final Class<E> clazz, final int contextId) throws BACnetException {
        values = new ArrayList<>();
        while (readEnd(queue) != contextId)
            values.add(read(queue, clazz));
    }

    public E getBase1(final int indexBase1) {
        return values.get(indexBase1 - 1);
    }

    @Override
    public E get(final int index) {
        return values.get(index);
    }

    public boolean has(final UnsignedInteger indexBase1) {
        final int index = indexBase1.intValue() - 1;
        return index >= 0 && index < values.size();
    }

    @Override
    public int size() {
        return values.size();
    }

    public int getCount() {
        return values.size();
    }

    public void setBase1(final int indexBase1, final E value) {
        final int index = indexBase1 - 1;
        while (values.size() <= index)
            values.add(null);
        values.set(index, value);
    }

    public void set(final int index, final E value) {
        values.set(index, value);
    }

    public void add(final E value) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) == null) {
                values.set(i, value);
                return;
            }
        }
        values.add(value);
    }

    public Encodable remove(final int indexBase1) {
        final int index = indexBase1 - 1;
        if (index < values.size())
            return values.remove(index);
        return null;
    }

    public void remove(final E value) {
        if (value == null)
            return;

        for (int i = 0; i < values.size(); i++) {
            if (Objects.equals(values.get(i), value)) {
                remove(i + 1);
                break;
            }
        }
    }

    public void removeAll(final E value) {
        for (final ListIterator<E> it = values.listIterator(); it.hasNext();) {
            final E e = it.next();
            if (Objects.equals(e, value))
                it.remove();
        }
    }

    public boolean contains(final E value) {
        for (final E e : values) {
            if (Objects.equals(e, value))
                return true;
        }
        return false;
    }

    public int indexOf(final Object value) {
        return values.indexOf(value);
    }

    @Override
    public Iterator<E> iterator() {
        return values.iterator();
    }

    @Override
    public String toString() {
        return values.toString();
    }

    public List<E> getValues() {
        return values;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (values == null ? 0 : values.hashCode());
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
        final SequenceOf<?> other = (SequenceOf<?>) obj;
        if (values == null) {
            if (other.values != null)
                return false;
        } else if (!values.equals(other.values))
            return false;
        return true;
    }
}

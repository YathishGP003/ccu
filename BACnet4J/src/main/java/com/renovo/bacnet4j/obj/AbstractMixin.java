
package com.renovo.bacnet4j.obj;

import java.util.Map;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.ValueSource;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;

/**
 * Mixins allow different objects to share functionality that is otherwise common between them. Functionality that is
 * specific to a given object type should still be coded into the class for the type itself.
 *
 * @author Matthew
 */
public class AbstractMixin {
    private final BACnetObject bo;

    public AbstractMixin(final BACnetObject bo) {
        this.bo = bo;
    }

    //
    //
    // Utility methods for subclasses to be able to access object properties.
    //
    protected final Map<PropertyIdentifier, Encodable> properties() {
        return bo.properties;
    }

    // Adds the property to the properties map, and updates the property list as required.
    protected final void addProperty(final PropertyIdentifier pid, final Encodable value, final boolean overwrite) {
        if (overwrite || !properties().containsKey(pid)) {
            properties().put(pid, value);
        }
    }

    protected final void writePropertyInternal(final PropertyIdentifier pid, final Encodable value) {
        bo.writePropertyInternal(pid, value);
    }

    protected final void set(final PropertyIdentifier pid, final Encodable value) {
        bo.set(pid, value);
    }

    protected final <T extends Encodable> T get(final PropertyIdentifier pid) {
        return bo.get(pid);
    }

    protected final <T extends Encodable> T get(final PropertyIdentifier pid, final T defaultValue) {
        final T value = get(pid);
        return value == null ? defaultValue : value;
    }

    protected final LocalDevice getLocalDevice() {
        return bo.getLocalDevice();
    }

    protected final ObjectIdentifier getId() {
        return bo.getId();
    }

    //
    //
    // Methods for subclasses to override as needed.
    //
    /**
     * Allow the mixin a chance to perform actions before the property is read.
     *
     * @param pid
     * @throws BACnetServiceException
     *             if for some reason the read should be prevented.
     */
    protected void beforeReadProperty(final PropertyIdentifier pid) throws BACnetServiceException {
        // no op
    }

    /**
     * Allow the mixin to override the property validation.
     *
     * @param valueSource
     * @param value
     * @return true of the validation was handled, false otherwise.
     * @throws BACnetServiceException
     *             if the property was so bad that an exception had to be thrown
     */
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        return false;
    }

    /**
     * Allow the mixin to override the property write.
     *
     * @param valueSource
     * @param value
     * @return true of the write was handled, false otherwise.
     * @throws BACnetServiceException
     */
    protected boolean writeProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        return false;
    }

    /**
     * @param pid
     * @param oldValue
     * @param newValue
     */
    protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        // no op
    }

    /**
     * Provides notice to the mixin that the object should initialize itself.
     */
    protected void initialize() {
        // no op
    }

    /**
     * Provides notice to the mixin that the object was removed from the device.
     */
    protected void terminate() {
        // no op
    }
}

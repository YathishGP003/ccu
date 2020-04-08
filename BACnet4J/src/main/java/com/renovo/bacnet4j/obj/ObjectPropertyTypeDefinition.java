
package com.renovo.bacnet4j.obj;

import com.renovo.bacnet4j.type.enumerated.ObjectType;

public class ObjectPropertyTypeDefinition {
    private final ObjectType objectType;
    private final boolean required;
    private final PropertyTypeDefinition propertyTypeDefinition;

    ObjectPropertyTypeDefinition(final ObjectType objectType, final boolean required,
            final PropertyTypeDefinition propertyTypeDefinition) {
        this.objectType = objectType;
        this.required = required;
        this.propertyTypeDefinition = propertyTypeDefinition;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public PropertyTypeDefinition getPropertyTypeDefinition() {
        return propertyTypeDefinition;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isOptional() {
        return !required;
    }
}

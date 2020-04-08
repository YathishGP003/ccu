
package com.renovo.bacnet4j.type.constructed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.primitive.Primitive;

public class ChoiceOptions {
    private final List<Class<? extends Primitive>> primitives;
    private final Map<Integer, ContextualType> contextual;

    public ChoiceOptions() {
        this.primitives = new ArrayList<>();
        this.contextual = new HashMap<>();
    }
    
    public ChoiceOptions(List<Class<? extends Primitive>> primitives,  Map<Integer, ContextualType> contextual) {
        this.primitives = primitives;
        this.contextual = contextual;
    }
    
    public List<Class<? extends Primitive>> getPrimitives() {
        return primitives;
    }
    
    public Map<Integer, ContextualType> getContextual() {
        return contextual;
    }
    
    public void addPrimitive(final Class<? extends Primitive> primitive) {
        primitives.add(primitive);
    }

    public void addContextual(final Integer contextId, final Class<? extends Encodable> clazz) {
        contextual.put(contextId, new ContextualType(clazz, false));
    }

    public void addContextualSequence(final Integer contextId, final Class<? extends Encodable> clazz) {
        contextual.put(contextId, new ContextualType(clazz, true));
    }

    public ContextualType getContextualClass(final int contextId) {
        return contextual.get(contextId);
    }

    public boolean containsPrimitive(final Class<? extends Primitive> clazz) {
        return primitives.contains(clazz);
    }

    public int getContextId(final Class<? extends Encodable> clazz, final boolean sequence) {
        for (final Map.Entry<Integer, ContextualType> e : contextual.entrySet()) {
            if (e.getValue().clazz.equals(clazz) && e.getValue().sequence == sequence)
                return e.getKey();
        }
        return -1;
    }

    public static class ContextualType {
        private final Class<? extends Encodable> clazz;
        private final boolean sequence;

        public ContextualType(final Class<? extends Encodable> clazz, final boolean sequence) {
            this.clazz = clazz;
            this.sequence = sequence;
        }

        public Class<? extends Encodable> getClazz() {
            return clazz;
        }

        public boolean isSequence() {
            return sequence;
        }
    }
}

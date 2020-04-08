
package com.renovo.bacnet4j.type;

import java.util.ArrayList;
import java.util.List;

public class ThreadLocalObjectTypePropertyReferenceStack {
    private static ThreadLocal<List<ObjectTypePropertyReference>> objTypePropRef = new ThreadLocal<>();

    public static void set(final ObjectTypePropertyReference objectTypePropertyReference) {
        List<ObjectTypePropertyReference> stack = objTypePropRef.get();

        if (stack == null) {
            stack = new ArrayList<>();
            objTypePropRef.set(stack);
        }

        stack.add(objectTypePropertyReference);
    }

    public static ObjectTypePropertyReference get() {
        final List<ObjectTypePropertyReference> stack = objTypePropRef.get();
        if (stack == null)
            return null;
        return stack.get(stack.size() - 1);
    }

    public static void remove() {
        final List<ObjectTypePropertyReference> stack = objTypePropRef.get();
        if (stack == null)
            return;

        if (stack.size() <= 1)
            objTypePropRef.remove();
        else
            stack.remove(stack.size() - 1);
    }
}

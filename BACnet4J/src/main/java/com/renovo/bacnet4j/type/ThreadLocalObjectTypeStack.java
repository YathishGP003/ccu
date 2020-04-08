
package com.renovo.bacnet4j.type;

import java.util.ArrayList;
import java.util.List;

import com.renovo.bacnet4j.type.enumerated.ObjectType;

public class ThreadLocalObjectTypeStack {
    private static ThreadLocal<List<ObjectType>> objType = new ThreadLocal<>();

    public static void set(final ObjectType objectType) {
        List<ObjectType> stack = objType.get();

        if (stack == null) {
            stack = new ArrayList<>();
            objType.set(stack);
        }

        stack.add(objectType);
    }

    public static ObjectType get() {
        final List<ObjectType> stack = objType.get();
        if (stack == null)
            return null;
        return stack.get(stack.size() - 1);
    }

    public static void remove() {
        final List<ObjectType> stack = objType.get();
        if (stack == null)
            return;

        if (stack.size() <= 1)
            objType.remove();
        else
            stack.remove(stack.size() - 1);
    }
}

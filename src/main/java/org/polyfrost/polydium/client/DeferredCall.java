package org.polyfrost.polydium.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

final class DeferredCall<T> {
    private static final String REF_PACKAGE = "com.llamalad7.mixinextras.sugar.ref.";
    private static final byte LIVE = 0;
    private static final byte DISPOSED = 2;
    private static final ClassValue<Field> STATE = new ClassValue<>() {
        @Override
        protected Field computeValue(Class<?> type) {
            try {
                Field field = type.getDeclaredField("state");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("Unknown LocalRef layout: " + type.getName(), e);
            }
        }
    };
    private static final ClassValue<Field[]> REFERENCE_FIELDS = new ClassValue<>() {
        @Override
        protected Field[] computeValue(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            for (Field field : type.getDeclaredFields()) {
                if (field.getType().isPrimitive() || Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                fields.add(field);
            }
            return fields.toArray(new Field[0]);
        }
    };

    private final Operation<T> operation;
    private final List<Object> refs = new ArrayList<>();

    DeferredCall(Operation<T> operation) throws IllegalAccessException {
        this.operation = operation;
        collect(operation, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    T call(Object... args) {
        setState(LIVE);
        try {
            return operation.call(args);
        } finally {
            setState(DISPOSED);
        }
    }

    private void setState(byte state) {
        try {
            for (Object ref : refs) {
                STATE.get(ref.getClass()).setByte(ref, state);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private void collect(Object object, Set<Object> seen) throws IllegalAccessException {
        if (object == null || !seen.add(object)) return;
        if (object instanceof Object[] array) {
            for (Object element : array) collect(element, seen);
        } else if (isLocalRef(object.getClass())) {
            STATE.get(object.getClass());
            refs.add(object);
        } else if (object instanceof Operation) {
            for (Field field : REFERENCE_FIELDS.get(object.getClass())) {
                collect(field.get(object), seen);
            }
        }
    }

    private static boolean isLocalRef(Class<?> type) {
        for (Class<?> iface : type.getInterfaces()) {
            if (iface.getName().startsWith(REF_PACKAGE)) return true;
        }
        return false;
    }
}

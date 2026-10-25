package org.taumc.celeritas.lwjgl;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * LWJGLService for tests without a GL context: real memory through Unsafe, and buffer calls recorded in
 * {@link #LOG} instead of reaching GL. Any other GL call throws.
 */
public final class FakeGlLWJGLService {
    private static final Unsafe UNSAFE = unsafe();

    /** Buffer calls as "gen 1", "data 1", "del 1". */
    public static final List<String> LOG = new ArrayList<>();

    private static int nextName = 1;
    private static int boundBuffer;

    private FakeGlLWJGLService() {
    }

    public static void reset() {
        LOG.clear();
        nextName = 1;
    }

    public static LWJGLService create() {
        return (LWJGLService) Proxy.newProxyInstance(LWJGLService.class.getClassLoader(),
                new Class<?>[] { LWJGLService.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "isOpenGLVersionSupported" -> true;
                    case "getPointerSize" -> 8;
                    case "nmemAlloc", "nmemAlignedAlloc" -> UNSAFE.allocateMemory((long) args[args.length - 1]);
                    case "nmemFree", "nmemAlignedFree" -> {
                        UNSAFE.freeMemory((long) args[0]);
                        yield null;
                    }
                    case "memSet" -> {
                        UNSAFE.setMemory((long) args[0], (long) args[2], (byte) (int) args[1]);
                        yield null;
                    }
                    case "memPutByte" -> {
                        UNSAFE.putByte((long) args[0], (byte) args[1]);
                        yield null;
                    }
                    case "memPutInt" -> {
                        UNSAFE.putInt((long) args[0], (int) args[1]);
                        yield null;
                    }
                    case "memPutLong", "memPutAddress" -> {
                        UNSAFE.putLong((long) args[0], (long) args[1]);
                        yield null;
                    }
                    case "memGetByte" -> UNSAFE.getByte((long) args[0]);
                    case "memGetInt" -> UNSAFE.getInt((long) args[0]);
                    case "memGetLong", "memGetAddress" -> UNSAFE.getLong((long) args[0]);
                    case "glGenBuffers" -> {
                        int name = nextName++;
                        LOG.add("gen " + name);
                        yield name;
                    }
                    case "glDeleteBuffers" -> {
                        LOG.add("del " + args[0]);
                        yield null;
                    }
                    case "glBindBuffer" -> {
                        boundBuffer = (int) args[1];
                        yield null;
                    }
                    case "glBufferData" -> {
                        LOG.add("data " + boundBuffer);
                        yield null;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "FakeGlLWJGLService";
                    default -> throw new UnsupportedOperationException("FakeGlLWJGLService: " + method.getName());
                });
    }

    private static Unsafe unsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}

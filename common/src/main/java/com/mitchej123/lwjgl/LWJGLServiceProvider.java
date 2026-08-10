package com.mitchej123.lwjgl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Loads LWJGLService via ServiceLoader, picking highest priority.
 */
public final class LWJGLServiceProvider {
    private static final Logger LOGGER = LogManager.getLogger("Celeritas/LWJGLService");
    private static final int MAX_ATTEMPTS = 16;

    public static final LWJGLService LWJGL = loadService();
    public static final int POINTER_SIZE = LWJGL.getPointerSize();
    public static final long NULL = 0L;

    private LWJGLServiceProvider() {}

    private static LWJGLService loadService() {
        ServiceLoader<LWJGLService> loader = ServiceLoader.load(LWJGLService.class, LWJGLService.class.getClassLoader());
        Iterator<LWJGLService> iterator = loader.iterator();

        LWJGLService best = null;

        int attempts = 0;
        while (attempts < MAX_ATTEMPTS) {
            try {
                attempts++;
                if (!iterator.hasNext()) break;

                LWJGLService service = iterator.next();
                int priority = service.getPriority();
                if (priority == LWJGLService.PRIORITY_UNAVAILABLE) {
                    LOGGER.info("Skipping unavailable LWJGLService: {}", service.getClass().getName());
                    continue;
                }
                LOGGER.info("Found LWJGLService: {} (priority {})", service.getClass().getName(), priority);
                if (best == null || priority > best.getPriority()) {
                    best = service;
                }
            } catch (ServiceConfigurationError | LinkageError e) {
                LOGGER.debug("Skipping unavailable service: {}", e.getMessage());
            }
        }

        if (best != null) {
            LOGGER.info("Using LWJGLService: {} (priority {})", best.getClass().getName(), best.getPriority());
            return best;
        }

        throw new IllegalStateException("No LWJGLService implementation found.");
    }
}

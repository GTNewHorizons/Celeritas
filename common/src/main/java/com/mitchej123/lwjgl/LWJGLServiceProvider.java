package com.mitchej123.lwjgl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Loads LWJGLService via ServiceLoader, picking highest priority.
 */
public final class LWJGLServiceProvider {
    private static final Logger LOGGER = LogManager.getLogger("Celeritas/LWJGLService");

    public static final LWJGLService LWJGL = loadService();
    public static final int POINTER_SIZE = LWJGL.getPointerSize();
    public static final long NULL = 0L;

    private LWJGLServiceProvider() {}

    private static LWJGLService loadService() {
        ServiceLoader<LWJGLService> loader = ServiceLoader.load(LWJGLService.class, LWJGLService.class.getClassLoader());

        LWJGLService best = null;
        var providers = loader.stream().iterator();
        while (providers.hasNext()) {
            var provider = providers.next();
            try {
                LWJGLService service = provider.get();
                LOGGER.debug("Found LWJGLService: {} (priority {})", service.getClass().getName(), service.getPriority());
                if (best == null || service.getPriority() > best.getPriority()) {
                    best = service;
                }
            } catch (ServiceConfigurationError | LinkageError e) {
                LOGGER.debug("Skipping unavailable service {}: {}", provider.type().getName(), e.getMessage());
            }
        }

        if (best != null) {
            LOGGER.info("Using LWJGLService: {} (priority {})", best.getClass().getName(), best.getPriority());
            return best;
        }

        throw new IllegalStateException("No LWJGLService implementation found.");
    }
}

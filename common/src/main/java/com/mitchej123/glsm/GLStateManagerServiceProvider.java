package com.mitchej123.glsm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Loads GLStateManagerService via ServiceLoader, picking highest priority.
 */
public final class GLStateManagerServiceProvider {
    private static final Logger LOGGER = LogManager.getLogger("Celeritas/GLStateManager");

    public static final GLStateManagerService GL_STATE_MANAGER = loadService();

    private GLStateManagerServiceProvider() {}

    private static GLStateManagerService loadService() {
        ServiceLoader<GLStateManagerService> loader = ServiceLoader.load(GLStateManagerService.class, GLStateManagerService.class.getClassLoader());

        GLStateManagerService best = null;
        var providers = loader.stream().iterator();
        while (providers.hasNext()) {
            var provider = providers.next();
            try {
                GLStateManagerService service = provider.get();
                LOGGER.debug("Found GLStateManagerService: {} (priority {})", service.getClass().getName(), service.getPriority());
                if (best == null || service.getPriority() > best.getPriority()) {
                    best = service;
                }
            } catch (ServiceConfigurationError | LinkageError e) {
                LOGGER.debug("Skipping unavailable service {}: {}", provider.type().getName(), e.getMessage());
            }
        }

        if (best != null) {
            LOGGER.info("Using GLStateManagerService: {} (priority {})", best.getClass().getName(), best.getPriority());
            return best;
        }

        throw new IllegalStateException("No GLStateManagerService implementation found.");
    }
}

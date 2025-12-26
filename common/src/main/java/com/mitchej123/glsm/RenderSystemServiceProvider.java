package com.mitchej123.glsm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Loads RenderSystemService via ServiceLoader, picking highest priority.
 */
public final class RenderSystemServiceProvider {
    private static final Logger LOGGER = LogManager.getLogger("Celeritas/RenderSystem");

    public static final RenderSystemService RENDER_SYSTEM = loadService();

    private RenderSystemServiceProvider() {}

    private static RenderSystemService loadService() {
        ServiceLoader<RenderSystemService> loader = ServiceLoader.load(RenderSystemService.class, RenderSystemService.class.getClassLoader());

        RenderSystemService best = null;
        var providers = loader.stream().iterator();
        while (providers.hasNext()) {
            var provider = providers.next();
            try {
                RenderSystemService service = provider.get();
                LOGGER.debug("Found RenderSystemService: {} (priority {})", service.getClass().getName(), service.getPriority());
                if (best == null || service.getPriority() > best.getPriority()) {
                    best = service;
                }
            } catch (ServiceConfigurationError | LinkageError e) {
                LOGGER.debug("Skipping unavailable service {}: {}", provider.type().getName(), e.getMessage());
            }
        }

        if (best != null) {
            LOGGER.info("Using RenderSystemService: {} (priority {})", best.getClass().getName(), best.getPriority());
            return best;
        }

        throw new IllegalStateException("No RenderSystemService implementation found.");
    }
}

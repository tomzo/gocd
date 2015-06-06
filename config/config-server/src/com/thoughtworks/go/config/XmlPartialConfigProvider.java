package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.PartialConfig;
import org.apache.commons.lang.NotImplementedException;

import java.util.List;

/**
 * Simple configuration provider.
 * Searches for .pipeline.xml in the source tree
 * and parses contents into configuration objects.
 */
public class XmlPartialConfigProvider implements PartialConfigProvider {

    // TODO sensible pattern
    // TODO optional override from provider config
    private String pattern = ".pipeline.xml";

    @Override
    public PartialConfig Load(String configRepoCheckoutDirectory, ConfigurationLoadContext context) {
        throw new NotImplementedException();
    }
}

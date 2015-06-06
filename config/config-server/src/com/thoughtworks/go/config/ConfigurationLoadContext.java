package com.thoughtworks.go.config;

/**
 * Available for PartialConfigProvider when loading configuration.
 */
public interface ConfigurationLoadContext {
    CruiseConfig getMainConfig();
}

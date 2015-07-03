package com.thoughtworks.go.config;

import com.thoughtworks.go.config.parts.XmlPartialConfigProvider;
import com.thoughtworks.go.config.plugin.ConfigRepoPlugin;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides config plugins implementations
 */
@Component
public class GoConfigPluginService {

    private final ConfigRepoExtension crExtension;
    private final XmlPartialConfigProvider embeddedXmlPlugin;

    @Autowired public GoConfigPluginService(ConfigRepoExtension configRepoExtension,
            ConfigCache configCache,ConfigElementImplementationRegistry configElementImplementationRegistry,
            MetricsProbeService metricsProbeService)
    {
        this.crExtension = configRepoExtension;
        MagicalGoConfigXmlLoader loader = new MagicalGoConfigXmlLoader(configCache, configElementImplementationRegistry, metricsProbeService);
        embeddedXmlPlugin = new XmlPartialConfigProvider(loader);
    }

    public PartialConfigProvider partialConfigProviderFor(ConfigRepoConfig repoConfig)
    {
        String pluginId = repoConfig.getConfigProviderPluginName();
        return partialConfigProviderFor(pluginId);
    }

    public PartialConfigProvider partialConfigProviderFor(String pluginId) {
        if(pluginId == null || pluginId.equals(XmlPartialConfigProvider.ProviderName))
            return embeddedXmlPlugin;

        return new ConfigRepoPlugin(crExtension,pluginId);
    }
}

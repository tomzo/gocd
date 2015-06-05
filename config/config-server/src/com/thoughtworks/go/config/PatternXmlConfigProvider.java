package com.thoughtworks.go.config;

import java.util.List;

/**
 * Simple configuration provider.
 * Searches for .pipeline.xml in the source tree
 * and parses contents into configuration objects.
 */
public class PatternXmlConfigProvider implements ConfigProvider {

    // TODO sensible pattern
    // TODO optional override from provider config
    private String pattern = ".pipeline.xml";

    @Override
    public List<PipelineConfig> allPipelines(String directory) {
        // TODO scan directory searching for pipeline.xml files and create configs
        return null;
    }

    @Override
    public PipelineConfigs pipelines(String directory, String groupName) {
        return null;
    }
}

package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.config.EnvironmentsConfig;

/**
 * Part of cruise configuration that can be stored outside of main cruise-config.xml.
 * It can be merged with others and main configuration.
 */
public class PartialConfig {
    // consider to include source of this part.

    private EnvironmentsConfig environments;

    // TODO lists of pipelines in some groups.
    // but without Authorization section because it makes no sense to store it in repo.
    // so we need other class than PipelineConfigs

}

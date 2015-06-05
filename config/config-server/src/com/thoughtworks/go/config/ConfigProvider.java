package com.thoughtworks.go.config;

import java.util.List;

/**
 * Can obtain configuration objects from a source code tree.
 * Possible extension point for custom pipeline configuration format.
 * Expects a checked-out source code tree.
 * It does not understand versioning.
 * Each implementation defines its own pattern
 * to identify configuration files in repository structure.
 */
public interface ConfigProvider {

    // TODO consider: context in arguments bigger than just directory

    // TODO consider: could have Parse() whose result is
    // stored by Go in memory so that single checkout is parsed only once.

    List<PipelineConfig> allPipelines(String directory);

    PipelineConfigs pipelines(String directory,String groupName);

    // any further elements that could be obtained from config repo
}

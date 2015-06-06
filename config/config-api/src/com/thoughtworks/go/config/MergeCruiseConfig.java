package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.PartialConfig;

import java.util.List;

/**
 *
 */
public class MergeCruiseConfig {//TODO #1133 extends CruiseConfig or replace CruiseConfig by interface and make 2 independent implementations
    private CruiseConfig mainConfig;
    private List<PartialConfig> parts;

    //TODO #1133 implement each method by merge
}

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigFileDao;
import com.thoughtworks.go.config.remote.PartialConfig;
import org.apache.commons.lang.NotImplementedException;

import java.util.List;

/**
 * Understands that configuration consists of main part and many remote configurations.
 */
public class ConfigMergeService {
    private GoConfigFileDao goConfigFileDao;
    private PartialConfigsService remoteConfigService;

    private CruiseConfig cruiseConfig() {
        return goConfigFileDao.load();
    }

    private List<PartialConfig> parts() {
        throw new NotImplementedException();
    }
}

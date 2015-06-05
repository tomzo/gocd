package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.ConfigurationMaterialConfig;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.server.materials.MaterialUpdateCompletedMessage;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.util.CollectionUtil;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * understands section of config which needs polling to get configuration objects.
 * Provides configuration with and its history.
 */
@Service
public class DynamicConfigService implements GoMessageListener<MaterialUpdateCompletedMessage>, ConfigChangedListener {
    private GoConfigFileDao goConfigFileDao;

    private CruiseConfig cruiseConfig() {
        return goConfigFileDao.load();
    }

    private ConfigProvider getConfigProvider(Material material){
        // TODO identify configuration of this material, use name of the plugin to get proper provider
        throw new NotImplementedException();
    }

    public List<ScmMaterialConfig> getConfigurationMaterials() {
        DynamicConfigSources sources = cruiseConfig().dynamicConfigSources();
        List<ScmMaterialConfig> materials = new ArrayList<ScmMaterialConfig>();
        for(ConfigurationMaterialConfig config : sources)
        {
            materials.add(config.getMaterialConfig());
        }
        return materials;
    }

    // we start with PipelineConfigs but it could be more
    public PipelineConfigs getPipelinesConfiguration(String materialFingerprint) {
        // just get latest
        throw new NotImplementedException();
    }
    public PipelineConfigs getPipelinesConfiguration(String materialFingerprint, Revision revision) {
        // checkout code at specified revision
        // get proper ConfigProvider to scan the repository and create configuration instances
        throw new NotImplementedException();
    }
    // TODO other methods similar to GoConfigService

    @Override
    public void onMessage(MaterialUpdateCompletedMessage message) {
        Material material = message.getMaterial();
        String folder = material.getFolder();
        //TODO if this is listed as configuration material then update lastest configuration
        throw new NotImplementedException();
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        // TODO reload list of config materials, drop removed sources
        throw new NotImplementedException();
    }
}

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.RemoteSourceConfig;
import com.thoughtworks.go.config.remote.RemoteSourcesConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.server.materials.MaterialUpdateCompletedMessage;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import org.apache.commons.lang.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * understands section of config which needs polling to get configuration objects.
 * Provides configuration and its history.
 */
//@Service
public class RemoteConfigService implements GoMessageListener<MaterialUpdateCompletedMessage>, ConfigChangedListener {
    //TODO actually all static config that this needs is the config-repos
    private GoConfigFileDao goConfigFileDao;
    // config provider for each repository type
    private Map<String, ConfigProvider> configProviderMap ;

    // maybe needed to convert from config to materials
    private MaterialConfigConverter materialConverter;

    private CruiseConfig cruiseConfig() {
        return goConfigFileDao.load();
    }

    private ConfigProvider getConfigProvider(Material material){
        // TODO identify configuration of this material, use name of the plugin to get proper provider
        throw new NotImplementedException();
    }

    public List<ScmMaterialConfig> getConfigurationMaterials() {
        RemoteSourcesConfig sources = cruiseConfig().dynamicConfigSources();
        List<ScmMaterialConfig> materials = new ArrayList<ScmMaterialConfig>();
        for(RemoteSourceConfig config : sources)
        {
            materials.add(config.getMaterialConfig());
        }
        return materials;
    }

    //TODO merge from many remote sources

    public PartialConfig getPartialConfig(String materialFingerprint) {
        // just get latest
        throw new NotImplementedException();
    }
    public PartialConfig getPartialConfig(String materialFingerprint, Revision revision) {
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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.server.materials.MaterialUpdateCompletedMessage;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * understands section of config which needs polling to get configuration objects.
 * Provides configuration parts and their history.
 */
@Service
public class PartialConfigsService implements GoMessageListener<MaterialUpdateCompletedMessage>, ConfigChangedListener {
    //TODO actually all static config that this needs is the config-repos

    // config provider for each repository type
    private Map<String, PartialConfigProvider> configProviderMap ;
    // private Map<MaterialConfig, LastestConfigPartHolder> configPartHolders;

    // maybe needed to convert from config to materials
    private MaterialConfigConverter materialConverter;

    /*
    @Autowired
    public PartialConfigsService(GoConfigFileDao goConfigFileDao) {
        this.goConfigFileDao = goConfigFileDao;
    }*/


    private ConfigReposConfig remoteConfigSources() {
        throw new NotImplementedException();
        //return cruiseConfig().remoteConfigSources();
    }

    private PartialConfigProvider getConfigProvider(Material material){
        // TODO identify configuration of this material, use name of the plugin to get proper provider
        throw new NotImplementedException();
    }

    public List<ScmMaterialConfig> getConfigurationMaterials() {
        ConfigReposConfig sources = remoteConfigSources();
        List<ScmMaterialConfig> materials = new ArrayList<ScmMaterialConfig>();
        for(ConfigRepoConfig config : sources)
        {
            materials.add(config.getMaterialConfig());
        }
        return materials;
    }

    public boolean hasConfiguration(Material material){
        MaterialConfig config = material.config();
        ConfigReposConfig sources = remoteConfigSources();
        return sources.hasMaterial(config);
    }

    //TODO merge from many remote sources

    public PartialConfig getPartialConfig(String materialFingerprint) {
        // just get latest
        throw new NotImplementedException();
    }
    public PartialConfig getPartialConfig(String materialFingerprint, Revision revision) {
        // checkout code at specified revision
        // get proper PartialConfigProvider to scan the repository and create configuration instances
        throw new NotImplementedException();
    }
    // TODO other methods similar to GoConfigService

    @Override
    public void onMessage(MaterialUpdateCompletedMessage message) {
        Material material = message.getMaterial();

        String folder = material.getFolder();
        //TODO if this is listed as configuration material then update lastest configuration
        if(!hasConfiguration(material))
        {
            // not interested in this material
            return;
        }
        // or perhaps do not update and merge each time from all sources but rather
        // 1. parse each as update completes and add parsed config as proposition for next config merge
        // 2. check validity to possible extent in single source.
        // 3. wait for all config materials to finish polling. (Just like pipeline waits for its auto-polled materials)
        // 4. merge all parts with main config.
        // 5. check validity and complain.

        throw new NotImplementedException();
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        // TODO reload list of config materials, drop removed sources
        throw new NotImplementedException();
    }
}

/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ConfigRepoConfigTest {
    @Test
    public void shouldReturnPluginNameWhenSpecified() {
        ConfigRepoConfig config = new ConfigRepoConfig();
        config.setConfigProviderPluginName("myplugin");
        assertThat(config.getConfigProviderPluginName(),is("myplugin"));
    }
    @Test
    public void shouldReturnNullPluginNameWhenEmpty() {
        ConfigRepoConfig config = new ConfigRepoConfig();
        config.setConfigProviderPluginName("");
        assertNull(config.getConfigProviderPluginName());
    }

    @Test
    public void validate_shouldNotAllowDisabledAutoUpdate()
    {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        GoConfigMother mother = new GoConfigMother();

        SvnMaterialConfig svn = new SvnMaterialConfig("url", false);
        svn.setAutoUpdate(false);

        ConfigRepoConfig configRepoConfig = new ConfigRepoConfig(svn,"plug");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));

        configRepoConfig.validate(null);

        assertThat(configRepoConfig.errors().isEmpty(),is(false));
        assertThat(configRepoConfig.errors().on(ConfigRepoConfig.AUTO_UPDATE),
                is("Configuration repository material url must have autoUpdate enabled"));
    }

    @Test
    public void validate_shouldNotAllowPipelineWithSameRepositoryAndDisabledAutoUpdate()
    {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        GoConfigMother mother = new GoConfigMother();

        MaterialConfigs materialConfigs = new MaterialConfigs();
        SvnMaterialConfig svn = new SvnMaterialConfig("url", false);
        svn.setAutoUpdate(false);
        materialConfigs.add(svn);

        ConfigRepoConfig configRepoConfig = new ConfigRepoConfig(svn,"plug");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));

        PipelineConfig pipeline1 = mother.addPipeline(cruiseConfig, "badpipe", "build", materialConfigs, "build");

        configRepoConfig.validate(ValidationContext.forChain(cruiseConfig, new BasicPipelineConfigs(), pipeline1));

        assertThat(svn.errors().isEmpty(),is(false));
        assertThat(svn.errors().on(ScmMaterialConfig.AUTO_UPDATE),
                is("Material of type Subversion (url) is specified as a configuration repository and pipeline material with disabled autoUpdate. All copies of the a material must have autoUpdate enabled or configuration repository must be removed"));
    }
}

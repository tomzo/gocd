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

package com.thoughtworks.go.server.service;

import java.util.Map;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.presentation.CanDeleteResult;
import org.junit.Test;

import static com.thoughtworks.go.helper.EnvironmentConfigMother.environment;
import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipelineConfigServiceTest {

    @Test
    public void shouldBeAbleToGetTheCanDeleteStatusOfAllPipelines() {
        PipelineConfigs configs = createGroup("group", "pipeline", "in_env");
        downstream(configs);
        CruiseConfig cruiseConfig = new BasicCruiseConfig(configs);
        cruiseConfig.addEnvironment(environment("foo", "in_env"));
        PipelineConfig remotePipeline = PipelineConfigMother.pipelineConfig("remote");
        remotePipeline.setOrigin(new RepoConfigOrigin(new ConfigRepoConfig(new GitMaterialConfig("url"),"plugin"),"1234"));
        cruiseConfig.addPipeline("group",remotePipeline);

        GoConfigService service = mock(GoConfigService.class);
        when(service.getCurrentConfig()).thenReturn(cruiseConfig);

        PipelineConfigService pipelineConfigService = new PipelineConfigService(service);

        Map<CaseInsensitiveString, CanDeleteResult> pipelineToCanDeleteIt = pipelineConfigService.canDeletePipelines();

        assertThat(pipelineToCanDeleteIt.size(), is(4));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("down")), is(new CanDeleteResult(true, LocalizedMessage.string("CAN_DELETE_PIPELINE"))));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("in_env")), is(new CanDeleteResult(false, LocalizedMessage.string("CANNOT_DELETE_PIPELINE_IN_ENVIRONMENT", new CaseInsensitiveString("in_env"), new CaseInsensitiveString("foo")))));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("pipeline")), is(new CanDeleteResult(false, LocalizedMessage.string("CANNOT_DELETE_PIPELINE_USED_AS_MATERIALS", new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("down")))));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("remote")), is(new CanDeleteResult(false, LocalizedMessage.string("CANNOT_DELETE_REMOTE_PIPELINE", new CaseInsensitiveString("remote"), "url at 1234"))));
    }

    private void downstream(PipelineConfigs configs) {
        PipelineConfig down = PipelineConfigMother.pipelineConfig("down");
        down.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("mingle")));
        configs.add(down);
    }
}

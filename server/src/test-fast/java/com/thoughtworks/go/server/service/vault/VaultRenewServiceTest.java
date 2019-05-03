/*
 * Copyright 2019 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.service.vault;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.EnvironmentVariableConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.service.AdminsConfigService;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class VaultRenewServiceTest {
    @Mock
    private GoConfigService goConfigService;

    SystemEnvironment systemEnvironment;
    GoCipher cipher;

    private VaultRenewService vaultRenewService;
    private List<PipelineConfig> pipelines;

    String token = "";

    @Before
    public void setUp() throws Exception {
        pipelines = new ArrayList<>();
        initMocks(this);

        systemEnvironment = mock(SystemEnvironment.class);
        cipher = mock(GoCipher.class);
        when(cipher.encrypt(token)).thenReturn(token);
        when(cipher.decrypt(token)).thenReturn(token);
        when(systemEnvironment.getVaultSslCert()).thenReturn("/usr/local/share/ca-certificates/ait.crt");
        when(systemEnvironment.getVaultAddress()).thenReturn("https://vault.ai-traders.com:8200");
        when(goConfigService.getAllPipelineConfigs()).thenReturn(pipelines);
        vaultRenewService = new VaultRenewService(goConfigService, systemEnvironment, cipher);
    }

    @Test
    public void shouldRenewMatchingToken() {
        PipelineConfig pipeline = GoConfigMother.createPipelineConfigWithMaterialConfig("test", new GitMaterialConfig("git-url"));
        pipeline.addEnvironmentVariable(new EnvironmentVariableConfig(cipher, "VAULT_TOKEN", token, true));
        pipelines.add(pipeline);
        vaultRenewService.renewVaultTokens();
    }
}

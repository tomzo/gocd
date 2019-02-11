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

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LookupResponse;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class VaultRenewService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VaultRenewService.class);

    private GoConfigService goConfigService;
    private SystemEnvironment systemEnvironment;
    private GoCipher cipher;

    public VaultRenewService(GoConfigService goConfigService, SystemEnvironment systemEnvironment, GoCipher cipher) {
        this.goConfigService = goConfigService;
        this.systemEnvironment = systemEnvironment;
        this.cipher = cipher;
    }

    @Autowired
    public VaultRenewService(GoConfigService goConfigService, SystemEnvironment systemEnvironment) {
        this.goConfigService = goConfigService;
        this.systemEnvironment = systemEnvironment;
        this.cipher = new GoCipher();
    }

    public VaultConfig createClientConfig(VaultServiceConfig vaultServiceConfig) throws VaultException {
        final SslConfig ssl = new SslConfig()
                .pemFile(new File(vaultServiceConfig.getSslCertFile()));
        final VaultConfig config =
                new VaultConfig()
                        .address(vaultServiceConfig.getVaultAddress())
                        .token(vaultServiceConfig.getServerToken())
                        .sslConfig(ssl.build())
                        .build();
        return config;
    }

    //NOTE: This is called on a thread by Spring
    public void onTimer() {
        renewVaultTokens();
    }

    public void renewVaultTokens() {
        String sslCertFile = systemEnvironment.getVaultSslCert();
        String address = systemEnvironment.getVaultAddress();
        for(String token : getVaultTokens()) {
            try {
                VaultServiceConfig config = new VaultServiceConfig(address, token, sslCertFile);
                VaultConfig vaultConfig = createClientConfig(config);
                Vault vault = new Vault(vaultConfig);
                PatchedAuthApi authApi = new PatchedAuthApi(vaultConfig);
                PatchedLookupResponse lookupResult = authApi.lookupSelf("token");
                if(lookupResult.isRenewable() &&
                        lookupResult.getMetadata() != null &&
                        lookupResult.getMetadata().get("gocd_renew") != null &&
                        "true".equalsIgnoreCase(lookupResult.getMetadata().get("gocd_renew").asString())) {
                    vault.auth().renewSelf();
                    LOGGER.info("Vault token renewed, accessor: {}", lookupResult.getAccessor());
                }
            }
            catch (Exception ex) {
                LOGGER.error("Failed to renew vault token", ex);
            }
        }
    }

    private List<String> getVaultTokens() {
        ArrayList<String> tokens = new ArrayList<>();
        for(PipelineConfig pipelineConfig : goConfigService.getAllPipelineConfigs()) {
            collectTokens(tokens, pipelineConfig.getSecureVariables());
            for(StageConfig stageConfig : pipelineConfig.getStages()) {
                collectTokens(tokens, stageConfig.getSecureVariables());
                for(JobConfig jobConfig : stageConfig.getJobs()) {
                    collectTokens(tokens, jobConfig.getSecureVariables());
                }
            }
        }
        return tokens;
    }

    private void collectTokens(ArrayList<String> tokens, EnvironmentVariablesConfig variables) {
        if(variables == null)
            return;
        for(EnvironmentVariableConfig env : variables) {
            if(env.getName().equals("VAULT_TOKEN")) {
                String tokenPlain = null;
                try {
                    tokenPlain = this.cipher.decrypt(env.getEncryptedValue());
                    tokens.add(tokenPlain);
                } catch (CryptoException e) {
                    LOGGER.error("Failed to decrypt VAULT_TOKEN", e);
                }
            }
        }
    }
}

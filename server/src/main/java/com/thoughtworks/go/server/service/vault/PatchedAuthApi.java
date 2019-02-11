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

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LookupResponse;
import com.bettercloud.vault.rest.Rest;
import com.bettercloud.vault.rest.RestResponse;

// original https://github.com/BetterCloud/vault-java-driver/blob/master/src/main/java/com/bettercloud/vault/api/Auth.java
public class PatchedAuthApi {

    private final VaultConfig config;

    public PatchedAuthApi(final VaultConfig config) {
        this.config = config;
    }

    public PatchedLookupResponse lookupSelf(final String tokenAuthMount) throws VaultException {
        int retryCount = 0;
        final String mount = tokenAuthMount != null ? tokenAuthMount : "token";
        while (true) {
            try {
                // HTTP request to Vault
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/auth/" + mount + "/lookup-self")
                        .header("X-Vault-Token", config.getToken())
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslVerification(config.getSslConfig().isVerify())
                        .sslContext(config.getSslConfig().getSslContext())
                        .get();

                // Validate restResponse
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }
                final String mimeType = restResponse.getMimeType();
                if (mimeType == null || !"application/json".equals(mimeType)) {
                    throw new VaultException("Vault responded with MIME type: " + mimeType, restResponse.getStatus());
                }
                return new PatchedLookupResponse(restResponse, retryCount);
            } catch (Exception e) {
                // If there are retries to perform, then pause for the configured interval and then execute the loop again...
                if (retryCount < config.getMaxRetries()) {
                    retryCount++;
                    try {
                        final int retryIntervalMilliseconds = config.getRetryIntervalMilliseconds();
                        Thread.sleep(retryIntervalMilliseconds);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                } else if (e instanceof VaultException) {
                    // ... otherwise, give up.
                    throw (VaultException) e;
                } else {
                    throw new VaultException(e);
                }
            }
        }
    }
}

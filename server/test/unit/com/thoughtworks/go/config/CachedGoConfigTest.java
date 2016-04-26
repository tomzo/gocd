/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.server.util.ServerVersion;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.*;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.thoughtworks.go.helper.ConfigFileFixture.CONFIG_WITH_1CONFIGREPO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class CachedGoConfigTest {
    private GoConfigPluginService configPluginService;
    private GoConfigWatchList configWatchList;
    private PartialConfigProvider plugin;
    private GoRepoConfigDataSource repoConfigDataSource;
    private GoPartialConfig partials;
    private File folder = new File("workdir");

    @Before
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper(CONFIG_WITH_1CONFIGREPO);
        SystemEnvironment env = new SystemEnvironment();
        ConfigRepository configRepository = new ConfigRepository(env);
        configRepository.initialize();
        dataSource = new GoFileConfigDataSource(new DoNotUpgrade(), configRepository, env, new TimeProvider(),
                new ConfigCache(), new ServerVersion(), ConfigElementImplementationRegistryMother.withNoPlugins(),
                serverHealthService, mock(CachedGoPartials.class));
        serverHealthService = new ServerHealthService();
        cachedGoConfig.loadConfigIfNull();

        configPluginService = mock(GoConfigPluginService.class);
        plugin = mock(PartialConfigProvider.class);
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class))).thenReturn(plugin);

        configWatchList = new GoConfigWatchList(cachedGoConfig);

        repoConfigDataSource = new GoRepoConfigDataSource(configWatchList, configPluginService);

        partials = new GoPartialConfig(repoConfigDataSource, configWatchList, mock(GoConfigService.class), mock(CachedGoPartials.class), mock(ServerHealthService.class));

        cachedGoConfig = new CachedGoConfig(serverHealthService, dataSource);

        configHelper.usingCruiseConfigDao(new GoConfigDao(cachedGoConfig));
    }

    @Test
    public void shouldNotifyListenersWhenFileChanged() {
        ConfigChangedListener listener = mock(ConfigChangedListener.class);
        cachedGoConfig.registerListener(listener);
        verify(listener, times(1)).onConfigChange(any(CruiseConfig.class));

        String content = "<cruise schemaVersion='" + GoConstants.CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' />"
                + "<pipelines>\n"
                + "</pipelines>\n"
                + "</cruise>";

        configHelper.writeXmlToConfigFile(content);

        // once during registerListener call, second when reloaded
        verify(listener, times(2)).onConfigChange(any(CruiseConfig.class));
    }

    public void shouldReturnMergedConfig_WhenThereIsValidPartialConfig() throws Exception {
        assertThat(configWatchList.getCurrentConfigRepos().size(), is(1));
        ConfigRepoConfig configRepo = configWatchList.getCurrentConfigRepos().get(0);
        PartialConfig part1 = new PartialConfig(new PipelineGroups(
                PipelineConfigMother.createGroup("part1", PipelineConfigMother.pipelineConfig("pipe1"))));
        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class))).thenReturn(
                part1
        );
        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), folder, "321e");
        assertThat(repoConfigDataSource.latestPartialConfigForMaterial(configRepo.getMaterialConfig()), is(part1));
        assertThat(cachedGoConfig.currentConfig().hasPipelineNamed(new CaseInsensitiveString("pipe1")), is(true));
    }

    @Test
    public void shouldNotifyWithMergedConfig_WhenPartUpdated() throws Exception {
        ConfigRepoConfig configRepo = configWatchList.getCurrentConfigRepos().get(0);
        PartialConfig part1 = new PartialConfig(new PipelineGroups(
                PipelineConfigMother.createGroup("part1", PipelineConfigMother.pipelineConfig("pipe1"))));
        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class))).thenReturn(part1);

        ConfigChangedListener listener = mock(ConfigChangedListener.class);
        cachedGoConfig.registerListener(listener);
        // at registration
        verify(listener, times(1)).onConfigChange(any(CruiseConfig.class));

        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), folder, "321e");

        assertThat("currentConfigShouldBeMerged",
                cachedGoConfig.currentConfig().hasPipelineNamed(new CaseInsensitiveString("pipe1")), is(true));
        verify(listener, times(2)).onConfigChange(any(CruiseConfig.class));
    }

    @Test
    public void shouldNotNotifyListenersWhenMergeFails() {
        ConfigRepoConfig configRepo = configWatchList.getCurrentConfigRepos().get(0);
        PartialConfig badPart = new PartialConfig(new PipelineGroups(
                PipelineConfigMother.createGroup("part1",
                        PipelineConfigMother.pipelineConfig("pipe1"), PipelineConfigMother.pipelineConfig("pipe1"))));
        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class))).thenReturn(badPart);

        ConfigChangedListener listener = mock(ConfigChangedListener.class);
        cachedGoConfig.registerListener(listener);
        // at registration
        verify(listener, times(1)).onConfigChange(any(CruiseConfig.class));

        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), folder, "321e");

        assertThat("currentConfigShouldBeMainXmlOnly",
                cachedGoConfig.currentConfig(), is(cachedGoConfig.currentConfig()));

        verify(listener, times(1)).onConfigChange(any(CruiseConfig.class));
    }

    @Test
    public void shouldSetErrorHealthStateWhenMergeFails() {
        ConfigRepoConfig configRepo = configWatchList.getCurrentConfigRepos().get(0);
        PartialConfig badPart = new PartialConfig(new PipelineGroups(
                PipelineConfigMother.createGroup("part1",
                        PipelineConfigMother.pipelineConfig("pipe1"), PipelineConfigMother.pipelineConfig("pipe1"))));
        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class))).thenReturn(badPart);

        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), folder, "321e");

        assertThat(serverHealthService.filterByScope(HealthStateScope.GLOBAL).isEmpty(), is(false));
        assertThat(serverHealthService.getLogsAsText().contains("Invalid Merged Config"), is(true));
        assertThat(serverHealthService.getLogsAsText().contains("pipe1"), is(true));
    }

    @Test
    public void shouldUnSetErrorHealthStateWhenMergePasses() {
        ConfigRepoConfig configRepo = configWatchList.getCurrentConfigRepos().get(0);
        PartialConfig badPart = new PartialConfig(new PipelineGroups(
                PipelineConfigMother.createGroup("part1",
                        PipelineConfigMother.pipelineConfig("pipe1"), PipelineConfigMother.pipelineConfig("pipe1"))));
        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class))).thenReturn(badPart);

        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), folder, "321e");

        assertThat(serverHealthService.getLogsAsText().contains("Invalid Merged Config"), is(true));

        //fix partial
        PartialConfig part1 = new PartialConfig(new PipelineGroups(
                PipelineConfigMother.createGroup("part1", PipelineConfigMother.pipelineConfig("pipe1"))));
        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class))).thenReturn(part1);

        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), folder, "321e");

        assertThat(serverHealthService.filterByScope(HealthStateScope.GLOBAL).isEmpty(), is(true));
    }

    @Test
    public void shouldDelegateWritePipelineConfigCallToFileService() {
        PipelineConfigService.SaveCommand saveCommand = mock(PipelineConfigService.SaveCommand.class);
        GoFileConfigDataSource dataSource = mock(GoFileConfigDataSource.class);
        CachedGoConfig cachedGoConfig = new CachedGoConfig(mock(ServerHealthService.class), dataSource);
        PipelineConfig pipelineConfig = new PipelineConfig();
        CachedGoConfig.PipelineConfigSaveResult saveResult = mock(CachedGoConfig.PipelineConfigSaveResult.class);
        GoConfigHolder savedConfig = new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig());
        when(saveResult.getConfigHolder()).thenReturn(savedConfig);
        GoConfigHolder holderBeforeUpdate = cachedGoConfig.loadConfigHolder();
        Username user = new Username(new CaseInsensitiveString("user"));
        when(dataSource.writePipelineWithLock(pipelineConfig, holderBeforeUpdate, saveCommand, user)).thenReturn(saveResult);

        cachedGoConfig.writePipelineWithLock(pipelineConfig, saveCommand, user);
        assertThat(cachedGoConfig.loadConfigHolder(), is(savedConfig));
        assertThat(cachedGoConfig.currentConfig(), is(savedConfig.config));
        assertThat(cachedGoConfig.loadForEditing(), is(savedConfig.configForEdit));
        verify(dataSource).writePipelineWithLock(pipelineConfig, holderBeforeUpdate, saveCommand, user);
    }

    protected CachedGoConfig cachedGoConfig;
    protected GoConfigFileHelper configHelper;
    protected GoFileConfigDataSource dataSource;
    protected ServerHealthService serverHealthService;

    @Test
    public void shouldUpdateCachedConfigOnSave() throws Exception {
        Assert.assertThat(cachedGoConfig.currentConfig().agents().size(), Matchers.is(1));
        configHelper.addAgent("hostname", "uuid2");
        Assert.assertThat(cachedGoConfig.currentConfig().agents().size(), Matchers.is(2));
    }

    @Test
    public void shouldReloadCachedConfigWhenWriting() throws Exception {
        cachedGoConfig.writeWithLock(updateFirstAgentResources("osx"));
        Assert.assertThat(cachedGoConfig.currentConfig().agents().get(0).getResources().toString(), Matchers.is("osx"));

        cachedGoConfig.writeWithLock(updateFirstAgentResources("osx, firefox"));
        Assert.assertThat(cachedGoConfig.currentConfig().agents().get(0).getResources().toString(), Matchers.is("firefox | osx"));
    }

    @Test
    public void shouldReloadCachedConfigFromDisk() throws Exception {
        Assert.assertThat(cachedGoConfig.currentConfig().agents().size(), Matchers.is(1));
        configHelper.writeXmlToConfigFile(ConfigFileFixture.TASKS_WITH_CONDITION);
        cachedGoConfig.forceReload();
        Assert.assertThat(cachedGoConfig.currentConfig().agents().size(), Matchers.is(0));
    }

    @Test
    public void shouldInterpolateParamsInTemplate() throws Exception {
        String content = "<cruise schemaVersion='" + GoConstants.CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' >"
                + "</server>"
                + "<pipelines>\n"
                + "<pipeline name='dev' template='abc'>\n"
                + "    <params>"
                + "        <param name='command'>ls</param>"
                + "        <param name='dir'>/tmp</param>"
                + "    </params>"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "</pipeline>\n"
                + "<pipeline name='acceptance' template='abc'>\n"
                + "    <params>"
                + "        <param name='command'>twist</param>"
                + "        <param name='dir'>./acceptance</param>"
                + "    </params>"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "<templates>\n"
                + "  <pipeline name='abc'>\n"
                + "    <stage name='stage1'>"
                + "      <jobs>"
                + "        <job name='job1'>"
                + "            <tasks>"
                + "                <exec command='/bin/#{command}' args='#{dir}'/>"
                + "            </tasks>"
                + "        </job>"
                + "      </jobs>"
                + "    </stage>"
                + "  </pipeline>\n"
                + "</templates>\n"
                + "</cruise>";

        configHelper.writeXmlToConfigFile(content);

        cachedGoConfig.forceReload();

        CruiseConfig cruiseConfig = cachedGoConfig.currentConfig();
        ExecTask devExec = (ExecTask) cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("dev")).getFirstStageConfig().jobConfigByConfigName(new CaseInsensitiveString("job1")).getTasks().first();
        Assert.assertThat(devExec, Is.is(new ExecTask("/bin/ls", "/tmp", (String) null)));

        ExecTask acceptanceExec = (ExecTask) cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("acceptance")).getFirstStageConfig().jobConfigByConfigName(new CaseInsensitiveString("job1")).getTasks().first();
        Assert.assertThat(acceptanceExec, Is.is(new ExecTask("/bin/twist", "./acceptance", (String) null)));

        cruiseConfig = cachedGoConfig.loadForEditing();
        devExec = (ExecTask) cruiseConfig.getTemplateByName(new CaseInsensitiveString("abc")).get(0).jobConfigByConfigName(new CaseInsensitiveString("job1")).getTasks().first();
        Assert.assertThat(devExec, Is.is(new ExecTask("/bin/#{command}", "#{dir}", (String) null)));

        Assert.assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("dev")).size(), Matchers.is(0));
        Assert.assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("acceptance")).size(), Matchers.is(0));
    }

    @Test
    public void shouldHandleParamQuotingCorrectly() throws Exception {
        String content = "<cruise schemaVersion='" + GoConstants.CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' />"
                + "<pipelines>\n"
                + "<pipeline name='dev'>\n"
                + "    <params>"
                + "        <param name='command'>ls#{a}</param>"
                + "        <param name='dir'>/tmp</param>"
                + "    </params>"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "    <stage name='stage1'>"
                + "      <jobs>"
                + "        <job name='job1'>"
                + "            <tasks>"
                + "                <exec command='/bin/#{command}##{b}' args='#{dir}'/>"
                + "            </tasks>"
                + "        </job>"
                + "      </jobs>"
                + "    </stage>"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "</cruise>";

        configHelper.writeXmlToConfigFile(content);

        cachedGoConfig.forceReload();

        CruiseConfig cruiseConfig = cachedGoConfig.currentConfig();
        ExecTask devExec = (ExecTask) cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("dev")).getFirstStageConfig().jobConfigByConfigName(new CaseInsensitiveString("job1")).getTasks().first();
        Assert.assertThat(devExec, Is.is(new ExecTask("/bin/ls#{a}#{b}", "/tmp", (String) null)));
    }

    @Test
    public void shouldAllowParamsInLabelTemplates() throws Exception {
        String content = "<cruise schemaVersion='" + GoConstants.CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' />"
                + "<pipelines>\n"
                + "<pipeline name='dev' labeltemplate='cruise-#{VERSION}-${COUNT}'>\n"
                + "    <params>"
                + "        <param name='VERSION'>1.2</param>"
                + "    </params>"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "    <stage name='stage1'>"
                + "      <jobs>"
                + "        <job name='job1'>"
                + "            <tasks>"
                + "                <exec command='/bin/ls' args='some'/>"
                + "            </tasks>"
                + "        </job>"
                + "      </jobs>"
                + "    </stage>"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "</cruise>";

        configHelper.writeXmlToConfigFile(content);

        cachedGoConfig.forceReload();

        CruiseConfig cruiseConfig = cachedGoConfig.currentConfig();
        Assert.assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("dev")).getLabelTemplate(), Is.is("cruise-1.2-${COUNT}"));
    }

    @Test
    public void shouldThrowErrorWhenEnvironmentVariablesAreDuplicate() throws Exception {
        String content = "<cruise schemaVersion='" + GoConstants.CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' />"
                + "<pipelines>\n"
                + "<pipeline name='dev'>\n"
                + "    <params>"
                + "        <param name='product'>GO</param>"
                + "    </params>"
                + "    <environmentvariables>"
                + "        <variable name='#{product}_WORKING_DIR'><value>go_dir</value></variable>"
                + "        <variable name='GO_WORKING_DIR'><value>dir</value></variable>"
                + "    </environmentvariables>"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "    <stage name='stage1'>"
                + "      <jobs>"
                + "        <job name='job1'>"
                + "            <tasks>"
                + "                <exec command='/bin/ls' args='some'/>"
                + "            </tasks>"
                + "        </job>"
                + "      </jobs>"
                + "    </stage>"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "</cruise>";

        configHelper.writeXmlToConfigFile(content);

        GoConfigValidity configValidity = cachedGoConfig.checkConfigFileValid();
        Assert.assertThat(configValidity.isValid(), Matchers.is(false));
        Assert.assertThat(configValidity.errorMessage(), containsString("Environment Variable name 'GO_WORKING_DIR' is not unique for pipeline 'dev'"));
    }

    @Test
    public void shouldReturnCachedConfigIfConfigFileIsInvalid() throws Exception {
        CruiseConfig inTheBefore = cachedGoConfig.currentConfig();
        Assert.assertThat(inTheBefore.agents().size(), Matchers.is(1));

        configHelper.writeXmlToConfigFile("invalid-xml");
        cachedGoConfig.forceReload();

        assertTrue(cachedGoConfig.currentConfig() == inTheBefore);
        Assert.assertThat(cachedGoConfig.checkConfigFileValid().isValid(), Matchers.is(false));
    }

    @Test
    public void shouldClearInvalidExceptionWhenConfigErrorsAreFixed() throws Exception {
        configHelper.writeXmlToConfigFile("invalid-xml");
        cachedGoConfig.forceReload();

        cachedGoConfig.currentConfig();
        Assert.assertThat(cachedGoConfig.checkConfigFileValid().isValid(), Matchers.is(false));

        configHelper.onSetUp();

        CruiseConfig cruiseConfig = cachedGoConfig.currentConfig();

        Assert.assertThat(cruiseConfig.agents().size(), Matchers.is(1));
        Assert.assertThat(cachedGoConfig.checkConfigFileValid().isValid(), Matchers.is(true));
    }

    @Test
    public void shouldSetServerHealthMessageWhenConfigFileIsInvalid() throws IOException {
        configHelper.writeXmlToConfigFile("invalid-xml");
        cachedGoConfig.forceReload();

        Assert.assertThat(cachedGoConfig.checkConfigFileValid().isValid(), Matchers.is(false));

        List<ServerHealthState> serverHealthStates = serverHealthService.getAllLogs();
        Assert.assertThat(serverHealthStates.size(), Matchers.is(1));
        Assert.assertThat(serverHealthStates.get(0), Matchers.is(ServerHealthState.error(GoConfigService.INVALID_CRUISE_CONFIG_XML, "Error on line 1: Content is not allowed in prolog.", HealthStateType.invalidConfig())));
    }

    @Test
    public void shouldClearServerHealthMessageWhenConfigFileIsValid() throws IOException {
        ServerHealthState error = ServerHealthState.error(GoConfigService.INVALID_CRUISE_CONFIG_XML, "Error on line 1: Content is not allowed in prolog.", HealthStateType.invalidConfig());
        serverHealthService.update(error);

        Assert.assertThat(serverHealthService.getAllLogs().size(), Matchers.is(1));

        configHelper.writeXmlToConfigFile(ConfigFileFixture.TASKS_WITH_CONDITION);
        cachedGoConfig.forceReload();

        Assert.assertThat(cachedGoConfig.checkConfigFileValid().isValid(), Matchers.is(true));

        Assert.assertThat(serverHealthService.getAllLogs().size(), Matchers.is(0));
    }

    @Test
    public void shouldReturnDefaultCruiseConfigIfLoadingTheConfigFailsForTheFirstTime() throws Exception {
        configHelper.writeXmlToConfigFile("invalid-xml");
        cachedGoConfig = new CachedGoConfig(new ServerHealthService(), dataSource);
        Assert.assertThat(cachedGoConfig.currentConfig(), Matchers.<CruiseConfig>is(new BasicCruiseConfig()));
    }

    @Test
    public void shouldLoadConfigHolderIfNotAvailable() throws Exception {
        configHelper.addPipeline("foo", "bar");
        cachedGoConfig = new CachedGoConfig(new ServerHealthService(), dataSource);
        dataSource.reloadIfModified();
        cachedGoConfig.forceReload();
        GoConfigHolder loaded = cachedGoConfig.loadConfigHolder();
        Assert.assertThat(loaded.config.hasPipelineNamed(new CaseInsensitiveString("foo")), Matchers.is(true));
        Assert.assertThat(loaded.configForEdit.hasPipelineNamed(new CaseInsensitiveString("foo")), Matchers.is(true));
    }

    @Test
    public void shouldGetConfigForEditAndRead() throws Exception {
        CruiseConfig cruiseConfig = configHelper.load();
        addPipelineWithParams(cruiseConfig);
        configHelper.writeConfigFile(cruiseConfig);
        cachedGoConfig = new CachedGoConfig(new ServerHealthService(), dataSource);
        dataSource.reloadIfModified();

        cachedGoConfig.forceReload();

        PipelineConfig config = cachedGoConfig.currentConfig().pipelineConfigByName(new CaseInsensitiveString("mingle"));
        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) byFolder(config.materialConfigs(), "folder");
        Assert.assertThat(hgMaterialConfig.getUrl(), Matchers.is("http://hg-server/repo-name"));

        config = cachedGoConfig.loadForEditing().pipelineConfigByName(new CaseInsensitiveString("mingle"));
        hgMaterialConfig = (HgMaterialConfig) byFolder(config.materialConfigs(), "folder");
        Assert.assertThat(hgMaterialConfig.getUrl(), Matchers.is("http://#{foo}/#{bar}"));

        cachedGoConfig.loadConfigHolder();
    }

    private void addPipelineWithParams(CruiseConfig cruiseConfig) {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("mingle", "dev", "ant");
        pipelineConfig.addParam(new ParamConfig("foo", "hg-server"));
        pipelineConfig.addParam(new ParamConfig("bar", "repo-name"));
        pipelineConfig.addMaterialConfig(MaterialConfigsMother.hgMaterialConfig("http://#{foo}/#{bar}", "folder"));
        cruiseConfig.addPipeline("another", pipelineConfig);
    }

    @Test
    public void shouldLoadConfigForReadAndEditWhenNewXMLIsWritten() throws Exception {
        cachedGoConfig.forceReload();
        GoConfigValidity configValidity = cachedGoConfig.checkConfigFileValid();
        Assert.assertThat(configValidity.isValid(), Matchers.is(true));

        CruiseConfig cruiseConfig = cachedGoConfig.loadForEditing();

        addPipelineWithParams(cruiseConfig);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        new MagicalGoConfigXmlWriter(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins()).write(cruiseConfig, buffer, false);

        cachedGoConfig.save(new String(buffer.toByteArray()), true);

        PipelineConfig reloadedPipelineConfig = cachedGoConfig.currentConfig().pipelineConfigByName(new CaseInsensitiveString("mingle"));
        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        Assert.assertThat(hgMaterialConfig.getUrl(), Matchers.is("http://hg-server/repo-name"));

        reloadedPipelineConfig = cachedGoConfig.loadForEditing().pipelineConfigByName(new CaseInsensitiveString("mingle"));
        hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        Assert.assertThat(hgMaterialConfig.getUrl(), Matchers.is("http://#{foo}/#{bar}"));

        GoConfigHolder configHolder = cachedGoConfig.loadConfigHolder();
        reloadedPipelineConfig = configHolder.config.pipelineConfigByName(new CaseInsensitiveString("mingle"));
        hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        Assert.assertThat(hgMaterialConfig.getUrl(), Matchers.is("http://hg-server/repo-name"));

        reloadedPipelineConfig = configHolder.configForEdit.pipelineConfigByName(new CaseInsensitiveString("mingle"));
        hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        Assert.assertThat(hgMaterialConfig.getUrl(), Matchers.is("http://#{foo}/#{bar}"));
    }

    @Test
    public void shouldLoadConfigForReadAndEditWhenConfigIsUpdatedThoughACommand() throws Exception {
        cachedGoConfig.forceReload();
        GoConfigValidity configValidity = cachedGoConfig.checkConfigFileValid();
        Assert.assertThat(configValidity.isValid(), Matchers.is(true));

        cachedGoConfig.writeWithLock(new UpdateConfigCommand() {
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                addPipelineWithParams(cruiseConfig);
                return cruiseConfig;
            }
        });
        PipelineConfig reloadedPipelineConfig = cachedGoConfig.currentConfig().pipelineConfigByName(new CaseInsensitiveString("mingle"));
        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        Assert.assertThat(hgMaterialConfig.getUrl(), Matchers.is("http://hg-server/repo-name"));

        reloadedPipelineConfig = cachedGoConfig.loadForEditing().pipelineConfigByName(new CaseInsensitiveString("mingle"));
        hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        Assert.assertThat(hgMaterialConfig.getUrl(), Matchers.is("http://#{foo}/#{bar}"));
    }

    @Test
    public void shouldNotifyConfigListenersWhenConfigChanges() throws Exception {
        final ConfigChangedListener listener = mock(ConfigChangedListener.class);
        cachedGoConfig.forceReload();

        cachedGoConfig.registerListener(listener);
        cachedGoConfig.writeWithLock(updateFirstAgentResources("osx"));

        verify(listener, times(2)).onConfigChange(any(BasicCruiseConfig.class));
    }

    @Test
    public void shouldNotNotifyWhenConfigIsNullDuringRegistration() throws Exception {
        configHelper.deleteConfigFile();
        ServerHealthService serverHealthService = new ServerHealthService();
        cachedGoConfig = new CachedGoConfig(serverHealthService, dataSource);
        final ConfigChangedListener listener = mock(ConfigChangedListener.class);
        cachedGoConfig.registerListener(listener);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void shouldReturnMergedStatusWhenConfigIsMergedWithStaleCopy() {
        GoFileConfigDataSource goFileConfigDataSource = mock(GoFileConfigDataSource.class);
        UpdateConfigCommand updateConfigCommand = mock(UpdateConfigCommand.class);
        CruiseConfig currentConfig = GoConfigMother.configWithPipelines("p1");
        GoFileConfigDataSource.GoConfigSaveResult goConfigSaveResult = new GoFileConfigDataSource.GoConfigSaveResult(new GoConfigHolder(currentConfig, currentConfig), ConfigSaveState.MERGED);
        when(goFileConfigDataSource.writeWithLock(argThat(Matchers.is(updateConfigCommand)), any(GoConfigHolder.class))).thenReturn(goConfigSaveResult);
        cachedGoConfig = new CachedGoConfig(serverHealthService, dataSource);

        ConfigSaveState configSaveState = cachedGoConfig.writeWithLock(updateConfigCommand);
        Assert.assertThat(configSaveState, Matchers.is(ConfigSaveState.MERGED));
    }

    @Test
    public void shouldReturnUpdatedStatusWhenConfigIsUpdatedWithLatestCopy() {
        GoFileConfigDataSource goFileConfigDataSource = mock(GoFileConfigDataSource.class);
        UpdateConfigCommand updateConfigCommand = mock(UpdateConfigCommand.class);
        CruiseConfig currentConfig = GoConfigMother.configWithPipelines("p1");
        GoFileConfigDataSource.GoConfigSaveResult goConfigSaveResult = new GoFileConfigDataSource.GoConfigSaveResult(new GoConfigHolder(currentConfig, currentConfig), ConfigSaveState.UPDATED);
        when(goFileConfigDataSource.writeWithLock(argThat(Matchers.is(updateConfigCommand)), any(GoConfigHolder.class))).thenReturn(goConfigSaveResult);
        cachedGoConfig = new CachedGoConfig(serverHealthService, dataSource);

        ConfigSaveState configSaveState = cachedGoConfig.writeWithLock(updateConfigCommand);
        Assert.assertThat(configSaveState, Matchers.is(ConfigSaveState.UPDATED));
    }

    private UpdateConfigCommand updateFirstAgentResources(final String resources) {
        return new UpdateConfigCommand() {
            public CruiseConfig update(CruiseConfig cruiseConfig) {
                AgentConfig agentConfig = cruiseConfig.agents().get(0);
                agentConfig.setResources(new Resources(resources));
                return cruiseConfig;
            }
        };
    }


    public MaterialConfig byFolder(MaterialConfigs materialConfigs, String folder) {
        for (MaterialConfig materialConfig : materialConfigs) {
            if (materialConfig instanceof ScmMaterialConfig && ObjectUtil.nullSafeEquals(folder, materialConfig.getFolder())) {
                return materialConfig;
            }
        }
        return null;
    }

}

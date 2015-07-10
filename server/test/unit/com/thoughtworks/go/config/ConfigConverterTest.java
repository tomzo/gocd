package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironment;
import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironmentVariable;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPluginConfiguration;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.CRDependencyMaterial;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.CRGitMaterial;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.CRHgMaterial;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.*;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.util.CollectionUtil;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.hamcrest.core.Is;
import org.jruby.ant.Rake;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigConverterTest {

    private ConfigConverter configConverter;
    private GoCipher goCipher;
    private List<String> filter = new ArrayList<>();

    @Before
    public void setUp() throws InvalidCipherTextException {
        goCipher = mock(GoCipher.class);
        configConverter = new ConfigConverter(goCipher);
        String encryptedText = "secret";
        when(goCipher.decrypt("encryptedvalue")).thenReturn(encryptedText);

        filter = new ArrayList<>();
        filter.add("filter");
    }

    @Test
    public void shouldConvertEnvironmentVariableWhenNotSecure()
    {
        CREnvironmentVariable crEnvironmentVariable = new CREnvironmentVariable("key1","value");
        EnvironmentVariableConfig result = configConverter.toEnvironmentVariableConfig(crEnvironmentVariable);
        assertThat(result.getValue(),is("value"));
        assertThat(result.getName(),is("key1"));
        assertThat(result.isSecure(),is(false));
    }

    @Test
    public void shouldConvertEnvironmentVariableWhenSecure()
    {
        CREnvironmentVariable crEnvironmentVariable = new CREnvironmentVariable("key1",null,"encryptedvalue");
        EnvironmentVariableConfig result = configConverter.toEnvironmentVariableConfig(crEnvironmentVariable);
        assertThat(result.isSecure(),is(true));
        assertThat(result.getValue(),is("secret"));
        assertThat(result.getName(),is("key1"));
    }

    @Test
    public void shouldMigrateEnvironment()
    {
        ArrayList<CREnvironmentVariable> environmentVariables = new ArrayList<>();
        environmentVariables.add(new CREnvironmentVariable("key","value"));
        ArrayList<String> agents= new ArrayList<>();
        agents.add("12");
        ArrayList<String> pipelines= new ArrayList<>();
        pipelines.add("pipe1");
        CREnvironment crEnvironment = new CREnvironment("dev", environmentVariables, agents, pipelines);

        BasicEnvironmentConfig environmentConfig = configConverter.toEnvironmentConfig(crEnvironment);
        assertThat(environmentConfig.name().toLower(),is("dev"));
        assertThat(environmentConfig.contains("pipe1"),is(true));
        assertThat(environmentConfig.hasVariable("key"),is(true));
        assertThat(environmentConfig.hasAgent("12"),is(true));
    }

    @Test
    public void shouldMigratePluggableTask()
    {
        ArrayList<CRConfigurationProperty> configs = new ArrayList<>();
        configs.add(new CRConfigurationProperty("k","m",null));
        CRPluggableTask pluggableTask = new CRPluggableTask(CRRunIf.any,null,
                new CRPluginConfiguration("myplugin","1"),configs);
        PluggableTask result = (PluggableTask)configConverter.toAbstractTask(pluggableTask);

        assertThat(result.getPluginConfiguration().getId(),is("myplugin"));
        assertThat(result.getPluginConfiguration().getVersion(),is("1"));
        assertThat(result.getConfiguration().getProperty("k").getValue(),is("m"));
        assertThat(result.getConditions().first(), is(RunIfConfig.ANY));
    }

    @Test
    public void shouldMigrateRakeTask()
    {
        CRBuildTask crBuildTask = new CRBuildTask(CRRunIf.failed,null,"Rakefile.rb","build","src", CRBuildFramework.rake);
        RakeTask result = (RakeTask)configConverter.toAbstractTask(crBuildTask);

        assertRakeTask(result);
    }

    private void assertRakeTask(RakeTask result) {
        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.getBuildFile(),is("Rakefile.rb"));
        assertThat(result.getTarget(),is("build"));
        assertThat(result.workingDirectory(),is("src"));
    }

    @Test
    public void shouldMigrateAntTask()
    {
        CRTask cancel = new CRBuildTask(CRRunIf.failed,null,"Rakefile.rb","build","src", CRBuildFramework.rake);
        CRBuildTask crBuildTask = new CRBuildTask(CRRunIf.failed,cancel,"ant","build","src", CRBuildFramework.ant);
        AntTask result = (AntTask)configConverter.toAbstractTask(crBuildTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.getBuildFile(),is("ant"));
        assertThat(result.getTarget(),is("build"));
        assertThat(result.workingDirectory(),is("src"));

        assertThat(result.cancelTask() instanceof RakeTask,is(true));
        assertRakeTask((RakeTask)result.cancelTask() );
    }

    @Test
    public void shouldMigrateNantTask()
    {
        CRBuildTask crBuildTask = new CRNantTask(CRRunIf.passed,null,"nant","build","src", "path");
        NantTask result = (NantTask)configConverter.toAbstractTask(crBuildTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.PASSED));
        assertThat(result.getBuildFile(),is("nant"));
        assertThat(result.getTarget(),is("build"));
        assertThat(result.workingDirectory(),is("src"));
    }

    @Test
    public void shouldConvertExecTask()
    {
        CRExecTask crExecTask = new CRExecTask(CRRunIf.failed,null,"bash","work",120L, Arrays.asList("1","2"));
        ExecTask result = (ExecTask)configConverter.toAbstractTask(crExecTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.command(),is("bash"));
        assertThat(result.getArgList(),hasItem(new Argument("1")));
        assertThat(result.getArgList(),hasItem(new Argument("2")));
        assertThat(result.workingDirectory(),is("work"));
        assertThat(result.getTimeout(),is(120L));
    }
    @Test
    public void shouldConvertFetchArtifactTaskWhenSourceIsDirectory()
    {
        CRFetchArtifactTask crFetchArtifactTask = new CRFetchArtifactTask(CRRunIf.failed,null,
                "upstream","stage","job","src","dest",true);

        FetchTask result = (FetchTask)configConverter.toAbstractTask(crFetchArtifactTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.getDest(),is("dest"));
        assertThat(result.getJob().toLower(),is("job"));
        assertThat(result.getPipelineName().toLower(),is("upstream"));
        assertThat(result.getSrc(),is("src"));
        assertNull(result.getSrcfile());
        assertThat(result.isSourceAFile(), is(false));
    }
    @Test
    public void shouldConvertFetchArtifactTaskWhenSourceIsFile()
    {
        CRFetchArtifactTask crFetchArtifactTask = new CRFetchArtifactTask(CRRunIf.failed,null,
                "upstream","stage","job","src","dest",false);

        FetchTask result = (FetchTask)configConverter.toAbstractTask(crFetchArtifactTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.getDest(),is("dest"));
        assertThat(result.getJob().toLower(),is("job"));
        assertThat(result.getPipelineName().toLower(),is("upstream"));
        assertThat(result.getSrc(),is("src"));
        assertNull(result.getSrcdir());
        assertThat(result.isSourceAFile(),is(true));
    }

    @Test
    public void shouldConvertDependencyMaterial()
    {
        CRDependencyMaterial crDependencyMaterial = new CRDependencyMaterial("name","pipe","stage");
        DependencyMaterialConfig dependencyMaterialConfig =
                (DependencyMaterialConfig)configConverter.toMaterialConfig(crDependencyMaterial);

        assertThat(dependencyMaterialConfig.getName().toLower(),is("name"));
        assertThat(dependencyMaterialConfig.getPipelineName().toLower(),is("pipe"));
        assertThat(dependencyMaterialConfig.getStageName().toLower(),is("stage"));
    }

    @Test
    public void shouldConvertGitMaterial()
    {
        CRGitMaterial crGitMaterial = new CRGitMaterial("name","folder",true,filter,"url","branch");

        GitMaterialConfig gitMaterialConfig =
                (GitMaterialConfig)configConverter.toMaterialConfig(crGitMaterial);

        assertThat(gitMaterialConfig.getName().toLower(),is("name"));
        assertThat(gitMaterialConfig.getFolder(),is("folder"));
        assertThat(gitMaterialConfig.getAutoUpdate(),is(true));
        assertThat(gitMaterialConfig.getFilterAsString(),is("filter"));
        assertThat(gitMaterialConfig.getUrl(),is("url"));
        assertThat(gitMaterialConfig.getBranch(),is("branch"));
    }

    @Test
    public void shouldConvertHgMaterial()
    {
        CRHgMaterial crHgMaterial = new CRHgMaterial("name","folder",true,filter,"url");

        HgMaterialConfig hgMaterialConfig =
                (HgMaterialConfig)configConverter.toMaterialConfig(crHgMaterial);

        assertThat(hgMaterialConfig.getName().toLower(),is("name"));
        assertThat(hgMaterialConfig.getFolder(),is("folder"));
        assertThat(hgMaterialConfig.getAutoUpdate(),is(true));
        assertThat(hgMaterialConfig.getFilterAsString(),is("filter"));
        assertThat(hgMaterialConfig.getUrl(),is("url"));
    }

}

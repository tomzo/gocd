package com.thoughtworks.go.config;

import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironment;
import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironmentVariable;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPluginConfiguration;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.CRBuildFramework;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.CRBuildTask;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.CRPluggableTask;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.CRRunIf;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.util.CollectionUtil;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.hamcrest.core.Is;
import org.jruby.ant.Rake;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigConverterTest {

    private ConfigConverter configConverter;
    private GoCipher goCipher;

    @Before
    public void setUp() throws InvalidCipherTextException {
        goCipher = mock(GoCipher.class);
        configConverter = new ConfigConverter(goCipher);
        String encryptedText = "secret";
        when(goCipher.decrypt("encryptedvalue")).thenReturn(encryptedText);
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

        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.getBuildFile(),is("Rakefile.rb"));
        assertThat(result.getTarget(),is("build"));
        assertThat(result.workingDirectory(),is("src"));
    }

}

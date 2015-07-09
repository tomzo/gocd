package com.thoughtworks.go.config;

import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.plugin.access.configrepo.contract.*;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.CRPluggableTask;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.CRRunIf;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.CRTask;
import com.thoughtworks.go.security.GoCipher;

import java.util.Collection;

/**
 * Helper to transform config repo classes to config-api classes
 */
public class ConfigConverter {

    private final GoCipher cipher;

    public ConfigConverter(GoCipher goCipher)
    {
        this.cipher = goCipher;
    }

    public PartialConfig toPartialConfig(CRPartialConfig crPartialConfig) {
        PartialConfig partialConfig = new PartialConfig();
        for(CREnvironment crEnvironment : crPartialConfig.getEnvironments())
        {
            EnvironmentConfig environment = toEnvironmentConfig(crEnvironment);
            partialConfig.getEnvironments().add(environment);
        }
        //TODO set other elements
        return partialConfig;
    }

    public BasicEnvironmentConfig toEnvironmentConfig(CREnvironment crEnvironment) {
        BasicEnvironmentConfig basicEnvironmentConfig =
                new BasicEnvironmentConfig(new CaseInsensitiveString(crEnvironment.getName()));
        for(String pipeline : crEnvironment.getPipelines())
        {
            basicEnvironmentConfig.addPipeline(new CaseInsensitiveString(pipeline));
        }
        for(String agent : crEnvironment.getAgents())
        {
            basicEnvironmentConfig.addAgent(agent);
        }
        for(CREnvironmentVariable var : crEnvironment.getEnvironmentVariables())
        {
            basicEnvironmentConfig.getVariables().add(toEnvironmentVariableConfig(var));
        }

        return basicEnvironmentConfig;
    }

    public EnvironmentVariableConfig toEnvironmentVariableConfig(CREnvironmentVariable crEnvironmentVariable) {
        if(crEnvironmentVariable.hasEncryptedValue())
        {
            return new EnvironmentVariableConfig(cipher,crEnvironmentVariable.getName(),crEnvironmentVariable.getEncryptedValue());
        }
        else
        {
            return new EnvironmentVariableConfig(crEnvironmentVariable.getName(),crEnvironmentVariable.getValue());
        }
    }

    public PluggableTask toPluggableTask(CRPluggableTask pluggableTask) {
        PluginConfiguration pluginConfiguration = toPluginConfiguration(pluggableTask.getPluginConfiguration());
        Configuration configuration = toConfiguration(pluggableTask.getConfiguration());
        PluggableTask task = new PluggableTask(pluginConfiguration, configuration);
        setCommonTaskMembers(task,pluggableTask);
        return task;
    }

    private void setCommonTaskMembers(AbstractTask task, CRTask crTask) {
        CRTask crTaskOnCancel = crTask.getOnCancel();
        task.setCancelTask(crTaskOnCancel != null ? toAbstractTask(crTaskOnCancel) : null);
        task.runIfConfigs = toRunIfConfigs(crTask.getRunIf());
    }

    private RunIfConfigs toRunIfConfigs(CRRunIf runIf) {
        switch (runIf)
        {
            case any:
                return new RunIfConfigs(RunIfConfig.ANY);
            case passed:
                return new RunIfConfigs(RunIfConfig.PASSED);
            case failed:
                return new RunIfConfigs(RunIfConfig.FAILED);
            default:
                throw new RuntimeException(
                        String.format("unknown run if condition '%s'",runIf));
        }
    }

    public AbstractTask toAbstractTask(CRTask crTask) {
        if(crTask == null)
            throw new ConfigConvertionException("task cannot be null");

        if(crTask instanceof CRPluggableTask)
            return toPluggableTask((CRPluggableTask)crTask);
        else
            throw new RuntimeException(
                    String.format("unknown type of task '%s'",crTask));
    }

    private Configuration toConfiguration(Collection<CRConfigurationProperty> properties) {
        Configuration configuration = new Configuration();
        for(CRConfigurationProperty p : properties)
        {
            if(p.getValue() != null)
                configuration.addNewConfigurationWithValue(p.getKey(),p.getValue(),false);
            else
                configuration.addNewConfigurationWithValue(p.getKey(),p.getEncryptedValue(),true);
        }
        return configuration;
    }

    public PluginConfiguration toPluginConfiguration(CRPluginConfiguration pluginConfiguration) {
        return new PluginConfiguration(pluginConfiguration.getId(),pluginConfiguration.getVersion());
    }

    //TODO #1133 convert each config element
}

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.config.Arguments;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.plugin.access.configrepo.contract.*;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.*;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.*;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.HgUrlArgument;
import com.thoughtworks.go.util.command.UrlArgument;

import java.util.Collection;
import java.util.List;

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
        else if(crTask instanceof CRBuildTask) {
            return toBuildTask((CRBuildTask)crTask);
        }
        else if(crTask instanceof CRExecTask)
        {
            return toExecTask((CRExecTask)crTask);
        }
        else if(crTask instanceof CRFetchArtifactTask)
        {
            return toFetchTask((CRFetchArtifactTask)crTask);
        }
        else
            throw new RuntimeException(
                    String.format("unknown type of task '%s'",crTask));
    }

    public FetchTask toFetchTask(CRFetchArtifactTask crTask) {
        FetchTask fetchTask = new FetchTask(
                new CaseInsensitiveString(crTask.getPipelineName()),
                new CaseInsensitiveString(crTask.getStage()),
                new CaseInsensitiveString(crTask.getJob()),
                crTask.getSource(),
                crTask.getDestination());

        if(crTask.sourceIsDirectory()) {
            fetchTask.setSrcdir(crTask.getSource());
            fetchTask.setSrcfile(null);
        }
        setCommonTaskMembers(fetchTask,crTask);
        return fetchTask;
    }

    public ExecTask toExecTask(CRExecTask crTask) {
        ExecTask execTask = new ExecTask(crTask.getCommand(), toArgList(crTask.getArgs()), crTask.getWorkingDirectory());
        execTask.setTimeout(crTask.getTimeout());
        setCommonTaskMembers(execTask, crTask);
        return execTask;
    }

    private Arguments toArgList(List<String> args) {
        Arguments arguments = new Arguments();
        for(String arg : args)
        {
            arguments.add(new Argument(arg));
        }
        return arguments;
    }

    public BuildTask toBuildTask(CRBuildTask crBuildTask) {
        BuildTask buildTask;
        switch (crBuildTask.getType())
        {
            case rake:
                buildTask = new RakeTask();
                break;
            case ant:
                buildTask = new AntTask();
                break;
            case nant:
                buildTask = new NantTask();
                break;
            default:
                throw new RuntimeException(
                        String.format("unknown type of build task '%s'",crBuildTask.getType()));
        }
        setCommonBuildTaskMembers(buildTask,crBuildTask);
        setCommonTaskMembers(buildTask,crBuildTask);
        return buildTask;
    }

    private void setCommonBuildTaskMembers(BuildTask buildTask, CRBuildTask crBuildTask) {
        buildTask.buildFile = crBuildTask.getBuildFile();
        buildTask.target = crBuildTask.getTarget();
        buildTask.workingDirectory = crBuildTask.getWorkingDirectory();
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

    public DependencyMaterialConfig toDependencyMaterialConfig(CRDependencyMaterial crDependencyMaterial) {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(
                new CaseInsensitiveString(crDependencyMaterial.getPipelineName()),
                new CaseInsensitiveString(crDependencyMaterial.getStageName()));
        setCommonMaterialMembers(dependencyMaterialConfig,crDependencyMaterial);
        return dependencyMaterialConfig;
    }

    private void setCommonMaterialMembers(AbstractMaterialConfig materialConfig, CRMaterial crMaterial) {
        materialConfig.setName(new CaseInsensitiveString(crMaterial.getName()));
    }

    public MaterialConfig toMaterialConfig(CRMaterial crMaterial) {
        if(crMaterial == null)
            throw new ConfigConvertionException("material cannot be null");

        if(crMaterial instanceof CRDependencyMaterial)
            return toDependencyMaterialConfig((CRDependencyMaterial)crMaterial);
        else if(crMaterial instanceof CRScmMaterial)
        {
            CRScmMaterial crScmMaterial = (CRScmMaterial)crMaterial;
            return toScmMaterialConfig(crScmMaterial);
        }
        else
            throw new ConfigConvertionException(
                    String.format("unknown material type '%s'",crMaterial));
    }

    private ScmMaterialConfig toScmMaterialConfig(CRScmMaterial crScmMaterial) {
        if(crScmMaterial instanceof CRGitMaterial)
        {
            CRGitMaterial git = (CRGitMaterial)crScmMaterial;
            Filter filter = toFilter(crScmMaterial);
            return new GitMaterialConfig(new UrlArgument(git.getUrl()),git.getBranch(),
                    null,git.isAutoUpdate(), filter,crScmMaterial.getFolder(),
                    new CaseInsensitiveString(crScmMaterial.getName()));
        }
        else if(crScmMaterial instanceof CRHgMaterial)
        {
            CRHgMaterial hg = (CRHgMaterial)crScmMaterial;
            return new HgMaterialConfig(new HgUrlArgument(hg.getUrl()),
            hg.isAutoUpdate(), toFilter(crScmMaterial), hg.getFolder(),
                    new CaseInsensitiveString(crScmMaterial.getName()));
        }
        else if(crScmMaterial instanceof CRP4Material)
        {
            CRP4Material crp4Material = (CRP4Material)crScmMaterial;
            P4MaterialConfig p4MaterialConfig = new P4MaterialConfig(crp4Material.getServerAndPort(), crp4Material.getView(), cipher);
            if(crp4Material.getEncryptedPassword() != null)
            {
                p4MaterialConfig.setEncryptedPassword(crp4Material.getEncryptedPassword());
            }
            else
            {
                p4MaterialConfig.setPassword(crp4Material.getPassword());
            }
            p4MaterialConfig.setUserName(crp4Material.getUserName());
            p4MaterialConfig.setUseTickets(crp4Material.getUseTickets());
            setCommonMaterialMembers(p4MaterialConfig,crScmMaterial);
            setCommonScmMaterialMembers(p4MaterialConfig,crp4Material);
            return p4MaterialConfig;
        }
        else
            throw new ConfigConvertionException(
                    String.format("unknown scm material type '%s'",crScmMaterial));
    }

    private void setCommonScmMaterialMembers(ScmMaterialConfig scmMaterialConfig, CRScmMaterial crScmMaterial) {
        scmMaterialConfig.setFolder(crScmMaterial.getFolder());
        scmMaterialConfig.setAutoUpdate(crScmMaterial.isAutoUpdate());
        scmMaterialConfig.setFilter(toFilter(crScmMaterial));
    }

    private Filter toFilter(CRScmMaterial crScmMaterial) {
        Filter filter = new Filter();
        for(String pattern : crScmMaterial.getFilter())
        {
            filter.add(new IgnoredFiles(pattern));
        }
        return filter;
    }

    //TODO #1133 convert each config element
}

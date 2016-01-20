package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class CRStage extends CRBase {
    private String name;
    private boolean fetchMaterials = true;
    private boolean artifactCleanupProhibited;
    private boolean cleanWorkingDir;
    private CRApproval approval ;
    private Collection<CREnvironmentVariable> environmentVariables = new ArrayList<>();
    private Collection<CRJob> jobs = new ArrayList<>();

    public CRStage(String name, boolean fetchMaterials, boolean artifactCleanupProhibited,
                   boolean cleanWorkingDir, CRApproval approval,
                   Collection<CREnvironmentVariable> environmentVariables, Collection<CRJob> jobs) {
        this.name = name;
        this.fetchMaterials = fetchMaterials;
        this.artifactCleanupProhibited = artifactCleanupProhibited;
        this.cleanWorkingDir = cleanWorkingDir;
        this.approval = approval;
        this.environmentVariables = environmentVariables;
        this.jobs = jobs;
    }

    public CRStage()
    {
    }

    public CRStage(String name,CRJob... jobs)
    {
        this.name = name;
        this.jobs = Arrays.asList(jobs);
    }

    public void addEnvironmentVariable(String key,String value){
        CREnvironmentVariable variable = new CREnvironmentVariable(key);
        variable.setValue(value);
        this.environmentVariables.add(variable);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRStage that = (CRStage) o;

        if (fetchMaterials != that.fetchMaterials) {
            return false;
        }
        if (artifactCleanupProhibited != that.artifactCleanupProhibited) {
            return false;
        }
        if (cleanWorkingDir != that.cleanWorkingDir) {
            return false;
        }
        if (approval != null ? !approval.equals(that.approval) : that.approval != null) {
            return false;
        }
        if (jobs != null ? !CollectionUtils.isEqualCollection(jobs, that.jobs) : that.jobs != null) {
            return false;
        }
        if (environmentVariables != null ? !CollectionUtils.isEqualCollection(environmentVariables,that.environmentVariables) : that.environmentVariables != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (fetchMaterials ? 1 : 0);
        result = 31 * result + (artifactCleanupProhibited ? 1 : 0);
        result = 31 * result + (cleanWorkingDir ? 1 : 0);
        result = 31 * result + (approval != null ? approval.hashCode() : 0);
        result = 31 * result + (environmentVariables != null ? environmentVariables.size() : 0);
        result = 31 * result + (jobs != null ? jobs.size() : 0);
        return result;
    }

    private void validateJobNameUniqueness(ErrorCollection errors, String location) {
        if(this.jobs == null)
            return;
        HashSet<String> keys = new HashSet<>();
        for(CRJob var : jobs)
        {
            String error = var.validateNameUniqueness(keys);
            if(error != null)
                errors.addError(location,error);
        }
    }

    private void validateEnvironmentVariableUniqueness(ErrorCollection errors, String location) {
        HashSet<String> keys = new HashSet<>();
        for(CREnvironmentVariable var : environmentVariables)
        {
            String error = var.validateNameUniqueness(keys);
            if(error != null)
                errors.addError(location,error);
        }
    }

    private void validateAtLeastOneJob(ErrorCollection errors, String location) {
        if(this.jobs == null || this.jobs.isEmpty())
            errors.addError(location,"Stage has no jobs");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFetchMaterials() {
        return fetchMaterials;
    }

    public void setFetchMaterials(boolean fetchMaterials) {
        this.fetchMaterials = fetchMaterials;
    }

    public boolean isArtifactCleanupProhibited() {
        return artifactCleanupProhibited;
    }

    public void setArtifactCleanupProhibited(boolean artifactCleanupProhibited) {
        this.artifactCleanupProhibited = artifactCleanupProhibited;
    }

    public boolean isCleanWorkingDir() {
        return cleanWorkingDir;
    }

    public void setCleanWorkingDir(boolean cleanWorkingDir) {
        this.cleanWorkingDir = cleanWorkingDir;
    }

    public CRApproval getApproval() {
        return approval;
    }

    public void setApproval(CRApproval approval) {
        this.approval = approval;
    }

    public Collection<CREnvironmentVariable> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Collection<CREnvironmentVariable> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public Collection<CRJob> getJobs() {
        return jobs;
    }

    public void setJobs(Collection<CRJob> jobs) {
        this.jobs = jobs;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        errors.checkMissing(location,"name",name);
        validateAtLeastOneJob(errors,location);
        validateEnvironmentVariableUniqueness(errors,location);
        validateJobNameUniqueness(errors,location);
        if(approval != null)
            approval.getErrors(errors,location);
    }

    @Override
    public String getLocation(String parent) {
        return null;
    }
}

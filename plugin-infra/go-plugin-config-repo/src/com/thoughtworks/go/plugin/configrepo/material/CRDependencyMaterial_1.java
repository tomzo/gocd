package com.thoughtworks.go.plugin.configrepo.material;

import com.thoughtworks.go.plugin.configrepo.ErrorCollection;
import com.thoughtworks.go.util.StringUtil;

public class CRDependencyMaterial_1 extends CRMaterial_1 {
    public static final String TYPE_NAME = "pipeline";
    private String pipelineName ;
    private String stageName ;

    public CRDependencyMaterial_1() {
        type = TYPE_NAME;
    }

    public CRDependencyMaterial_1(String name,String pipelineName,String stageName) {
        super(TYPE_NAME,name);
        this.pipelineName = pipelineName;
        this.stageName = stageName;
    }
    public CRDependencyMaterial_1(String pipelineName,String stageName) {
        type = TYPE_NAME;
        this.pipelineName = pipelineName;
        this.stageName = stageName;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CRDependencyMaterial_1 that = (CRDependencyMaterial_1) o;
        if(!super.equals(that))
            return false;

        if (pipelineName != null ? !pipelineName.equals(that.pipelineName) : that.pipelineName != null) {
            return false;
        }
        if (stageName != null ? !stageName.equals(that.stageName) : that.stageName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pipelineName != null ? pipelineName.hashCode() : 0);
        result = 31 * result + (stageName != null ? stageName.hashCode() : 0);
        return result;
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validatePipelineName(errors);
        validateStageName(errors);
    }

    private void validateStageName(ErrorCollection errors) {
        if(StringUtil.isBlank(stageName))
            errors.add(this,"material's stage name not set. You must specify on which stage in pipeline to depend on.");
    }

    private void validatePipelineName(ErrorCollection errors) {
        if(StringUtil.isBlank(pipelineName))
            errors.add(this,"material's pipeline name not set. You must specify on which pipeline to depend on.");
    }
}

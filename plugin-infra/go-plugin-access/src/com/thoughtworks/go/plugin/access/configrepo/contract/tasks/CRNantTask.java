package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

public class CRNantTask extends CRBuildTask {
    private String nantPath;

    public CRNantTask(CRRunIf runIf, CRTask onCancel) {
        super(runIf, onCancel);
    }
}

package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

public class CRBuildTask extends CRTask {
    private String buildFile;
    private String target;
    private String workingDirectory;
    private CRBuildFramework type;

    public CRBuildTask(CRRunIf runIf, CRTask onCancel) {
        super(runIf, onCancel);
    }
}

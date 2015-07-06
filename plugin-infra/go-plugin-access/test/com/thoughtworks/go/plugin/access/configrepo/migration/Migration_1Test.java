package com.thoughtworks.go.plugin.access.configrepo.migration;

import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironment;
import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironmentVariable;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.*;
import com.thoughtworks.go.plugin.configrepo.CREnvironment_1;
import com.thoughtworks.go.plugin.configrepo.material.*;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class Migration_1Test {
    private Migration_1 migration;

    @Before
    public void SetUp()
    {
        migration = new Migration_1();
    }

    @Test
    public void shouldMigrateEnvironmentName()
    {
        CREnvironment result = migration.migrate(new CREnvironment_1("dev"));
        assertThat(result.getName(),is("dev"));
    }

    @Test
    public void shouldMigrateEnvironmentVariablesInEnvironment()
    {
        CREnvironment_1 dev = new CREnvironment_1("dev");
        dev.addEnvironmentVariable("key1","value1");
        CREnvironment result = migration.migrate(dev);
        assertThat(result.getEnvironmentVariables(),hasItem(new CREnvironmentVariable("key1","value1")));
    }

    @Test
    public void shouldMigrateAgentsInEnvironment()
    {
        CREnvironment_1 dev = new CREnvironment_1("dev");
        dev.addAgent("123");
        CREnvironment result = migration.migrate(dev);
        assertThat(result.getAgents(),hasItem("123"));
    }

    @Test
    public void shouldMigratePipelinesInEnvironment()
    {
        CREnvironment_1 dev = new CREnvironment_1("dev");
        dev.addPipeline("pipe1");
        CREnvironment result = migration.migrate(dev);
        assertThat(result.getPipelines(),hasItem("pipe1"));
    }

    @Test
    public void shouldMigrateDependencyMaterial()
    {
        CRDependencyMaterial_1 dependencyMaterial_1 = new CRDependencyMaterial_1("pipe1","pipelineA","test");
        CRMaterial result = migration.migrate(dependencyMaterial_1);
        assertThat(result.getName(),is("pipe1"));
        assertThat(result instanceof CRDependencyMaterial,is(true));
        CRDependencyMaterial crDependencyMaterial = (CRDependencyMaterial)result;
        assertThat(crDependencyMaterial.getPipelineName(),is("pipelineA"));
        assertThat(crDependencyMaterial.getStageName(),is("test"));
    }

    @Test
    public void shouldMigratePackageMaterial()
    {
        CRPackageMaterial_1 packageMaterial = new CRPackageMaterial_1("myapt", "apt-package-plugin-id");
        CRMaterial result = migration.migrate(packageMaterial);
        assertThat(result.getName(),is("myapt"));
        assertThat(result instanceof CRPackageMaterial,is(true));
        CRPackageMaterial crDependencyMaterial = (CRPackageMaterial)result;
        assertThat(crDependencyMaterial.getPackageId(),is("apt-package-plugin-id"));
    }

    @Test
    public void shouldMigratePluggableScmMaterial()
    {
        CRPluggableScmMaterial_1 scmMaterial = new CRPluggableScmMaterial_1("myPluggableGit",
                "someScmGitRepositoryId","destinationDir","dir1","dir2");
        CRMaterial result = migration.migrate(scmMaterial);
        assertThat(result.getName(),is("myPluggableGit"));
        assertThat(result instanceof CRPluggableScmMaterial,is(true));
        CRPluggableScmMaterial crPluggableScmMaterial = (CRPluggableScmMaterial)result;
        assertThat(crPluggableScmMaterial.getScmId(),is("someScmGitRepositoryId"));
        assertThat(crPluggableScmMaterial.getName(),is("myPluggableGit"));
        assertThat(crPluggableScmMaterial.getDirectory(),is("destinationDir"));
        assertThat(crPluggableScmMaterial.getFilter(),hasItem("dir1"));
        assertThat(crPluggableScmMaterial.getFilter(),hasItem("dir2"));
    }


    @Test
    public void shouldMigrateGitMaterial()
    {
        CRGitMaterial_1 scmMaterial = new CRGitMaterial_1("gitMaterial1","dir1",false,"url1","branch","externals","tools");

        CRMaterial result = migration.migrate(scmMaterial);
        assertThat(result.getName(),is("gitMaterial1"));
        assertThat(result instanceof CRGitMaterial,is(true));
        CRGitMaterial gitMaterial = (CRGitMaterial)result;

        assertThat(gitMaterial.getUrl(),is("url1"));
        assertThat(gitMaterial.getBranch(),is("branch"));
        assertThat(gitMaterial.getName(),is("gitMaterial1"));
        assertThat(gitMaterial.getFolder(), is("dir1"));
        assertThat(gitMaterial.getFilter(),hasItem("externals"));
        assertThat(gitMaterial.getFilter(),hasItem("tools"));
    }


    @Test
    public void shouldMigrateHgMaterial()
    {
        CRHgMaterial_1 scmMaterial = new CRHgMaterial_1("hgMaterial1","dir1",false,"url1","externals","tools");

        CRMaterial result = migration.migrate(scmMaterial);
        assertThat(result.getName(),is("hgMaterial1"));
        assertThat(result instanceof CRHgMaterial,is(true));
        CRHgMaterial hgMaterial = (CRHgMaterial)result;

        assertThat(hgMaterial.getUrl(),is("url1"));
        assertThat(hgMaterial.getName(),is("hgMaterial1"));
        assertThat(hgMaterial.getFolder(), is("dir1"));
        assertThat(hgMaterial.getFilter(),hasItem("externals"));
        assertThat(hgMaterial.getFilter(),hasItem("tools"));
    }

    @Test
    public void shouldMigrateSvnMaterial_WithPlainPassword()
    {
        CRSvnMaterial_1 scmMaterial = new CRSvnMaterial_1("svnMaterial1","destDir1", false,
                "http://svn","user1","pass1",true,"tools","lib");

        CRMaterial result = migration.migrate(scmMaterial);
        assertThat(result.getName(),is("svnMaterial1"));
        assertThat(result instanceof CRSvnMaterial,is(true));
        CRSvnMaterial svnMaterial = (CRSvnMaterial)result;

        assertThat(svnMaterial.getUrl(),is("http://svn"));
        assertThat(svnMaterial.getUsername(),is("user1"));
        assertThat(svnMaterial.getPassword(),is("pass1"));
        assertNull(svnMaterial.getEncryptedPassword());
        assertThat(svnMaterial.isCheckExternals(),is(true));
        assertThat(svnMaterial.getName(),is("svnMaterial1"));
        assertThat(svnMaterial.getFolder(), is("destDir1"));
        assertThat(svnMaterial.getFilter(),hasItem("lib"));
        assertThat(svnMaterial.getFilter(),hasItem("tools"));
    }
    @Test
    public void shouldMigrateSvnMaterial_WithEncryptedPassword()
    {
        CRSvnMaterial_1 scmMaterial = new CRSvnMaterial_1("svnMaterial1","destDir1", false,
                "http://svn","user1","pass1",true,"tools","lib");
        scmMaterial.setPassword(null);
        scmMaterial.setEncryptedPassword("127g736gfr");

        CRMaterial result = migration.migrate(scmMaterial);
        assertThat(result.getName(),is("svnMaterial1"));
        assertThat(result instanceof CRSvnMaterial,is(true));
        CRSvnMaterial svnMaterial = (CRSvnMaterial)result;

        assertThat(svnMaterial.getUrl(),is("http://svn"));
        assertThat(svnMaterial.getUsername(),is("user1"));
        assertThat(svnMaterial.getEncryptedPassword(),is("127g736gfr"));
        assertNull(svnMaterial.getPassword());
        assertThat(svnMaterial.isCheckExternals(),is(true));
        assertThat(svnMaterial.getName(),is("svnMaterial1"));
        assertThat(svnMaterial.getFolder(), is("destDir1"));
        assertThat(svnMaterial.getFilter(),hasItem("lib"));
        assertThat(svnMaterial.getFilter(),hasItem("tools"));
    }
}

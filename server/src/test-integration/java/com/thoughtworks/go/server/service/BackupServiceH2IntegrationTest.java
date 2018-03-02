/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.junitext.DatabaseChecker;
import com.thoughtworks.go.junitext.GoJUnitExtSpringRunner;
import com.thoughtworks.go.security.CipherProvider;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.ServerBackup;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.ServerBackupRepository;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.util.ServerVersion;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.h2.tools.Restore;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@RunWith(GoJUnitExtSpringRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class BackupServiceH2IntegrationTest {

    @Autowired
    GoConfigService goConfigService;
    @Autowired DataSource dataSource;
    @Autowired ArtifactsDirHolder artifactsDirHolder;
    @Autowired DatabaseAccessHelper dbHelper;
    @Autowired
    GoConfigDao goConfigDao;
    @Autowired
    ServerBackupRepository backupInfoRepository;
    @Autowired Localizer localizer;
    @Autowired SystemEnvironment systemEnvironment;
    @Autowired ServerVersion serverVersion;
    @Autowired ConfigRepository configRepository;
    @Autowired Database databaseStrategy;
    @Autowired BackupService backupService;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    private File backupsDirectory;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private byte[] originalCipher;
    private Username admin;

    @Before
    public void setUp() throws Exception {
        configHelper.onSetUp();
        dbHelper.onSetUp();
        admin = new Username(new CaseInsensitiveString("admin"));
        configHelper.enableSecurity();
        configHelper.addAdmins(CaseInsensitiveString.str(admin.getUsername()));
        goConfigDao.forceReload();
        backupsDirectory = new File(artifactsDirHolder.getArtifactsDir(), ServerConfig.SERVER_BACKUPS);
        FileUtils.deleteQuietly(backupsDirectory);
        originalCipher = new CipherProvider(systemEnvironment).getKey();

        FileUtils.writeStringToFile(new File(systemEnvironment.getConfigDir(), "cruise-config.xml"), "invalid crapy config", UTF_8);
        FileUtils.writeStringToFile(new File(systemEnvironment.getConfigDir(), "cipher"), "invalid crapy cipher", UTF_8);
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
        FileUtils.writeStringToFile(new File(systemEnvironment.getConfigDir(), "cruise-config.xml"), goConfigService.xml(), UTF_8);
        FileUtils.writeByteArrayToFile(systemEnvironment.getCipherFile(), originalCipher);
        configHelper.onTearDown();
        FileUtils.deleteQuietly(backupsDirectory);
    }

    @Test
    @RunIf(value = DatabaseChecker.class, arguments = {DatabaseChecker.H2})
    public void shouldCreateTheBackupUnderArtifactRepository() {
        TimeProvider timeProvider = mock(TimeProvider.class);
        DateTime now = new DateTime();
        when(timeProvider.currentDateTime()).thenReturn(now);
        assertThat(backupsDirectory.exists(), is(false));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        BackupService service = new BackupService(dataSource, artifactsDirHolder, goConfigService, timeProvider, backupInfoRepository, systemEnvironment, serverVersion, configRepository,
                databaseStrategy);
        service.startBackup(admin, result);

        assertThat(result.isSuccessful(), is(true));
        assertThat(backupsDirectory.exists(), is(true));
        assertThat(backupsDirectory.isDirectory(), is(true));
        File backup = new File(backupsDirectory, BackupService.BACKUP + now.toString("YYYYMMdd-HHmmss"));
        assertThat(backup.exists(), is(true));
        assertThat(new File(backup, "db.zip").exists(), is(true));
        assertEquals(new ServerBackup(backup.getAbsolutePath(), now.toDate(), admin.getUsername().toString()), backupInfoRepository.lastBackup());
    }

    @Test
    @RunIf(value = DatabaseChecker.class, arguments = {DatabaseChecker.H2})
    public void shouldPerformDbBackupProperly() throws SQLException, IOException {
        Pipeline expectedPipeline = saveAPipeline();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        backupService.startBackup(admin, result);
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.message(localizer), is("Backup completed successfully."));

        String location = temporaryFolder.newFolder().getAbsolutePath();

        Restore.execute(dbZip(), location, "cruise", false);

        BasicDataSource source = constructTestDataSource(new File(location));
        ResultSet resultSet = source.getConnection().prepareStatement("select * from pipelines where id = " + expectedPipeline.getId()).executeQuery();
        int size = 0;
        while (resultSet.next()) {
            assertThat(resultSet.getString("name"), is(expectedPipeline.getName()));
            size++;
        }
        assertThat(size, is(1));
    }

    private Pipeline saveAPipeline() {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline", new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(new JobConfig("job-one"))));
        pipelineConfig.materialConfigs().clear();
        SvnMaterialConfig onDirOne = MaterialConfigsMother.svnMaterialConfig("google.com", "dirOne", "loser", "boozer", false, "**/*.html");
        pipelineConfig.addMaterialConfig(onDirOne);

        Pipeline building = PipelineMother.building(pipelineConfig);

        return dbHelper.savePipelineWithMaterials(building);
    }

    private String dbZip() {
        return backedUpFile("db.zip").getAbsolutePath();
    }

    private File backedUpFile(final String filename) {
        return new ArrayList<>(FileUtils.listFiles(backupsDirectory, new NameFileFilter(filename), TrueFileFilter.TRUE)).get(0);
    }

    private BasicDataSource constructTestDataSource(File file) {
        BasicDataSource source = new BasicDataSource();
        source.setDriverClassName("org.h2.Driver");
        source.setUrl("jdbc:h2:" + file.getAbsolutePath() + "/cruise;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        source.setUsername("sa");
        source.setPassword("");
        source.setMaxActive(32);
        source.setMaxIdle(32);
        return source;
    }



}

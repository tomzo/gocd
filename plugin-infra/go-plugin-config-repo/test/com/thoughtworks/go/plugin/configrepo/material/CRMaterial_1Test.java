package com.thoughtworks.go.plugin.configrepo.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.CRBaseTest;
import com.thoughtworks.go.plugin.configrepo.material.CRDependencyMaterial_1;
import com.thoughtworks.go.plugin.configrepo.material.CRMaterial_1;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRMaterial_1Test extends CRBaseTest<CRMaterial_1> {

    private CRDependencyMaterial_1 dependsOnPipeline;
    private CRPackageMaterial_1 packageMaterial ;

    public CRMaterial_1Test()
    {
        dependsOnPipeline = new CRDependencyMaterial_1("pipe1","pipeline1","build");
        packageMaterial = new CRPackageMaterial_1("apt-repo-id");
    }

    @Override
    public void addGoodExamples(Map<String, CRMaterial_1> examples) {
        examples.put("dependsOnPipeline",dependsOnPipeline);
        examples.put("packageMaterial",packageMaterial);
    }

    @Override
    public void addBadExamples(Map<String, CRMaterial_1> examples) {

    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        Map<String, CRMaterial_1> examples = getExamples();
        for(Map.Entry<String,CRMaterial_1> example : examples.entrySet()) {
            CRMaterial_1 value = example.getValue();
            JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
            assertNotNull(jsonObject.get("type"));
        }
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializing_CRDependencyMaterial_1()
    {
        CRMaterial_1 value = dependsOnPipeline;
        String json = gson.toJson(value);

        CRDependencyMaterial_1 deserializedValue = (CRDependencyMaterial_1)gson.fromJson(json,CRMaterial_1.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializing_CRPackageMaterial_1()
    {
        CRMaterial_1 value = packageMaterial;
        String json = gson.toJson(value);

        CRPackageMaterial_1 deserializedValue = (CRPackageMaterial_1)gson.fromJson(json,CRMaterial_1.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }

}

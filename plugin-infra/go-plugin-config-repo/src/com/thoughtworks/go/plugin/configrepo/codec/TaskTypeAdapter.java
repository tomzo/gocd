package com.thoughtworks.go.plugin.configrepo.codec;

import com.google.gson.*;
import com.thoughtworks.go.plugin.configrepo.tasks.CRExecTask_1;
import com.thoughtworks.go.plugin.configrepo.tasks.CRTask_1;

import java.lang.reflect.Type;

public class TaskTypeAdapter implements JsonDeserializer<CRTask_1> {
    private static final String TYPE = "type";

    @Override
    public CRTask_1 deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject =  json.getAsJsonObject();
        JsonPrimitive prim = (JsonPrimitive) jsonObject.get(TYPE);
        String typeName = prim.getAsString();

        Class<?> klass = classForName(typeName);
        return context.deserialize(jsonObject, klass);
    }
    private Class<?> classForName(String typeName) {
        if(typeName.equals(CRExecTask_1.TYPE_NAME))
            return CRExecTask_1.class;
        else
            throw new JsonParseException(
                    String.format("Invalid or unknown material type '%s'",typeName));
    }
}

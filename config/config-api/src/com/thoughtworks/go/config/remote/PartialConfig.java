package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.ConfigErrors;

/**
 * Part of cruise configuration that can be stored outside of main cruise-config.xml.
 * It can be merged with others and main configuration.
 */
public class PartialConfig implements Validatable {
    // consider to include source of this part.

    private EnvironmentsConfig environments;

    // TODO lists of pipelines in some groups.
    // but without Authorization section because it makes no sense to store it in repo.
    // so we need other class than PipelineConfigs


    // to some extent only this part of configuration could be validated
    // but inside MergeCruiseConfig it will just validate globally

    @Override
    public void validate(ValidationContext validationContext) {

    }

    @Override
    public ConfigErrors errors() {
        return null;
    }

    @Override
    public void addError(String fieldName, String message) {

    }


}

package com.playsql.extensions.reqif.ui;

import com.google.common.collect.Lists;
import com.playsql.extensions.reqif.xml.ReqifXmlElements.SpecObjectType;

import java.util.List;

/**
 * Represents a {@link SpecObjectType}, but hides the XML implementation details, and makes it
 * ready to be used by a .vm file.
 */
public class UIRequirementType {
    private String identifier;
    private String label;
    List<UIAttributeDefinition> uiAttributeDefinitions = Lists.newArrayList();

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<UIAttributeDefinition> getUiAttributeDefinitions() {
        return uiAttributeDefinitions;
    }

    public void setUiAttributeDefinitions(List<UIAttributeDefinition> uiAttributeDefinitions) {
        this.uiAttributeDefinitions = uiAttributeDefinitions;
    }
}

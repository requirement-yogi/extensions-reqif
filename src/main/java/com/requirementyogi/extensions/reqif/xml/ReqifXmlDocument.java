package com.requirementyogi.extensions.reqif.xml;

/*-
 * #%L
 * Play SQL - ReqIF Import
 * %%
 * Copyright (C) 2019 Play SQL S.A.S.U.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.collect.Lists;
import com.requirementyogi.extensions.reqif.ui.UIAttributeDefinition;
import com.requirementyogi.extensions.reqif.ui.UIRequirement;
import com.requirementyogi.extensions.reqif.ui.UIRequirementType;
import com.requirementyogi.extensions.reqif.ui.UIRequirementValue;
import com.requirementyogi.extensions.reqif.xml.ReqifXmlElements.*;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * An in-memory representation of a whole XML file.
 */
public class ReqifXmlDocument extends XmlElement {
    private List<SpecObjectType> specObjectTypes = Lists.newArrayList();
    private List<SpecObject> specObjects = Lists.newArrayList();
    private List<SpecRelationType> specRelationTypes = Lists.newArrayList();
    private List<SpecRelation> specRelations = Lists.newArrayList();
    private List<AttributeDefinition> attributeDefinitions = Lists.newArrayList();
    private List<EnumValue> enumValues = Lists.newArrayList();
    private List<DatatypeDefinitionEnumeration> datatypeDefinitionEnumerations = Lists.newArrayList();

    /* === Hard-coded types ==
     * All the datatypes are stored, they will be much needed using random access.
     */
    void declare(SpecObjectType element) {
        specObjectTypes.add(element);
    }
    void declare(SpecObject element) {
        specObjects.add(element);
    }
    void declare(SpecRelationType element) {
        specRelationTypes.add(element);
    }
    void declare(SpecRelation element) {
        specRelations.add(element);
    }
    void declare(AttributeDefinition element) {
        attributeDefinitions.add(element);
    }
    void declare(EnumValue element) {
        enumValues.add(element);
    }
    void declare(DatatypeDefinitionEnumeration element) {
        datatypeDefinitionEnumerations.add(element);
    }

    public <T extends Reffable> T getDeclaredObject(String type, String identifier) {
        switch (type) {
            case "SPEC-OBJECT-TYPE": return (T) get(specObjectTypes, type, identifier);
            case "SPEC-OBJECT": return (T) get(specObjects, type, identifier);
            case "SPEC-RELATION-TYPE": return (T) get(specRelationTypes, type, identifier);
            case "SPEC-RELATION": return (T) get(specRelations, type, identifier);
            case "ATTRIBUTE-DEFINITION-STRING":
            case "ATTRIBUTE-DEFINITION-XHTML":
            case "ATTRIBUTE-DEFINITION-BOOLEAN":
            case "ATTRIBUTE-DEFINITION-DATE":
            case "ATTRIBUTE-DEFINITION-ENUMERATION":
            case "ATTRIBUTE-DEFINITION-INTEGER":
            case "ATTRIBUTE-DEFINITION-REAL":
                return (T) get(attributeDefinitions, type, identifier);
            case "ENUM-VALUE": return (T) get(enumValues, type, identifier);
            case "DATATYPE-DEFINITION-ENUMERATION": return (T) get(datatypeDefinitionEnumerations, type, identifier);
            default: return null;
        }
    }

    private static <T extends Reffable> T get(List<T> list, String type, String identifier) {
        for (T item : list)
            if (Objects.equals(item.identifier, identifier)
                && Objects.equals(item.localName, type))
                return item;
        return null;
    }

    /**
     * Transform the XML parsing beans into UI beans
     */
    public List<UIRequirement> getRequirements(String filterType) {
        List<UIRequirement> requirements = Lists.newArrayList();
        for (SpecObject specObject : specObjects) {
            UIRequirement requirement = new UIRequirement(specObject.identifier);
            SpecObjectType type = specObject.getType();
            if (type != null) {
                requirement.setTypeId(type.identifier);
                String typeName = type.getName();
                if (filterType != null) {
                    if (!Objects.equals(filterType, type.identifier))
                        continue; // Skips this requirement
                }
                if (typeName != null) {
                    requirement.setTypeName(typeName);
                }
            }
            requirement.setDescription(specObject.description);
            requirement.setLastChange(specObject.lastChange);
            for (AttributeValue attribute : specObject.getAttributes()) {
                AttributeDefinition definition = attribute.getDefinition();
                if (definition != null && StringUtils.isNotBlank(definition.identifier)) {
                    if (definition.isXhtml()) {
                        requirement.put(new UIRequirementValue(definition.identifier, attribute.getValueInXhtml(), true));
                    } else {
                        requirement.put(new UIRequirementValue(definition.identifier, attribute.getValueInAttributes(), false));
                    }
                }
            }
            requirements.add(requirement);
        }
        return requirements;
    }

    public boolean hasRequirements(UIRequirementType uiRequirementType) {
        for (SpecObject specObject : specObjects) {
            SpecObjectType type = specObject.getType();
            if (type != null) {
                if (Objects.equals(uiRequirementType.getIdentifier(), type.identifier))
                    return true;
                }
        }
        return false;
    }

    public List<UIRequirementType> getSpecObjectTypes() {
        List<UIRequirementType> result = Lists.newArrayList();
        for (SpecObjectType specObjectType : specObjectTypes) {
            UIRequirementType type = new UIRequirementType();
            type.setIdentifier(specObjectType.identifier);
            type.setLabel(specObjectType.getName());
            List<UIAttributeDefinition> definitions = Lists.newArrayList();
            for (AttributeDefinition attributeDefinition : specObjectType.getAttributeDefinitions()) {
                UIAttributeDefinition definition = new UIAttributeDefinition();
                definition.setIdentifier(attributeDefinition.identifier);
                definition.setName(attributeDefinition.getNameOrIdentifier());
                definition.setDescription(attributeDefinition.description);
                definitions.add(definition);
            }
            type.setUiAttributeDefinitions(definitions);
            result.add(type);
        }
        return result;
    }
}

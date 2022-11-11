package com.requirementyogi.extensions.reqif.ui;

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

import com.atlassian.confluence.content.render.xhtml.RenderedContentCleaner;
import com.google.common.collect.Lists;
import com.playsql.requirementyogi.api.DocumentImporterAPI.RequirementPersister;
import com.playsql.requirementyogi.api.beans.ImportedRef;
import com.playsql.requirementyogi.api.beans.Property;
import com.playsql.requirementyogi.api.beans.Requirement;
import com.requirementyogi.extensions.reqif.ReqifDescriptor;
import com.requirementyogi.extensions.reqif.ReqifUtils;
import com.requirementyogi.extensions.reqif.xml.ReqifConfig;
import com.requirementyogi.extensions.reqif.xml.ReqifConfig.ColumnMapping;
import com.requirementyogi.extensions.reqif.xml.ReqifConfig.SpecObjectMapping;
import com.requirementyogi.extensions.reqif.xml.ReqifXmlDocument;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.List;
import java.util.Objects;

import static com.playsql.requirementyogi.api.beans.enums.PropertyType.INLINE;
import static com.playsql.requirementyogi.search.enums.CriterionKey.PROPERTY_SEPARATOR;

/**
 * An object for the UI which can return all the information of the parsed document.
 */
public class UIReqifDocument {
    private final ReqifXmlDocument xmlDocument;
    private final RenderedContentCleaner antisamy;

    public UIReqifDocument(ReqifXmlDocument xmlDocument, RenderedContentCleaner antisamy) {
        this.xmlDocument = xmlDocument;
        this.antisamy = antisamy;
    }

    public List<UIRequirementType> getSpecObjectTypes() {
        return xmlDocument.getSpecObjectTypes();
    }

    public List<UIRequirementType> getSpecObjectTypesWithRequirements() {
        List<UIRequirementType> result = Lists.newArrayList();
        for (UIRequirementType type : xmlDocument.getSpecObjectTypes())
            if (xmlDocument.hasRequirements(type))
                result.add(type);
        return result;
    }

    public List<UIRequirementType> getSpecObjectTypesWithoutRequirements() {
        List<UIRequirementType> result = Lists.newArrayList();
        for (UIRequirementType type : xmlDocument.getSpecObjectTypes())
            if (!xmlDocument.hasRequirements(type))
                result.add(type);
        return result;
    }

    public List<UIRequirement> getRequirements() {
        return xmlDocument.getRequirements(null);
    }

    public List<UIRequirement> getRequirements(String specObjectTypeIdentifier, int max, String focus) {
        if (specObjectTypeIdentifier == null)
            // Ensure we don't call getRequirements(null), otherwise all requirements will be returned
            return Lists.newArrayList();
        List<UIRequirement> requirements = xmlDocument.getRequirements(specObjectTypeIdentifier);

        // Narrow down around the focus
        if (focus != null) {
            int row = indexOf(requirements, focus);
            if (row != -1) {
                requirements.get(row).setFocus(true);
                int startAt = ReqifUtils.between(0, Math.min(row - max/2, requirements.size() - max), null);
                int endAt = ReqifUtils.between(0, startAt + max, requirements.size());
                requirements = requirements.subList(startAt, endAt);
            }
        }

        // And apply the max
        if (requirements.size() > max) {
            requirements = requirements.subList(0, max);
        }

        return requirements;
    }

    private int indexOf(List<UIRequirement> requirements, String focus) {
        for (int i = 0; i < requirements.size() ; i++) {
            if (Objects.equals(requirements.get(i).getIdentifier(), focus))
                return i;
        }
        return -1;
    }

    /**
     * Based on the config and the document, build the Requirement.java objects and call the api to import them
     */
    public void importRequirements(RequirementPersister api, ReqifConfig config, String spaceKey, String documentId) {
        if (config == null) throw new NullPointerException("config");
        for (UIRequirement uiRequirement : getRequirements()) {
            String type = uiRequirement.get("#TYPEID").getValue();
            if (type != null) {
                SpecObjectMapping mapping = config.getMapping(type);
                if (mapping != null) {
                    Requirement requirement = new Requirement();
                    List<Property> properties = Lists.newArrayList();
                    for (ColumnMapping columnMapping : mapping.getMappings()) {
                        UIRequirementValue value = uiRequirement.get(columnMapping.getIdentifier());
                        if (value != null && value.getValue() != null && columnMapping.getTarget() != null) {
                            if (columnMapping.isTargetKey()) {
                                requirement.setKey(value.getValue());
                            } else if (columnMapping.isTargetText()) {
                                String existingHtml = StringUtils.isNotBlank(requirement.getHtmlExcerpt()) ? requirement.getHtmlExcerpt() + " - " : "";
                                if (value.isXhtml()) {
                                    requirement.setHtmlExcerpt(existingHtml + sanitize(value.getValue()));
                                } else {
                                    requirement.setHtmlExcerpt(existingHtml + escape(value.getValue()));
                                }
                            } else if (columnMapping.getTarget().startsWith(PROPERTY_SEPARATOR)) {
                                String propertyName = columnMapping.getTarget().substring(1);
                                Property property;
                                if (value.isXhtml()) {
                                    property = new Property(INLINE, propertyName, sanitize(value.getValue()));
                                } else {
                                    property = new Property(INLINE, propertyName, escape(value.getValue()));
                                }
                                properties.add(property);
                            }
                        }
                    }
                    if (StringUtils.isNotBlank(mapping.getCategory())) {
                        properties.add(new Property(INLINE, Property.KEY_CATEGORY, escape(mapping.getCategory())));
                    }
                    requirement.setProperties(properties);
                    requirement.setSpaceKey(spaceKey);
                    requirement.setOrigin(new ImportedRef(ReqifDescriptor.DESCRIPTOR_KEY, documentId, null, uiRequirement.getIdentifier()));
                    api.saveRequirement(requirement);
                }
            }
        }
    }

    private String escape(String value) {
        return StringEscapeUtils.escapeHtml4(value);
    }

    private String sanitize(String value) {
        return antisamy.cleanQuietly(value);
    }

}

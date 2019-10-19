package com.playsql.extensions.reqif.xml;

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
import com.playsql.extensions.reqif.xml.ReqifXmlElements.AttributeDefinition;
import com.playsql.extensions.reqif.xml.ReqifXmlElements.SpecObjectType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Objects;

/**
 * A JSON-enabled class which stores the import configuration for a ReqIF file.
 * The configuration maps the ReqIF attributes to Requirement Yogi attributes.
 * <p/>
 * This is what the user produces when they map fields in the "ReqIF" screen.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ReqifConfig {
    private final List<SpecObjectMapping> mappings = Lists.newArrayList();

    public SpecObjectMapping getMapping(String type) {
        for (SpecObjectMapping mapping : mappings) {
            if (Objects.equals(mapping.identifier, type))
                return mapping;
        }
        return null;
    }

    public boolean isImported(String type) {
        SpecObjectMapping mapping = getMapping(type);
        if (mapping == null) return false;
        return mapping.isImported();
    }

    public String getMappingCategory(String type) {
        SpecObjectMapping mapping = getMapping(type);
        if (mapping == null) return null;
        return mapping.getCategory();
    }

    public ColumnMapping getMapping(String type, String column) {
        SpecObjectMapping mapping = getMapping(type);
        if (mapping == null) return null;
        return mapping.getMapping(column);
    }

    public static class SpecObjectMapping {
        /** The type of the specobject, as returned by {@link SpecObjectType#getName()} */
        private String identifier;
        private boolean imported;
        private String category;
        private List<ColumnMapping> mappings = Lists.newArrayList();

        /**
         * Return the mapping for a column, if exists.
         * @param columnKey the identifier of the attribute
         */
        public ColumnMapping getMapping(String columnKey) {
            for (ColumnMapping mapping : mappings)
                if (Objects.equals(columnKey, mapping.identifier))
                    return mapping;
            return null;
        }

        public String getIdentifier() {
            return identifier;
        }

        public List<ColumnMapping> getMappings() {
            return mappings;
        }

        public boolean isImported() {
            return imported;
        }

        public void setImported(boolean imported) {
            this.imported = imported;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }

    public static class ColumnMapping {
        /** Possible value for the target - the requirement key */
        public static final String KEY = "KEY";
        /** Possible value for the target - the requirement's htmlExcerpt */
        public static final String TEXT = "TEXT";
        /** The identifier of the attribute, as returned by {@link AttributeDefinition#getIdentifier()} */
        private String identifier;
        /** KEY, TEXT, or @property. If empty/null/missing, the column is not mapped. */
        private String target;

        public String getIdentifier() {
            return identifier;
        }

        public String getTarget() {
            return target;
        }

        public boolean isTargetKey() {
            return Objects.equals(KEY, target);
        }

        public boolean isTargetText() {
            return Objects.equals(TEXT, target);
        }

        public boolean isTargetProperty() {
            return target != null && target.startsWith("@");
        }

        public String getMappedProperty() {
            if (isTargetProperty())
                return target.substring(1);
            return null;
        }
    }
}

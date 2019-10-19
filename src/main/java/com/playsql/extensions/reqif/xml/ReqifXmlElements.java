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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.xml.sax.Attributes;

import javax.xml.bind.annotation.XmlAttribute;
import java.util.List;
import java.util.Objects;

/**
 * Contains the JAXB objects to parse the ReqIF files.
 * <p/>
 * If you wonder why we're not using JAXB directly, it is because the ReqIF XSD file depends on the
 * general XHTML and many other files, and I don't know how to generate a partial JAXB model.
 */
public class ReqifXmlElements {

    /** The base class for all elements */
    public abstract static class XmlElement {
        protected final StringBuilder sb = new StringBuilder();

        @XmlAttribute(name = "#localName")
        protected String localName;

        @XmlAttribute(name = "#document")
        protected ReqifXmlDocument document;

        public void append(String string) {
            String trimmed = string.trim();
            if (!string.startsWith(trimmed)) sb.append(" ");
            sb.append(trimmed);
            if (!string.endsWith(trimmed)) sb.append(" ");
            //sb.append(string.trim());
        }

        public void start(String uri, String localName, Attributes attributes) {
            // By default, do nothing, unless one wants to collect the XHTML.
        }

        /**
         * Declare that a child was added. By default, does nothing.
         */
        public void addChild(XmlElement child) {
            // Do nothing
        }

        public void end(XmlElement parent) {
            // By default, do nothing
        }

        @Override
        public String toString() {
            return String.format("%s %s",
                this.getClass().getSimpleName(),
                Objects.toString(sb));
        }
    }

    /**
     * An XmlElement which captures its text contents.
     * This avoid capturing text nodes "by default" */
    public abstract static class TextCapturingXmlElement extends XmlElement {

        /**
         * Declare that a child was added. By default, it only coalesces the texts.
         */
        public void addChild(XmlElement child) {
            sb.append(child.sb.toString());
        }

        /**
         * Returns the contents of the stringbuilder. By default, it contains the
         * inner text, trimmed.
         */
        public String getText() {
            return sb.toString().trim();
        }
    }

    public abstract static class Reffable extends XmlElement {
        @XmlAttribute(name = "IDENTIFIER")
        String identifier;

        @XmlAttribute(name = "LONG-NAME")
        String longName;

        @XmlAttribute(name = "DESC")
        String description;

        @XmlAttribute(name = "LAST-CHANGE")
        String lastChange;

        @Override
        public String toString() {
            // Example: "SPEC-OBJECT 100001 <div>contents</div>"
            return String.format("%s %s, %s",
                localName,
                identifier,
                Objects.toString(sb));
        }
    }

    public abstract static class Ref<T extends Reffable> extends TextCapturingXmlElement {

        private T original;

        public T getOriginal() {
            return original;
        }

        @Override
        public void end(XmlElement parent) {
            super.end(parent);

            // Since it's reference to a reffable element and they're all declared in the first sections of the file,
            // we should be able to find its value in xmlDocument immediately.
            String type = StringUtils.removeEnd(localName, "-REF");
            String id = sb.toString().trim();
            this.original = getDeclaredObject(type, id);
        }

        protected T getDeclaredObject(String type, String identifier) {
            T object = document.getDeclaredObject(type, identifier);
            if (object == null)
                object = addDefinitionNotFound(type, identifier);
            return object;
        }

        protected T addDefinitionNotFound(String type, String identifier) {
            throw new RuntimeException("Reference not found: " + type + " " + identifier);
        }
    }

    public static class Unknown extends XmlElement {

    }

    private static String escapeXmlAttributeValue(String value) {
        return StringEscapeUtils.escapeXml11(value);
    }

    @XmlElementHandler("EMBEDDED-VALUE")
    public static class EmbeddedValue extends XmlElement {

        @XmlAttribute(name = "KEY")
        private String requirementAttributeValue;

    }

    @XmlElementHandler("DATATYPE-DEFINITION-ENUMERATION")
    public static class DatatypeDefinitionEnumeration extends Reffable {

        @Override
        public void end(XmlElement parent) {
            super.end(parent);
            document.declare(this);
        }
    }

    @XmlElementHandler("DATATYPE-DEFINITION-ENUMERATION-REF")
    public static class DatatypeDefinitionEnumerationRef extends Ref<DatatypeDefinitionEnumeration> {

    }

    @XmlElementHandler("SPEC-RELATION-TYPE")
    public static class SpecRelationType extends Reffable {

        @Override
        public void end(XmlElement parent) {
            super.end(parent);
            document.declare(this);
        }

    }

    @XmlElementHandler("SPEC-RELATION")
    public static class SpecRelation extends Reffable {

        Type type;
        List<AttributeValue> attributes = Lists.newArrayList();
        SpecObjectRef source;
        SpecObjectRef target;

        @Override
        public void addChild(XmlElement element) {
            if (element instanceof Values) {
                attributes.addAll(((Values) element).getAttributeValues());
            } else if (element instanceof Type) {
                this.type = (Type) element;
            } else if (element instanceof SourceOrTarget) {
                if (((SourceOrTarget) element).isSource()) {
                    source = ((SourceOrTarget) element).getRef();
                } else {
                    target = ((SourceOrTarget) element).getRef();
                }
            }
        }

        @Override
        public void end(XmlElement parent) {
            super.end(parent);
            document.declare(this);
        }
    }

    @XmlElementHandler("TITLE")
    public static class Title extends XmlElement {

    }

    @XmlElementHandler(names = {
        "ATTRIBUTE-VALUE-STRING",
        "ATTRIBUTE-VALUE-BOOLEAN",
        "ATTRIBUTE-VALUE-DATE",
        "ATTRIBUTE-VALUE-INTEGER",
        "ATTRIBUTE-VALUE-REAL",
        "ATTRIBUTE-VALUE-XHTML"})
    public static class AttributeValue extends XmlElement {
        @XmlAttribute(name = "THE-VALUE")
        private String valueInAttributes;
        private String valueInXhtml;

        private Definition definition;

        public AttributeDefinition getDefinition() {
            if (definition.ref != null)
                return definition.ref.getOriginal();
            return null;
        }

        @Override
        public void addChild(XmlElement child) {
            if (child instanceof TheValue) {
                this.valueInXhtml = ((TheValue) child).getXhtml();
            } else if (child instanceof Definition) {
                this.definition = (Definition) child;
            }
        }

        public String getValueInAttributes() {
            return valueInAttributes;
        }

        public String getValueInXhtml() {
            return valueInXhtml;
        }
    }

    @XmlElementHandler(names = {
        "ATTRIBUTE-DEFINITION-STRING",
        "ATTRIBUTE-DEFINITION-XHTML",
        "ATTRIBUTE-DEFINITION-BOOLEAN",
        "ATTRIBUTE-DEFINITION-DATE",
        "ATTRIBUTE-DEFINITION-INTEGER",
        "ATTRIBUTE-DEFINITION-REAL",
        "ATTRIBUTE-DEFINITION-ENUMERATION"})
    public static class AttributeDefinition extends Reffable {

        private Type type;

        @Override
        public void addChild(XmlElement child) {
            if (child instanceof Type) {
                this.type = (Type) child;
            }
        }

        public String getLabel() {
            return StringUtils.isNotBlank(longName) ? longName : identifier;
        }

        public String getNameOrIdentifier() {
            return StringUtils.isNotBlank(longName) ? longName : identifier;
        }

        public String getIdentifier() {
            return identifier;
        }

        @Override
        public void end(XmlElement parent) {
            super.end(parent);
            document.declare(this);
        }

        public boolean isXhtml() {
            return Objects.equals(localName, "ATTRIBUTE-DEFINITION-XHTML");
        }
    }

    @XmlElementHandler(names = {
        "ATTRIBUTE-DEFINITION-STRING-REF",
        "ATTRIBUTE-DEFINITION-BOOLEAN-REF",
        "ATTRIBUTE-DEFINITION-DATE-REF",
        "ATTRIBUTE-DEFINITION-INTEGER-REF",
        "ATTRIBUTE-DEFINITION-REAL-REF",
        "ATTRIBUTE-DEFINITION-XHTML-REF"})
    public static class AttributeDefinitionRef extends Ref<AttributeDefinition> {

        @Override
        protected AttributeDefinition addDefinitionNotFound(String type, String identifier) {
            AttributeDefinition definition = new AttributeDefinition();
            definition.identifier = identifier;
            definition.longName = type + " " + identifier;
            document.declare(definition);
            return definition;
        }
    }

    @XmlElementHandler(names = { "THE-VALUE" })
    public static class TheValue extends XmlElement {

        // We take all XHTML as children
        @Override
        public void addChild(XmlElement child) {
            if (child instanceof XhtmlElement) {
                sb.append(child.sb);
            }
        }

        public String getXhtml() {
            return sb.toString().trim();
        }
    }

    @XmlElementHandler("ENUM-VALUE-REF")
    public static class EnumValueRef extends Ref<EnumValue> {

    }

    @XmlElementHandler("ATTRIBUTE-VALUE-ENUMERATION")
    public static class AttributeValueEnumeration extends XmlElement {

    }

    @XmlElementHandler("ATTRIBUTE-DEFINITION-ENUMERATION-REF")
    public static class AttributeDefinitionEnumerationRef extends XmlElement {

    }

    @XmlElementHandler("TYPE")
    public static class Type extends XmlElement {

        private SpecObjectTypeRef ref;

        @Override
        public void addChild(XmlElement child) {
            if (child instanceof SpecObjectTypeRef) {
                this.ref = (SpecObjectTypeRef) child;
            }
        }
    }

    @XmlElementHandler("DEFINITION")
    public static class Definition extends XmlElement {

        private AttributeDefinitionRef ref;

        @Override
        public void addChild(XmlElement child) {
            if (child instanceof AttributeDefinitionRef) {
                ref = (AttributeDefinitionRef) child;
            }
        }

    }

    @XmlElementHandler("SPEC-OBJECT-TYPE")
    public static class SpecObjectType extends Reffable {

        private List<AttributeDefinition> attributeDefinitions = Lists.newArrayList();

        @Override
        public void end(XmlElement parent) {
            super.end(parent);
            document.declare(this);
        }

        @Override
        public void addChild(XmlElement child) {
            if (child instanceof SpecAttributes) {
                attributeDefinitions.addAll(((SpecAttributes) child).definitions);
            }
        }

        public List<AttributeDefinition> getAttributeDefinitions() {
            return attributeDefinitions;
        }

        public String getName() {
            return StringUtils.isNotBlank(longName) ? longName : identifier;
        }
    }

    @XmlElementHandler("SPEC-OBJECT-TYPE-REF")
    public static class SpecObjectTypeRef extends Ref<SpecObjectType> {

        @Override
        protected SpecObjectType addDefinitionNotFound(String type, String identifier) {
            SpecObjectType definition = new SpecObjectType();
            definition.identifier = identifier;
            definition.longName = type + " " + identifier;
            document.declare(definition);
            return definition;
        }
    }

    @XmlElementHandler("SPEC-OBJECT")
    public static class SpecObject extends Reffable {

        private Type type;

        private List<AttributeValue> attributes = Lists.newArrayList();

        @Override
        public void addChild(XmlElement element) {
            if (element instanceof Values) {
                attributes.addAll(((Values) element).getAttributeValues());
            } else if (element instanceof Type) {
                this.type = (Type) element;
            }
        }

        @Override
        public void end(XmlElement parent) {
            super.end(parent);
            document.declare(this);
        }

        public SpecObjectType getType() {
            if (type != null && type.ref != null)
                return type.ref.getOriginal();
            return null;
        }

        public Iterable<? extends AttributeValue> getAttributes() {
            return attributes;
        }
    }

    @XmlElementHandler("VALUES")
    public static class Values extends XmlElement {

        private List<AttributeValue> values = Lists.newArrayList();

        @Override
        public void addChild(XmlElement child) {
            if (child instanceof AttributeValue)
                values.add((AttributeValue) child);
        }

        public List<AttributeValue> getAttributeValues() {
            return values;
        }
    }

    @XmlElementHandler("SPEC-OBJECT-REF")
    public static class SpecObjectRef extends Ref<SpecObject> {

    }

    @XmlElementHandler("TARGET, SOURCE")
    public static class SourceOrTarget extends XmlElement {

        private SpecObjectRef ref;

        @Override
        public void addChild(XmlElement child) {
            if (child instanceof SourceOrTarget) {
                ref = (SpecObjectRef) child;
            }
        }

        public boolean isSource() {
            return Objects.equals(localName, "SOURCE");
        }

        public SpecObjectRef getRef() {
            return ref;
        }
    }

    @XmlElementHandler("SPEC-RELATION-TYPE-REF")
    public static class SpecRelationTypeRef extends Ref<SpecRelationType> {

    }

    @XmlElementHandler("ENUM-VALUE")
    public static class EnumValue extends Reffable {

        @Override
        public void end(XmlElement parent) {
            super.end(parent);
            document.declare(this);
        }
    }

    @XmlElementHandler("SPEC-ATTRIBUTES")
    public static class SpecAttributes extends XmlElement {

        private List<AttributeDefinition> definitions = Lists.newArrayList();

        @Override
        public void addChild(XmlElement child) {
            if (child instanceof AttributeDefinition) {
                definitions.add((AttributeDefinition) child);
            }
        }
    }

    /** Handler for all elements of the XHTML specification: DIV, P, IMG, etc. */
    @XmlElementHandler(namespace = "http://www.w3.org/1999/xhtml")
    public static class XhtmlElement extends XmlElement {

        @Override
        public void start(String uri, String localName, Attributes attributes) {
            sb.append("<");
            sb.append(this.localName);

            for (int i = 0 ; i < attributes.getLength() ; i++) {
                String attributeName = attributes.getLocalName(i);
                if (!isXhtmlName(attributes.getURI(i))) {
                    attributeName = "data-" + attributeName;
                }
                sb.append(" ");
                sb.append(attributeName).append("=\"");
                sb.append(escapeXmlAttributeValue(attributes.getValue(i))).append("\"");
            }
            sb.append(">");
        }

        @Override
        public void addChild(XmlElement child) {
            if (child instanceof XhtmlElement) {
                sb.append(child.sb);
            }
        }

        private boolean isXhtmlName(String uri) {
            if (StringUtils.isBlank(uri)) return true;
            if (Objects.equals(uri, "http://www.w3.org/1999/xhtml")) return true;
            return false;
        }

        @Override
        public void end(XmlElement parent) {
            super.end(parent);
            sb.append("</").append(localName).append(">");
        }
    }
}

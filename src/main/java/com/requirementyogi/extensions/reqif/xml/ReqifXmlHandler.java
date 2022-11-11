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

import com.atlassian.confluence.content.render.xhtml.RenderedContentCleaner;
import com.google.common.collect.Lists;
import com.requirementyogi.extensions.reqif.managers.ReqifDocumentManager;
import com.requirementyogi.extensions.reqif.ui.UIReqifDocument;
import com.requirementyogi.extensions.reqif.xml.ReqifXmlElements.Unknown;
import com.requirementyogi.extensions.reqif.xml.ReqifXmlElements.XmlElement;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.bind.annotation.XmlAttribute;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * This class is a ReqIF parser, inspired by JAXB, which uses mappings defined in the inner classes of {@link ReqifXmlElements}.
 * <p/>
 * If you're looking for a parser, look into {@link ReqifDocumentManager}.
 * <p/>
 * If you're looking for the mappings, look into {@link ReqifXmlElements}.
 */
public class ReqifXmlHandler extends DefaultHandler {

    private ReqifXmlDocument document = new ReqifXmlDocument();
    private LinkedList<XmlElement> stack = Lists.newLinkedList();

    /** Transforms the namespace of the reqif elements, to ensure compatibility for various ReqIF spec versions */
    private static String getNamespace(String namespace) {
        if (namespace == null) return null;
        // We take all namespaces, since there could be XSDs that we're not yet aware of:
        // http://www.omg.org/spec/ReqIF/20110401/reqif.xsd
        // http://www.omg.org/spec/ReqIF/20110402/driver.xsd
        if (namespace.startsWith("http://www.omg.org/spec/ReqIF/")) return "reqif";
        return namespace;
    }

    /**
     * This method triggers when we start a new tag
     * @param qName contains the label of the current tag
     * @param attributes contains the list of its attributes
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        Class<? extends XmlElement> elementTypeClass = getMatchingXmlHandler(uri, localName);
        if (elementTypeClass == null) {
            elementTypeClass = Unknown.class;
        }

        XmlElement parent = stack.isEmpty() ? null : stack.getLast();
        XmlElement element = buildElement(elementTypeClass, localName, attributes, parent);
        stack.add(element);
        element.start(uri, localName, attributes);
    }

    /**
     * Find a class of {@link ReqifXmlElements} which has the annotation {@link XmlElementHandler}, which
     * properties (namespace and name) match the uri (or "reqif") and localname.
     *
     * @param uri the uri of the namespace. Will be replaced by the result of {@link #getNamespace(String)}.
     *            In {@code <xhtml:div ...>}, it is the URI at the top of the file used for "xhtml".
     * @param localName the name of the XML element. In {@code <xhtml:div ...>}, it is "div".
     * @return a class extending XmlElement.
     */
    private static Class<? extends XmlElement> getMatchingXmlHandler(String uri, String localName) {
        Class<? extends XmlElement> elementTypeClass = null;
        for (Class<?> clazz : ReqifXmlElements.class.getDeclaredClasses()) {
            if (XmlElement.class.isAssignableFrom(clazz)) {
                XmlElementHandler annotation = clazz.getAnnotation(XmlElementHandler.class);
                if (annotation != null) {

                    // Check the namespace
                    if (!annotation.namespace().equals("")) {
                        if (!Objects.equals(getNamespace(uri), annotation.namespace())) {
                            continue;
                        }
                    }

                    // Check the qNames
                    List<String> names = Lists.newArrayList(annotation.names());
                    names.add(annotation.value());
                    names.remove("");
                    if (names.size() > 0 && !names.contains(localName)) {
                        continue;
                    }

                    // It's a match
                    elementTypeClass = (Class<XmlElement>) clazz;
                    break;
                }
            }
        }
        return elementTypeClass;
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        stack.getLast().append(" ");
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        String value = String.copyValueOf(ch, start, length);
        // TODO Is it raw XML? Is it rendered as ASCII? Does copyValueOf encode in UTF-8?
        stack.getLast().append(value);
    }

    /**
     * This method triggers when we reach the end of a tag
     * @param qName contains the tag label
     */
    @Override
    public void endElement(String uri, String localName, String qName) {
        XmlElement element = stack.removeLast();
        XmlElement parent = stack.peekLast();
        if (element instanceof Unknown) {
            String closingTag = "</" + (StringUtils.isNotBlank(localName) ? localName + ":" : "") + ">";
            element.append(closingTag);
        }

        element.end(parent);
        if (parent != null) {
            parent.addChild(element);
        }
    }

    /**
     * Return a bean which exposes the contents of the document without exposing its technical details.
     */
    public UIReqifDocument getReqifDocument(RenderedContentCleaner antisamy) {
        return new UIReqifDocument(document, antisamy);
    }

    private <T extends XmlElement> T buildElement(Class<T> clazz, String localName, Attributes attributes, XmlElement parent) {
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length != 1) throw new RuntimeException("Exactly 1 constructor is required in " + clazz.getCanonicalName());
        Constructor<?> constructor = constructors[0];

        T object;
        try {
            object = (T) constructor.newInstance(null);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Couldn't call " + clazz.getCanonicalName() + ".new()");
        }

        Class<?> clazz1 = clazz;
        while (clazz1 != null) {
            for (Field field : clazz1.getDeclaredFields()) {
                XmlAttribute annotation = field.getAnnotation(XmlAttribute.class);
                if (annotation != null) {
                    String name = annotation.name();
                    Object value = null;
                    if (Objects.equals(name, "#localName")) {
                        value = localName;
                    } else if (Objects.equals(name, "#parent")) {
                        value = parent;
                    } else if (Objects.equals(name, "#document")) {
                        value = document;
                    } else if (name.startsWith("#")) {
                        throw new RuntimeException("Unknown binding: " + name + " for " + clazz.getCanonicalName() + "#" + field.getName());
                    } else {
                        value = attributes.getValue(name);
                    }
                    try {
                        field.setAccessible(true);
                        field.set(object, value);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Couldn't assign value to " + clazz.getCanonicalName() + "#" + field.getName());
                    }
                }
            }
            clazz1 = clazz1.getSuperclass();
        }
        return object;
    }
}

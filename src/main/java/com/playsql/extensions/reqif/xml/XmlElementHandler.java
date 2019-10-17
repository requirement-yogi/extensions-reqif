package com.playsql.extensions.reqif.xml;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({TYPE})
public @interface XmlElementHandler {

    /**
     * namespace name of the XML element to be parsed.
     * <p>
     * If the value is "", then it matches any namespace
     */
    String namespace() default "";

    /**
     * Same as value, but plural. local name of the XML element.
     * <p>
     * If neither names nor value has any value, then all names are matched.
     */
    String[] names() default {};

    /**
     * local name of the XML element.
     * <p>
     * If neither names nor value has any value, then all names are matched.
     */
    String value() default "";
}

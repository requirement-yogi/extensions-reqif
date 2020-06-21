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

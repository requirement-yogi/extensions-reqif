package com.playsql.extensions.reqif.ui;

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

import java.util.Objects;

public class UIRequirementValue {
    private String key;
    private String value;
    // XHTML or null
    private String type;

    public UIRequirementValue(String key, String value, boolean xhtml) {
        this.key = key;
        this.value = value;
        this.type = xhtml ? "XHTML" : null;
    }

    public boolean isXhtml() {
        return Objects.equals(type, "XHTML");
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}

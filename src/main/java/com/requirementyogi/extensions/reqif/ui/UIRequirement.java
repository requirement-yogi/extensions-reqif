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

import com.google.common.collect.Lists;
import com.playsql.requirementyogi.api.beans.Requirement;
import com.requirementyogi.extensions.reqif.xml.ReqifConfig;
import com.requirementyogi.extensions.reqif.xml.ReqifXmlElements.SpecObject;

import java.util.List;
import java.util.Objects;

/**
 * An object representing a {@link SpecObject}, but with nicer methods and without the XML details of implementation.
 * Ready to be used in a .vm file.
 *
 * <p/>
 *
 * It is entirely different from a {@link Requirement} because it contains the raw attributes of the Reqif document,
 * before import.
 *
 * <p/>
 *
 * It can be converted into a {@link Requirement} by applying the mappings defined in {@link ReqifConfig}. That is
 * what {@link UIReqifDocument#importRequirements} does.
 */
public class UIRequirement {
    private List<UIRequirementValue> values = Lists.newArrayList();
    private String identifier;
    private boolean focus;

    public UIRequirement(String identifier) {
        this.identifier = identifier;
        values.add(new UIRequirementValue("#IDENTIFIER", identifier, false));
    }

    public UIRequirementValue get(String id) {
        UIRequirementValue value = getInternal(id);
        if (value != null) {
            return value;
        } else {
            // This is to please our Velocity templates
            return new UIRequirementValue(id, null, false);
        }
    }

    private UIRequirementValue getInternal(String id) {
        for(UIRequirementValue value : values)
            if (Objects.equals(value.getKey(),id))
            return value;
        return null;
    }

    public void setTypeId(String typeId) {
        put(new UIRequirementValue("#TYPEID", typeId, false));
    }

    public void setTypeName(String typeName) {
        put(new UIRequirementValue("#TYPE", typeName, false));
    }

    public void setDescription(String description) {
        put(new UIRequirementValue("#DESCRIPTION", description, false));
    }

    public void setLastChange(String lastChange) {
        put(new UIRequirementValue("#LASTCHANGE", lastChange, false));
    }

    public void put(UIRequirementValue value) {
        UIRequirementValue existing = getInternal(value.getKey());
        if (existing != null) {
            values.remove(existing);
        }
        values.add(value);
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean isFocus() {
        return focus;
    }

    public void setFocus(boolean focus) {
        this.focus = focus;
    }
}

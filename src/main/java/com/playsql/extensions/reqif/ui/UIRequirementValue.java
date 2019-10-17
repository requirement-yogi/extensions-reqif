package com.playsql.extensions.reqif.ui;

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

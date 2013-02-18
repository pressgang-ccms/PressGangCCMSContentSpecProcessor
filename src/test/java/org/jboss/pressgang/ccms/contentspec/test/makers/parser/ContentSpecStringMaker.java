package org.jboss.pressgang.ccms.contentspec.test.makers.parser;

import static com.natpryce.makeiteasy.Property.newProperty;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;

public class ContentSpecStringMaker {

    public static final Property<String, String> title = newProperty();
    public static final Property<String, String> product = newProperty();
    public static final Property<String, String> version = newProperty();
    public static final Property<String, String> edition = newProperty();
    public static final Property<String, String> globalOptions = newProperty();
    public static final Property<String, String> publicanCfg = newProperty();
    public static final Property<String, String> injectionOptions = newProperty();

    public static final Instantiator<String> ContentSpecString = new Instantiator<String>() {
        @Override
        public String instantiate(PropertyLookup<String> lookup) {
            final String nullString = null;
            final StringBuilder retValue = new StringBuilder().append("Title = ").append(lookup.valueOf(title, "Test")).append("\n");
            if (lookup.valueOf(product, nullString) != null) {
                retValue.append("Product = ").append(lookup.valueOf(product, "")).append("\n");
            }
            if (lookup.valueOf(version, nullString) != null) {
                retValue.append("Version = ").append(lookup.valueOf(version, "")).append("\n");
            }
            if (lookup.valueOf(edition, nullString) != null) {
                retValue.append("Edition = ").append(lookup.valueOf(edition, "")).append("\n");
            }
            if (lookup.valueOf(injectionOptions, nullString) != null) {
                retValue.append("Inline Injection = ").append(lookup.valueOf(injectionOptions, "")).append("\n");
            }
            if (lookup.valueOf(publicanCfg, nullString) != null) {
                retValue.append("publican.cfg = [").append(lookup.valueOf(publicanCfg, "")).append("]").append("\n");
            }
            if (lookup.valueOf(globalOptions, nullString) != null) {
                retValue.append("[").append(lookup.valueOf(globalOptions, "")).append("]").append("\n");
            }

            return retValue.toString();
        }
    };
}

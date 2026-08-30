package com.classdiagrammer.infrastructure.parsing.xforms;

import java.util.ArrayList;
import com.classdiagrammer.infrastructure.xml.XmlTagScanner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for XForms documents.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
final class FormModelCollector {

    static final String XFORMS_NAMESPACE = "http://www.w3.org/2002/xforms";

    private final Map<String, String> namespaceBindings = new HashMap<>();
    private final List<String> models = new ArrayList<>();
    private final List<String> binds = new ArrayList<>();
    private final List<String> submissions = new ArrayList<>();
    private final List<String> instanceSources = new ArrayList<>();
    private boolean xformsDocument;

    void accept(XmlTagScanner.Tag tag) {
        rememberNamespaceBindings(tag);
        String prefix = XmlTagScanner.prefixOf(tag.name);
        if (!XFORMS_NAMESPACE.equals(namespaceBindings.get(prefix))) {
            return;
        }
        xformsDocument = true;
        String localName = XmlTagScanner.localNameOf(tag.name);
        if ("model".equals(localName)) {
            addUnique(models, tag.attributes.get("id"));
        } else if ("bind".equals(localName)) {
            addUnique(binds, firstNonEmpty(
                    tag.attributes.get("nodeset"), tag.attributes.get("ref")));
        } else if ("submission".equals(localName)) {
            addUnique(submissions, firstNonEmpty(
                    tag.attributes.get("action"),
                    tag.attributes.get("resource"),
                    tag.attributes.get("id")));
        } else if ("instance".equals(localName)) {
            addUnique(instanceSources, tag.attributes.get("src"));
        }
    }

    boolean isXFormsDocument() {
        return xformsDocument;
    }

    boolean declaresXFormsNamespace() {
        return namespaceBindings.containsValue(XFORMS_NAMESPACE);
    }

    List<String> models() {
        return models;
    }

    List<String> binds() {
        return binds;
    }

    List<String> submissions() {
        return submissions;
    }

    List<String> instanceSources() {
        return instanceSources;
    }

    private void rememberNamespaceBindings(XmlTagScanner.Tag tag) {
        for (Map.Entry<String, String> attribute : tag.attributes.entrySet()) {
            String name = attribute.getKey();
            if (name.equals("xmlns")) {
                namespaceBindings.put("", attribute.getValue());
            } else if (name.startsWith("xmlns:")) {
                namespaceBindings.put(name.substring("xmlns:".length()),
                        attribute.getValue());
            }
        }
    }

    private static void addUnique(List<String> into, String value) {
        if (value != null && !value.trim().isEmpty() && !into.contains(value.trim())) {
            into.add(value.trim());
        }
    }

    private static String firstNonEmpty(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.trim().isEmpty()) {
                return candidate.trim();
            }
        }
        return "";
    }
}

package com.classdiagrammer.tests.adapter;

import com.classdiagrammer.application.port.out.ArtifactParser;
import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.infrastructure.parsing.xforms.XFormsArtifactParser;
import com.classdiagrammer.tests.support.TestHarness;

public final class XFormsDocumentBehavior {

    private static final String OPEN =
            "<html xmlns:xf=\"http://www.w3.org/2002/xforms\" xmlns:h=\"http://www.w3.org/1999/xhtml\">\n";

    private XFormsDocumentBehavior() {
    }

    public static void verify(TestHarness h) {
        final ArtifactParser parser = new XFormsArtifactParser();
        h.scope("adapter/documentos-xforms");

        h.expect("xhtml with xforms namespace is accepted and one without is not", () -> {
            String conXforms = OPEN + "<xf:model id=\"m1\"/>\n</html>\n";
            String sinXforms = "<html xmlns:h=\"http://www.w3.org/1999/xhtml\">"
                    + "<h:p>texto</h:p></html>\n";
            return parser.accepts(file("forms/a.xhtml", conXforms))
                    && !parser.accepts(file("forms/b.xhtml", sinXforms))
                    && !parser.accepts(file("forms/c.txt", conXforms));
        });

        h.expect("models, binds y submissions se describen como miembros", () -> {
            String content = OPEN
                    + "<xf:model id=\"principal\">\n"
                    + "  <xf:bind nodeset=\"/persona/nombre\" required=\"true()\"/>\n"
                    + "  <xf:bind ref=\"/persona/edad\"/>\n"
                    + "  <xf:submission action=\"guardar.php\" method=\"post\"/>\n"
                    + "</xf:model>\n</html>\n";
            TypeNode node = only(parser.parse(file("forms/persona.xhtml", content)));
            boolean modelo = false;
            boolean envio = false;
            for (com.classdiagrammer.domain.model.Method m : node.methods()) {
                if (m.name().equals("model:principal")) {
                    modelo = true;
                }
                if (m.name().equals("submission:guardar.php")) {
                    envio = true;
                }
            }
            return modelo && envio
                    && node.fields().size() == 2
                    && node.fields().get(0).name().equals("/persona/nombre")
                    && node.fields().get(1).name().equals("/persona/edad");
        });

        h.expect("greater-than inside attribute value does not break scanning", () -> {
            String content = OPEN
                    + "<xf:model id=\"raro\">\n"
                    + "  <xf:bind nodeset=\"/x[y > 2]\" />\n"
                    + "</xf:model>\n</html>\n";
            TypeNode node = only(parser.parse(file("forms/raro.xhtml", content)));
            return node.fields().size() == 1
                    && node.fields().get(0).name().equals("/x[y > 2]");
        });

        h.expect("comentarios xml y cdata no producen miembros fantasma", () -> {
            String content = OPEN
                    + "<!-- <xf:bind nodeset=\"/oculto\"/> -->\n"
                    + "<script><![CDATA[ <xf:bind nodeset=\"/cdata\"/> ]]></script>\n"
                    + "<xf:bind nodeset=\"/visible\"/>\n"
                    + "</html>\n";
            TypeNode node = only(parser.parse(file("forms/comentarios.xhtml", content)));
            return node.fields().size() == 1
                    && node.fields().get(0).name().equals("/visible");
        });

        h.expect("only instances pointing to documents generate references", () -> {
            String content = OPEN
                    + "<xf:model>\n"
                    + "  <xf:instance src=\"datos-persona.xml\"/>\n"
                    + "  <xf:instance src=\"subformulario.xhtml\"/>\n"
                    + "</xf:model>\n</html>\n";
            TypeNode node = only(parser.parse(file("forms/refs.xhtml", content)));
            return node.imports().size() == 1
                    && node.imports().contains("subformulario.xhtml");
        });
    }

    private static TypeNode only(java.util.List<TypeNode> nodes) {
        if (nodes.size() != 1) {
            throw new AssertionError("se esperaba un unico nodo, hubo " + nodes.size());
        }
        return nodes.get(0);
    }

    private static SourceFile file(String relativePath, String content) {
        String folder = relativePath.contains("/")
                ? relativePath.substring(0, relativePath.lastIndexOf('/'))
                : "";
        return new SourceFile(folder, relativePath, content);
    }
}

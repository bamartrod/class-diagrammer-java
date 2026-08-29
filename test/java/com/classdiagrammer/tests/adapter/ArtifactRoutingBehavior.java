package com.classdiagrammer.tests.adapter;

import com.classdiagrammer.application.port.out.ArtifactParser;
import com.classdiagrammer.domain.model.Edge;
import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.domain.model.TypeRelationKind;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.domain.resolution.EdgeResolver;
import com.classdiagrammer.infrastructure.parsing.CompositeArtifactParser;
import com.classdiagrammer.infrastructure.parsing.java.JavaArtifactParser;
import com.classdiagrammer.infrastructure.parsing.velocity.VelocityArtifactParser;
import com.classdiagrammer.infrastructure.parsing.xforms.XFormsArtifactParser;
import com.classdiagrammer.tests.support.TestHarness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ArtifactRoutingBehavior {

    private ArtifactRoutingBehavior() {
    }

    public static void verify(TestHarness h) {
        h.scope("adapter/enrutado-de-parsers");

        h.expect("cada artefacto viaja hacia su parser por extension y contenido", () -> {
            ArtifactParser composite = composite();
            return composite.accepts(file("src/A.java", "class A {}"))
                    && composite.accepts(file("views/b.vm", "$x"))
                    && composite.accepts(file("forms/c.xhtml",
                            "<html xmlns:xf=\"http://www.w3.org/2002/xforms\"/>"))
                    && !composite.accepts(file("docs/readme.md", "# titulo"));
        });

        h.expect("un compuesto sin parsers se rechaza", () -> {
            try {
                new CompositeArtifactParser(new ArrayList<ArtifactParser>());
                return false;
            } catch (IllegalArgumentException expected) {
                return true;
            }
        });

        h.expect("un proyecto mixto produce un grafo combinado con aristas incluidas", () -> {
            List<TypeNode> nodes = new ArrayList<>();
            List<SourceFile> files = Arrays.asList(
                    file("src/app/Servicio.java",
                            "package app;\npublic class Servicio {}\n"),
                    file("views/pagina.vm",
                            "#parse(\"views/comun.vm\")\n#macro(dibuja $x)\n#end\n"),
                    file("views/comun.vm", "#macro(apoya $y)\n#end\n"),
                    file("forms/ficha.xhtml",
                            "<html xmlns:xf=\"http://www.w3.org/2002/xforms\">"
                                    + "<xf:model id=\"m\"/></html>\n"));
            for (SourceFile f : files) {
                nodes.addAll(composite().parse(f));
            }
            List<Edge> edges = new EdgeResolver().resolve(
                    com.classdiagrammer.domain.model.CodeGraph.of(nodes));
            boolean tieneJava = hasNode(nodes, "app.Servicio");
            boolean tienePlantillas = hasNode(nodes, "views/pagina.vm")
                    && hasNode(nodes, "views/comun.vm");
            boolean tieneFormulario = hasNode(nodes, "forms/ficha.xhtml");
            boolean aristaIncluida = false;
            for (Edge e : edges) {
                if (e.kind() == TypeRelationKind.IMPORTS
                        && e.from().equals("views/pagina.vm")
                        && e.to().equals("views/comun.vm")
                        && e.isResolved()) {
                    aristaIncluida = true;
                }
            }
            return nodes.size() == 4 && tieneJava && tienePlantillas
                    && tieneFormulario && aristaIncluida;
        });
    }

    private static ArtifactParser composite() {
        return new CompositeArtifactParser(Arrays.asList(
                new JavaArtifactParser(),
                new VelocityArtifactParser(),
                new XFormsArtifactParser()));
    }

    private static boolean hasNode(List<TypeNode> nodes, String qualifiedName) {
        for (TypeNode node : nodes) {
            if (node.qualifiedName().equals(qualifiedName)) {
                return true;
            }
        }
        return false;
    }

    private static SourceFile file(String relativePath, String content) {
        String folder = relativePath.contains("/")
                ? relativePath.substring(0, relativePath.lastIndexOf('/'))
                : "";
        return new SourceFile(folder, relativePath, content);
    }
}

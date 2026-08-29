package com.classdiagrammer.tests.adapter;

import com.classdiagrammer.application.port.out.ArtifactParser;
import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.infrastructure.parsing.velocity.VelocityArtifactParser;
import com.classdiagrammer.tests.support.TestHarness;

public final class VelocityTemplateBehavior {

    private VelocityTemplateBehavior() {
    }

    public static void verify(TestHarness h) {
        final ArtifactParser parser = new VelocityArtifactParser();
        h.scope("adapter/plantillas-velocity");

        h.expect("solo las plantillas velocity son aceptadas", () ->
                parser.accepts(file("layout/base.vm", "#macro(a $b)\n#end"))
                        && parser.accepts(file("layout/otro.vtl", "$x"))
                        && !parser.accepts(file("layout/base.txt", "texto"))
                        && !parser.accepts(null));

        h.expect("un macro se describe como metodo publico con sus parametros", () -> {
            TypeNode node = only(parser.parse(file(
                    "views/list.vm",
                    "#macro(renderRow $item $style)\n"
                            + "  <tr class='$style'>$item</tr>\n"
                            + "#end\n")));
            return node.kind().jsonName().equals("template")
                    && node.methods().size() == 1
                    && node.methods().get(0).name().equals("renderRow")
                    && node.methods().get(0).parameters().size() == 2
                    && node.methods().get(0).parameters().get(1).name().equals("style");
        });

        h.expect("los set globales se vuelven campos y los internos no", () -> {
            TypeNode node = only(parser.parse(file(
                    "views/page.vm",
                    "#set($titulo = 'hola')\n"
                            + "#macro(dentro $x)\n"
                            + "  #set($local = $x)\n"
                            + "#end\n")));
            return node.fields().size() == 1
                    && node.fields().get(0).name().equals("titulo")
                    && node.methods().get(0).name().equals("dentro");
        });

        h.expect("parse e include dejan referencias para aristas entre plantillas", () -> {
            TypeNode node = only(parser.parse(file(
                    "views/a.vm",
                    "#parse(\"views/b.vm\")\n#include('header.vm')\n")));
            return node.imports().contains("views/b.vm")
                    && node.imports().contains("header.vm");
        });

        h.expect("los comentarios de linea y bloque no producen directivas fantasma", () -> {
            TypeNode node = only(parser.parse(file(
                    "views/c.vm",
                    "## #macro(falso $x)\n"
                            + "#* #set($fantasma = 1) *#\n"
                            + "#macro(real $y)\n#end\n")));
            return node.methods().size() == 1
                    && node.methods().get(0).name().equals("real")
                    && node.fields().isEmpty();
        });

        h.expect("condicionales y texto libre con mayor-que no rompen el escaneo", () -> {
            TypeNode node = only(parser.parse(file(
                    "views/d.vm",
                    "#if($a > $b)\n"
                            + "  <p>$a &gt; $b</p>\n"
                            + "#end\n"
                            + "#set($msg = \"dijo: #macro(trampa)\")\n"
                            + "#macro(fin $z)\n#end\n")));
            boolean trampaIgnorada = true;
            for (com.classdiagrammer.domain.model.Method m : node.methods()) {
                if (m.name().equals("trampa")) {
                    trampaIgnorada = false;
                }
            }
            return trampaIgnorada && node.methods().size() == 1
                    && node.methods().get(0).name().equals("fin");
        });

        h.expect("dos macros homonimos no duplican el metodo", () -> {
            TypeNode node = only(parser.parse(file(
                    "views/e.vm",
                    "#macro(x $a)\n#end\n#if(true)#macro(x $a)#end#end\n")));
            return node.methods().size() == 1;
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

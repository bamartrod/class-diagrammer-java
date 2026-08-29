package com.classdiagrammer.tests.unit;

import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.infrastructure.parsing.JavaParserFactory;
import com.classdiagrammer.infrastructure.parsing.JavaVersion;
import com.classdiagrammer.infrastructure.parsing.java.JavaArtifactParser;
import com.classdiagrammer.infrastructure.parsing.java11.Java11ArtifactParser;
import com.classdiagrammer.infrastructure.parsing.java17.Java17ArtifactParser;
import com.classdiagrammer.infrastructure.parsing.java21.Java21ArtifactParser;
import com.classdiagrammer.infrastructure.parsing.java26.Java26ArtifactParser;
import com.classdiagrammer.interfaces.cli.CliArgs;
import com.classdiagrammer.tests.support.TestHarness;

import java.util.List;

public final class JavaVersionBehavior {

    private JavaVersionBehavior() {
    }

    public static void verify(TestHarness h) {
        h.scope("unit/versiones-java");

        h.expect("el parser base acepta fuentes java", () -> {
            SourceFile file = new SourceFile("", "A.java", "class A {}");
            return new JavaArtifactParser().accepts(file)
                    && new Java11ArtifactParser().accepts(file)
                    && new Java17ArtifactParser().accepts(file)
                    && new Java21ArtifactParser().accepts(file)
                    && new Java26ArtifactParser().accepts(file);
        });

        h.expect("la factoria elige el parser segun la version solicitada", () -> {
            return JavaParserFactory.forVersion(JavaVersion.from("8")) instanceof JavaArtifactParser
                    && JavaParserFactory.forVersion(JavaVersion.from("11")) instanceof Java11ArtifactParser
                    && JavaParserFactory.forVersion(JavaVersion.from("17")) instanceof Java17ArtifactParser
                    && JavaParserFactory.forVersion(JavaVersion.from("21")) instanceof Java21ArtifactParser
                    && JavaParserFactory.forVersion(JavaVersion.from("26")) instanceof Java26ArtifactParser;
        });

        h.expect("java 11 entiende var en cuerpo de metodo", () -> {
            SourceFile file = new SourceFile("app", "app/Serv.java",
                    "package app; class Serv { void m() { var x = 1; } }");
            List<TypeNode> nodes = new Java11ArtifactParser().parse(file);
            return nodes.size() == 1 && nodes.get(0).simpleName().equals("Serv");
        });

        h.expect("java 17 tolera text blocks sin romper llaves", () -> {
            SourceFile file = new SourceFile("app", "app/T.java",
                    "package app; class T { String s = \"\"\"\n"
                            + "  linea 1\n"
                            + "  { falso }\n"
                            + "  \"\"\"; }");
            List<TypeNode> nodes = new Java17ArtifactParser().parse(file);
            return nodes.size() == 1;
        });

        h.expect("java 17 expone permits como relacion propia y conserva sealed", () -> {
            SourceFile file = new SourceFile("app", "app/Shape.java",
                    "package app; public sealed class Shape permits Circle, Square {} "
                            + "final class Circle extends Shape {} "
                            + "final class Square extends Shape {}");
            List<TypeNode> nodes = new Java17ArtifactParser().parse(file);
            var shape = nodes.stream().filter(n -> n.simpleName().equals("Shape")).findFirst().orElse(null);
            return nodes.size() == 3
                    && shape != null
                    && shape.permitsTypes().contains("Circle")
                    && shape.permitsTypes().contains("Square")
                    && shape.extendsTypes().isEmpty()
                    && shape.modifiers().contains("sealed");
        });

        h.expect("java 21 y 26 heredan el soporte de 17 sin regresiones", () -> {
            SourceFile file = new SourceFile("app", "app/R.java",
                    "package app; record R(int x, int y) {}");
            return new Java21ArtifactParser().parse(file).size() == 1
                    && new Java26ArtifactParser().parse(file).size() == 1;
        });

        h.expect("el flag --java rechaza versiones no soportadas", () -> {
            try {
                CliArgs.parse(new String[]{"src", "--java", "9"});
                return false;
            } catch (IllegalArgumentException expected) {
                return expected.getMessage().contains("no soportada");
            }
        });

        h.expect("el flag --java por defecto es 8 y acepta 8/11/17/21/26", () -> {
            return CliArgs.parse(new String[]{"src"}).javaVersion().equals("8")
                    && CliArgs.parse(new String[]{"src", "--java", "17"}).javaVersion().equals("17")
                    && CliArgs.parse(new String[]{"src", "--java", "26"}).javaVersion().equals("26");
        });
    }
}

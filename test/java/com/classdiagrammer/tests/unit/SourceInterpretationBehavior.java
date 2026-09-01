package com.classdiagrammer.tests.unit;

import com.classdiagrammer.domain.model.Field;
import com.classdiagrammer.domain.model.Method;
import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.domain.model.TypeKind;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.domain.model.Visibility;
import com.classdiagrammer.infrastructure.parsing.java.JavaArtifactParser;
import com.classdiagrammer.tests.support.Sources;
import com.classdiagrammer.tests.support.TestHarness;

import java.util.List;

/**
 * Behavior verification suite for SourceInterpretationBehavior.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class SourceInterpretationBehavior {

    private SourceInterpretationBehavior() {
    }

    public static void verify(TestHarness h) {
        JavaArtifactParser parser = new JavaArtifactParser();

        h.scope("unit/interpretacion-de-fuentes");
        h.expect("paquete e imports quedan registrados", () -> {
            List<TypeNode> nodes = parser.parse(userServiceSource());
            TypeNode type = only(nodes);
            // class-level imports now only for hierarchy (extends/implements/permits), per-member for fields/methods
            return "com.demo".equals(type.packageName())
                    && type.imports().contains("com.demo.Base")
                    && !type.imports().contains("java.util.List")
                    && type.imports().size() == 1;
        });
        h.expect("folder and file accompany the type", () -> {
            TypeNode type = only(parser.parse(userServiceSource()));
            return "src/com/demo".equals(type.folder())
                    && type.file().endsWith("UserService.java");
        });
        h.expect("los imports requeridos por campo se resuelven por uso", () -> {
            List<TypeNode> nodes = parser.parse(Sources.java("shop/Order.java",
                    "package shop;",
                    "import com.shop.domain.Base;",
                    "import com.shop.domain.Validable;",
                    "import com.shop.domain.Cliente;",
                    "import com.shop.domain.PedidoExpress;",
                    "public class Order extends Base implements Validable {",
                    "    private Cliente cliente;",
                    "    private PedidoExpress express;",
                    "    private int cantidad;",
                    "    public Order(Cliente c) {}",
                    "    public void process(Cliente c, PedidoExpress p) {}",
                    "}"));
            TypeNode order = only(nodes);
            // class-level only hierarchy (Base, Validable)
            boolean classFiltered = order.imports().size() == 2
                    && order.imports().contains("com.shop.domain.Base")
                    && order.imports().contains("com.shop.domain.Validable")
                    && !order.imports().contains("com.shop.domain.Cliente");
            Field clienteField = findField(order, "cliente");
            Field expressField = findField(order, "express");
            Field cantField = findField(order, "cantidad");
            boolean fieldCliente = clienteField != null && clienteField.requiredImports().contains("com.shop.domain.Cliente");
            boolean fieldExpress = expressField != null && expressField.requiredImports().contains("com.shop.domain.PedidoExpress");
            boolean fieldCantEmpty = cantField != null && cantField.requiredImports().isEmpty();
            Method ctor = order.constructors().stream().filter(m -> m.name().equals("Order")).findFirst().orElse(null);
            boolean ctorImports = ctor != null && ctor.requiredImports().contains("com.shop.domain.Cliente");
            Method proc = findMethod(order, "process");
            boolean methodImports = proc != null && proc.requiredImports().contains("com.shop.domain.Cliente")
                    && proc.requiredImports().contains("com.shop.domain.PedidoExpress");
            return classFiltered && fieldCliente && fieldExpress && fieldCantEmpty && ctorImports && methodImports;
        });
        h.expect("una clase publica abstracta expone naturaleza y visibilidad", () -> {
            TypeNode type = only(parser.parse(userServiceSource()));
            return type.kind() == TypeKind.CLASS
                    && type.visibility() == Visibility.PUBLIC
                    && type.modifiers().contains("abstract");
        });
        h.expect("inheritance and implemented contracts are read from header", () -> {
            TypeNode type = only(parser.parse(userServiceSource()));
            return type.extendsTypes().contains("Base")
                    && type.implementsTypes().contains("Named")
                    && type.implementsTypes().contains("java.io.Serializable");
        });
        h.expect("los campos conservan tipo y visibilidad sin confundirse con metodos", () -> {
            TypeNode type = only(parser.parse(userServiceSource()));
            return hasField(type, "CACHE", "List<String>", Visibility.PRIVATE)
                    && hasField(type, "seed", "int", Visibility.PROTECTED)
                    && type.fields().size() == 2;
        });
        h.expect("los constructores se separan por firma", () -> {
            TypeNode type = only(parser.parse(userServiceSource()));
            return type.constructors().size() == 2
                    && allNamed(type.constructors(), "UserService")
                    && anyConstructorWithParams(type, "seed", "history");
        });
        h.expect("los metodos conservan retorno, visibilidad y parametros genericos", () -> {
            TypeNode type = only(parser.parse(userServiceSource()));
            Method grouped = findMethod(type, "grouped");
            return type.methods().size() == 3
                    && grouped != null
                    && "Map<String, List<Integer>>".equals(grouped.returnType())
                    && grouped.visibility() == Visibility.PUBLIC
                    && grouped.parameters().size() == 1
                    && "items".equals(grouped.parameters().get(0).name())
                    && "List<Integer>".equals(grouped.parameters().get(0).type());
        });
        h.expect("foreign bodies do not contaminate member reading", () -> {
            TypeNode type = only(parser.parse(userServiceSource()));
            return findMethod(type, "audit") != null
                    && findField(type, "r") == null
                    && findField(type, "Fake") == null;
        });

        h.expect("las interfaces promueven visibilidad publica implicita", () -> {
            List<TypeNode> nodes = parser.parse(Sources.java("api/Named.java",
                    "package api;",
                    "public interface Named {",
                    "    int MAX = 5;",
                    "    String name();",
                    "    default String greet() { return \"hi\"; }",
                    "}"));
            TypeNode named = only(nodes);
            return named.kind() == TypeKind.INTERFACE
                    && hasField(named, "MAX", "int", Visibility.PUBLIC)
                    && findMethod(named, "name").visibility() == Visibility.PUBLIC
                    && findMethod(named, "greet").modifiers().contains("default");
        });
        h.expect("un enum distingue constantes de miembros reales", () -> {
            List<TypeNode> nodes = parser.parse(Sources.java("demo/Color.java",
                    "package demo;",
                    "public enum Color {",
                    "    RED(1), GREEN(2), BLUE;",
                    "",
                    "    private int code;",
                    "",
                    "    Color() { this(0); }",
                    "",
                    "    Color(int code) { this.code = code; }",
                    "",
                    "    public int code() { return code; }",
                    "}"));
            TypeNode color = only(nodes);
            return color.kind() == TypeKind.ENUM
                    && color.fields().size() == 1
                    && findField(color, "code") != null
                    && color.constructors().size() == 2
                    && color.methods().size() == 1;
        });
        h.expect("nested types qualify their full name", () -> {
            List<TypeNode> nodes = parser.parse(Sources.java("Outer.java",
                    "class Outer {",
                    "    class Inner { void ping() { } }",
                    "    static class SInner extends Outer { }",
                    "}"));
            boolean outer = containsType(nodes, "Outer");
            boolean inner = false;
            boolean nestedStatic = false;
            for (TypeNode node : nodes) {
                if ("Outer.Inner".equals(node.qualifiedName()) && node.methods().size() == 1) {
                    inner = true;
                }
                if ("Outer.SInner".equals(node.qualifiedName())
                        && node.modifiers().contains("static")) {
                    nestedStatic = true;
                }
            }
            return outer && inner && nestedStatic && nodes.size() == 3;
        });
        h.expect("a type in default package omits package segment", () -> {
            TypeNode bare = only(parser.parse(Sources.java("Bare.java",
                    "public class Bare { public void hi() { } }")));
            return "Bare".equals(bare.qualifiedName()) && "".equals(bare.packageName());
        });
        h.expect("comentarios y literales con llaves no enganan al lector", () -> {
            List<TypeNode> nodes = parser.parse(Sources.java("weird/Odd.java",
                    "// trampa: class Bogus { void x() {}",
                    "package weird;",
                    "/* } } */ public class Odd {",
                    "    String literal = \"no soy } una declaracion ; class X {\";",
                    "    char brace = '{';",
                    "    void run() { String s = \"}\"; }",
                    "}"));
            TypeNode odd = only(nodes);
            return "weird.Odd".equals(odd.qualifiedName())
                    && odd.methods().size() == 1
                    && odd.fields().size() == 2
                    && !containsType(nodes, "Bogus");
        });
        h.expect("varargs and arrays survive reading", () -> {
            TypeNode shaped = only(parser.parse(Sources.java("demo/Shapes.java",
                    "package demo;",
                    "class Shapes {",
                    "    int[][] grid = new int[1][1];",
                    "    void draw(String... parts) { }",
                    "}")));
            return hasField(shaped, "grid", "int[][]", Visibility.PACKAGE_PRIVATE)
                    && findMethod(shaped, "draw").parameters().get(0).type().equals("String...");
        });
    }

    private static SourceFile userServiceSource() {
        return Sources.java("src/com/demo/UserService.java",
                "package com.demo;",
                "",
                "import java.util.List;",
                "import com.demo.Base;",
                "",
                "/** trampa: class Fake { } */",
                "public abstract class UserService extends Base implements Named, java.io.Serializable {",
                "",
                "    private static final List<String> CACHE = new ArrayList<>();",
                "",
                "    protected int seed;",
                "",
                "    public UserService() { }",
                "",
                "    public UserService(int seed, List<String> history) {",
                "        this.seed = seed;",
                "    }",
                "",
                "    private void audit(String reason) {",
                "        if (reason.contains(\"}\")) { throw new IllegalStateException(\"{};\"); }",
                "    }",
                "",
                "    public Map<String, List<Integer>> grouped(List<Integer> items) {",
                "        Runnable r = new Runnable() { public void run() { } };",
                "        return null;",
                "    }",
                "",
                "    @Override",
                "    public String toString() { return \"u\"; }",
                "}");
    }

    private static <T> T only(List<T> items) {
        if (items.size() != 1) {
            throw new AssertionError("se esperaba un unico elemento y llegaron " + items.size());
        }
        return items.get(0);
    }

    private static boolean containsType(List<TypeNode> nodes, String simpleName) {
        for (TypeNode node : nodes) {
            if (node.simpleName().equals(simpleName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasField(TypeNode type, String name, String fieldType, Visibility visibility) {
        Field found = findField(type, name);
        return found != null && found.type().equals(fieldType)
                && found.visibility() == visibility;
    }

    private static Field findField(TypeNode type, String name) {
        for (Field field : type.fields()) {
            if (field.name().equals(name)) {
                return field;
            }
        }
        return null;
    }

    private static Method findMethod(TypeNode type, String name) {
        for (Method method : type.methods()) {
            if (method.name().equals(name)) {
                return method;
            }
        }
        return null;
    }

    private static boolean allNamed(List<Method> constructors, String name) {
        for (Method constructor : constructors) {
            if (!constructor.name().equals(name) || !constructor.isConstructor()) {
                return false;
            }
        }
        return true;
    }

    private static boolean anyConstructorWithParams(TypeNode type, String... names) {
        for (Method constructor : type.constructors()) {
            if (constructor.parameters().size() == names.length) {
                boolean allMatch = true;
                for (int i = 0; i < names.length; i++) {
                    if (!names[i].equals(constructor.parameters().get(i).name())) {
                        allMatch = false;
                    }
                }
                if (allMatch) {
                    return true;
                }
            }
        }
        return false;
    }
}

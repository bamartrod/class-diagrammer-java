package com.classdiagrammer.tests.unit;

import com.classdiagrammer.domain.model.Method;
import com.classdiagrammer.domain.model.TypeKind;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.domain.model.Visibility;
import com.classdiagrammer.tests.support.TestHarness;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class TypeNodeIntegrity {

    private TypeNodeIntegrity() {
    }

    public static void verify(TestHarness h) {
        h.scope("unit/type-identity");
        h.expect("un tipo sin nombre calificado es rechazado", () -> {
            try {
                TypeNode.named("   ", "A");
                return false;
            } catch (IllegalArgumentException expected) {
                return true;
            }
        });
        h.expect("un tipo sin naturaleza declarada es rechazado",
                () -> refuses(TypeNode.named("p.A", "A")));
        h.expect("type identity is its qualified name", () -> {
            TypeNode first = TypeNode.named("p.A", "A").ofKind(TypeKind.CLASS).build();
            TypeNode twin = TypeNode.named("p.A", "A").ofKind(TypeKind.CLASS)
                    .withVisibility(Visibility.PUBLIC).build();
            TypeNode other = TypeNode.named("p.B", "B").ofKind(TypeKind.CLASS).build();
            return first.equals(twin) && first.hashCode() == twin.hashCode() && !first.equals(other);
        });
        h.expect("los miembros quedan blindados contra cambios externos", () -> {
            List<Method> editable = new ArrayList<>();
            editable.add(Method.returning("go", "void", Visibility.PUBLIC,
                    new HashSet<String>(), new ArrayList<>()));
            TypeNode type = TypeNode.named("p.A", "A").ofKind(TypeKind.CLASS)
                    .withMethods(editable).build();
            editable.clear();
            boolean innerCopySurvives = type.methods().size() == 1;
            try {
                type.methods().add(null);
                return false;
            } catch (UnsupportedOperationException expected) {
                return innerCopySurvives;
            }
        });
    }

    private static boolean refuses(TypeNode.Builder builder) {
        try {
            builder.build();
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }
}

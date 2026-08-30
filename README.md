# ClassDiagrammer — v2.1.0 (Java 21 LTS)

Herramienta **Java 26** que recorre un proyecto, interpreta sus fuentes y genera un
grafo JSON con forma de diagrama de clases: carpetas, paquetes, imports,
`extends`/`implements`/`permits`, constructores, métodos, campos, visibilidad y
modificadores (`sealed`/`non-sealed`/`final`).

**Lenguajes soportados:** código Java (`.java` 8/11/17/21/26), plantillas Apache Velocity
(`.vm`, `.vtl`) y formularios XForms (`.xhtml`/`.xforms`/`.xml` con el
namespace `http://www.w3.org/2002/xforms`). Cada tecnología tiene su parser
especializado detrás del puerto `ArtifactParser`; el enrutado es por extensión
y, para XForms, por contenido. La versión Java analizada se elige con `--java`
y es independiente del JDK que ejecuta la herramienta (runtime 21 puede analizar
fuentes 8).

**Concurrencia:** parsing concurrente con **Virtual Threads** (`Executors.newVirtualThreadPerTaskExecutor()`) en `GenerateClassDiagramUseCase` — 7k tipos de `xwiki-platform` en 14s sin acoplar dominio.

**Origen de dependencias externas.** Las referencias hacia tipos que no están
en el árbol analizado se enriquecen con su artefacto de origen (groupId,
artifactId, versión) leyendo los `pom.xml` / `build.gradle(.kts)` del propio
proyecto y contrastando los paquetes publicados en los JARs del repositorio
local (`~/.m2/repository`, con respaldo en la caché de Gradle). No se explora
el grafo transitivo de las librerías: solo se etiqueta aquello que el código
analizado usa directamente.

**Cero dependencias.** Solo se necesita un **JDK 26**. Sin Maven, Gradle ni librerías de
terceros: parser propio, escritor JSON propio y arnés de verificación propio.

---

## 1. Requisitos

* JDK 21 (`java-21-openjdk`), compilado con `javac --release 21`
* Sin repositorios remotos necesarios

Verificación completa del proyecto:

```bash
./run-tests.sh          # 84 verificaciones, 0 fallos
```

## 2. Uso

```bash
./classdiagrammer.sh <carpeta-fuente> [-o salida.json] [--java <8|11|17|21|26>]

# ejemplos
./classdiagrammer.sh ~/Proyectos/MiProyecto/src -o ~/Descargas/code.json --java 17
./classdiagrammer.sh ~/spring-framework/spring-core --java 17 -o diagramas/spring-core.json
./classdiagrammer.sh ~/spring-framework/spring-web --java 26 -o diagramas/spring-web.json
./classdiagrammer.sh --help
```

`--java` elige el parser Java (defecto 8). Runtime es siempre 26.

## 3. Formato de salida

```json
{
  "tool": "ClassDiagrammer",
  "version": "2.0.0",
  "sourceRoot": "...",
  "summary": { "types": 5, "relations": 4 },
  "nodes": [
    {
      "id": "com.tienda.dominio.Pedido",
      "name": "Pedido",
      "kind": "class",
      "visibility": "public",
      "package": "com.tienda.dominio",
      "folder": "src/com/tienda/dominio",
      "file": "src/com/tienda/dominio/Pedido.java",
      "modifiers": ["public", "sealed"],
      "imports": ["com.tienda.dominio.Cliente"],
      "extends": ["ObjetoDominio"],
      "implements": ["Validable"],
      "permits": ["PedidoExpress"],
      "fields":     [{ "name": "...", "type": "...", "visibility": "private" }],
      "constructors": [{ "name": "Pedido", "visibility": "public", "parameters": [] }],
      "methods":    [{ "name": "total", "returnType": "double", "visibility": "public", "parameters": [] }]
    }
  ],
  "edges": [
    { "from": "com.tienda.dominio.Pedido", "to": "com.tienda.dominio.ObjetoDominio",
      "kind": "extends", "resolved": true, "origin": "project" },
    { "from": "app.Shape", "to": "app.Circle",
      "kind": "permits", "resolved": true, "origin": "project" },
    { "from": "a.X", "to": "ExternoDesconocido", "kind": "extends",
      "resolved": false, "origin": "unknown" },
    { "from": "com.myapp.UserController", "to": "org.apache.velocity.Template",
      "kind": "imports", "resolved": true, "origin": "external",
      "artifact": { "groupId": "org.apache.velocity",
                    "artifactId": "velocity-engine-core", "version": "2.4.1" } }
  ]
}
```

Semántica de `origin`:

- `project` — el destino existe en el grafo analizado (`resolved: true`).
- `external` — el destino no está en el árbol pero se identificó su artefacto
  de origen; `resolved` pasa a `true` y el `to` se reescribe con el nombre
  calificado confirmado por el JAR correspondiente.
- `unknown` — no se pudo atribuir; `resolved: false`, sin objeto `artifact`.

Los imports del JDK (`java.*`, `javax.*`) y los comodines (`foo.bar.*`) se
omiten por ruido; las demás referencias externas generan arista para poder
atribuirlas.

Contenido modelado por tecnología:

| Tecnología | Nodo | Métodos | Campos | Aristas |
| --- | --- | --- | --- | --- |
| Java 8-26 | clase / interfaz / enum / anotación / record (sealed/non-sealed/final conservados) | constructores + métodos | campos | extends / implements / **permits** / imports |
| Velocity `.vm/.vtl` | `template` (id = ruta del archivo) | cada `#macro(name $args)` | cada `#set($var)` global (fuera de macros) | imports hacia `#parse`/`#include` |
| XForms `.xhtml/.xforms/.xml` | `form` (id = ruta del archivo) | cada `<xf:model>` y `<xf:submission>` | cada `<xf:bind nodeset/ref>` | imports hacia instancias/submissions que apuntan a documentos del árbol |

## 4. Arquitectura (hexagonal, Java 21)

```
src/com/classdiagrammer/
├── domain/                  NUCLEO — cero dependencias hacia afuera
│   ├── model/               TypeNode, Method, Field, Edge, EdgeOrigin, ArtifactRef, CodeGraph… (records para Parameter/ArtifactRef, List.copyOf)
│   └── resolution/          EdgeResolver (extends/implements/permits/imports)
├── application/
│   ├── port/in/             GenerateClassDiagram (+ Command / Result)
│   ├── port/out/            SourceCodeReader, ArtifactParser, DependencyResolver,
│   │                        DiagramOutput, DiagramReport
│   └── usecase/             GenerateClassDiagramUseCase (Virtual Threads, 21)
├── infrastructure/
│   ├── filesystem/          FileSystemSourceReader
│   ├── parsing/             LanguageCapabilities + JavaVersion (versión = configuración, no copia)
│   │   ├── java/            JavaArtifactParser (núcleo, text-blocks en SourceText, permits en HeaderParser)
│   │   ├── java11/17/21/26/ Wrappers finos por LTS (composición, CSAS-004-U18)
│   │   ├── velocity/        VelocityArtifactParser, TemplateDirectives, DirectiveReader
│   │   ├── xforms/          XFormsArtifactParser, FormModelCollector
│   │   ├── hibernate/       HbmArtifactParser (.hbm.xml)
│   │   └── xml/             XmlTagScanner (kernel compartido)
│   ├── dependencies/        BuildDependencyScanner, LocalRepositoryIndex, ClasspathArtifactResolver
│   └── json/                JsonDiagramOutput, JsonWriter
└── interfaces/cli/          Main, CliArgs (text blocks, Set.of, switch expression)
```

**Única excepción registrada:** `interfaces.cli.Main` actúa como raíz de
composición e importa `infrastructure.*` para cablear los adaptadores.

## 5. Trazabilidad CSAS

Conforme al Collaborative Software Architecture Standard:

| Regla | Unidad | Aplicación |
| --- | --- | --- |
| Inversión de dependencias | `CSAS-004-U13` | Todas las dependencias apuntan hacia el núcleo; verificada por prueba de arquitectura |
| Aislamiento del núcleo | `CSAS-004-U15` | El dominio no importa aplicación, infraestructura ni interfaces |
| Sin acoplamiento entre adaptadores | `CSAS-004-U18` | Adaptadores no se conocen entre sí; kernels compartidos declarados: `infrastructure.xml` y familia `parsing/java*` |
| Puertos segregados por capacidad | `CSAS-007-U4/U5` | `ArtifactParser` y `DependencyResolver` como puertos; versión Java = `LanguageCapabilities` (OCP) |
| Aislamiento de tipos de infraestructura | `CSAS-004-U20` | Sin anotaciones ni tipos de framework en el núcleo (`QAL-002a`) |
| Ámbitos de verificación | `CSAS-005-U7…U12` | Suites separadas por scope: unit, usecase, adapter, architecture (84 verificaciones) |
| Contaminación inversa prohibida | `CSAS-005-U14` | La producción jamás importa `tests` |
| Nombrado conductual | `CSAS-005-U26`, `CSAS-008-U9…U11` | Toda verificación lleva nombre de comportamiento |
| SOLID | `CSAS-007-U2…U6` | Un caso de uso, puertos segregados, parsers por composición |
| Umbral de cohesión 200 LOC | `CSAS-007-U8`, `CSAS-008-U5` | Bloquea clases >200 (TypeNode 110 tras compactado) |
| Concurrencia | `CSAS-009` | Virtual Threads solo en `application`, dominio puro |
| Layout físico canónico | `CSAS-008-U2/U3/U8` | Producción / prueba / configuración separados |

**Mutación:** suites *mutation-first* (`CSAS-TS-000 §4`). `pitest` diferido por cero-dependencias.

## 6. Límites conocidos

- El parser es pragmático: cubre clases, interfaces, enums, records, sealed/non-sealed, permits, tipos
  anidados, genéricos, varargs, arreglos, anotaciones y text-blocks (`"""`). No construye un AST completo.
- `permits` genera arista `permits` direccional `Shape→Circle` y `Circle→Shape` sigue como `extends`.
- Miembros implícitos de interfaces (`abstract` en métodos) no se añaden como modificadores.

# ClassDiagrammer — v2.0.0 (Java 8)

Tool **Java 8** that scans a project, interprets its sources and generates a
JSON graph shaped as a class diagram: folders, packages, imports,
`extends`/`implements`/`permits`, constructors, methods, fields, visibility and
modifiers (`sealed`/`non-sealed`/`final`).

**Supported languages:** Java code (`.java` 8/11/17/21/25), Apache Velocity
templates (`.vm`, `.vtl`) and XForms forms (`.xhtml`/`.xforms`/`.xml` with
namespace `http://www.w3.org/2002/xforms`). Each technology has its specialized
parser behind the `ArtifactParser` port; routing is by extension and, for
XForms, by content. The Java version to parse is chosen with `--java`
and is independent of the JDK running the tool (runtime 8 can parse
sources 8).

**Concurrency:** concurrent parsing with **Fixed Thread Pool** (`Executors.newFixedThreadPool(...)`) in `GenerateClassDiagramUseCase` — 7k types from `xwiki-platform` in 14s without coupling the domain.

**External dependency origin.** References to types not in the analyzed tree
are enriched with their origin artifact (groupId, artifactId, version) by reading
the project’s own `pom.xml` / `build.gradle(.kts)` and matching published
packages in JARs from the local repository (`~/.m2/repository`, falling back to
Gradle cache). The transitive graph of libraries is not explored: only what the
analyzed code directly uses is tagged.

**Zero dependencies.** Only a **JDK 8** is needed. No Maven, Gradle or third-party
libraries: own parser, own JSON writer and own verification harness.

---

## 1. Requirements

* JDK 8 (`java-8-openjdk`), compiled with `javac --release 8` (or any JDK with `--release 8` support, e.g., 17/21/26)
* No remote repositories required

Full project verification:

```bash
./run-tests.sh          # 84 checks, 0 failures
```

## 2. Usage

```bash
./classdiagrammer.sh <source-folder> [-o output.json] [--java <8|11|17|21|25>]

# examples
./classdiagrammer.sh ~/Projects/MyProject/src -o ~/Downloads/code.json --java 17
./classdiagrammer.sh ~/spring-framework/spring-core --java 17 -o diagrams/spring-core.json
./classdiagrammer.sh ~/spring-framework/spring-web --java 8 -o diagrams/spring-web.json
./classdiagrammer.sh --help
```

`--java` selects the Java parser (default 8). Runtime is always 8.

## 3. Output format

```json
{
  "tool": "ClassDiagrammer",
  "version": "v2.0.0",
  "sourceRoot": "...",
  "summary": { "types": 5, "relations": 4 },
  "nodes": [
    {
      "id": "com.shop.domain.Order",
      "name": "Order",
      "kind": "class",
      "visibility": "public",
      "package": "com.shop.domain",
      "folder": "src/com/shop/domain",
      "file": "src/com/shop/domain/Order.java",
      "modifiers": ["public", "sealed"],
      "imports": ["com.shop.domain.Customer"],
      "extends": ["DomainObject"],
      "implements": ["Validatable"],
      "permits": ["ExpressOrder"],
      "fields":     [{ "name": "...", "type": "...", "visibility": "private" }],
      "constructors": [{ "name": "Order", "visibility": "public", "parameters": [] }],
      "methods":    [{ "name": "total", "returnType": "double", "visibility": "public", "parameters": [] }]
    }
  ],
  "edges": [
    { "from": "com.shop.domain.Order", "to": "com.shop.domain.DomainObject",
      "kind": "extends", "resolved": true, "origin": "project" },
    { "from": "app.Shape", "to": "app.Circle",
      "kind": "permits", "resolved": true, "origin": "project" },
    { "from": "a.X", "to": "UnknownExternal", "kind": "extends",
      "resolved": false, "origin": "unknown" },
    { "from": "com.myapp.UserController", "to": "org.apache.velocity.Template",
      "kind": "imports", "resolved": true, "origin": "external",
      "artifact": { "groupId": "org.apache.velocity",
                    "artifactId": "velocity-engine-core", "version": "2.4.1" } }
  ]
}
```

`origin` semantics:

- `project` — destination exists in the analyzed graph (`resolved: true`).
- `external` — destination not in tree but its origin artifact was identified;
  `resolved` becomes `true` and `to` is rewritten with the qualified name
  confirmed by the corresponding JAR.
- `unknown` — could not be attributed; `resolved: false`, no `artifact` object.

JDK imports (`java.*`, `javax.*`) and wildcards (`foo.bar.*`) are omitted as
noise; other external references generate an edge so they can be attributed.

Modeled content by technology:

| Technology | Node | Methods | Fields | Edges |
| --- | --- | --- | --- | --- |
| Java 8-25 | class / interface / enum / annotation / record (sealed/non-sealed/final preserved) | constructors + methods | fields | extends / implements / **permits** / imports |
| Velocity `.vm/.vtl` | `template` (id = file path) | each `#macro(name $args)` | each `#set($var)` global (outside macros) | imports towards `#parse`/`#include` |
| XForms `.xhtml/.xforms/.xml` | `form` (id = file path) | each `<xf:model>` and `<xf:submission>` | each `<xf:bind nodeset/ref>` | imports towards instances/submissions pointing to in-tree documents |

## 4. Architecture (hexagonal, Java 8)

```
src/com/classdiagrammer/
├── domain/                  CORE — zero outward dependencies
│   ├── model/               TypeNode, Method, Field, Edge, EdgeOrigin, ArtifactRef, CodeGraph… (records for Parameter/ArtifactRef, List.copyOf)
│   └── resolution/          EdgeResolver (extends/implements/permits/imports)
├── application/
│   ├── port/in/             GenerateClassDiagram (+ Command / Result)
│   ├── port/out/            SourceCodeReader, ArtifactParser, DependencyResolver,
│   │                        DiagramOutput, DiagramReport
│   └── usecase/             GenerateClassDiagramUseCase (Fixed Thread Pool, 8)
├── infrastructure/
│   ├── filesystem/          FileSystemSourceReader
│   ├── parsing/             LanguageCapabilities + JavaVersion (version = configuration, not copy)
│   │   ├── java/            JavaArtifactParser (core, text-blocks in SourceText, permits in HeaderParser)
│   │   ├── java11/17/21/25/ Thin wrappers per LTS (composition, CSAS-004-U18)
│   │   ├── velocity/        VelocityArtifactParser, TemplateDirectives, DirectiveReader
│   │   ├── xforms/          XFormsArtifactParser, FormModelCollector
│   │   ├── hibernate/       HbmArtifactParser (.hbm.xml)
│   │   └── xml/             XmlTagScanner (shared kernel)
│   ├── dependencies/        BuildDependencyScanner, LocalRepositoryIndex, ClasspathArtifactResolver
│   └── json/                JsonDiagramOutput, JsonWriter
└── interfaces/cli/          Main, CliArgs (text blocks, Set.of, switch expression)
```

**Only registered exception:** `interfaces.cli.Main` acts as composition root
and imports `infrastructure.*` to wire adapters.

## 5. CSAS Traceability

Compliant with the Collaborative Software Architecture Standard:

| Rule | Unit | Application |
| --- | --- | --- |
| Dependency inversion | `CSAS-004-U13` | All dependencies point towards the core; verified by architecture test |
| Core isolation | `CSAS-004-U15` | Domain imports nothing from application, infrastructure or interfaces |
| No coupling between adapters | `CSAS-004-U18` | Adapters do not know each other; shared kernels declared: `infrastructure.xml` and `parsing/java*` family |
| Ports segregated by capability | `CSAS-007-U4/U5` | `ArtifactParser` and `DependencyResolver` as ports; Java version = `LanguageCapabilities` (OCP) |
| Infrastructure type isolation | `CSAS-004-U20` | No framework annotations or types in core (`QAL-002a`) |
| Verification scopes | `CSAS-005-U7…U12` | Suites separated by scope: unit, usecase, adapter, architecture (84 checks) |
| Reverse contamination forbidden | `CSAS-005-U14` | Production never imports `tests` |
| Behavioral naming | `CSAS-005-U26`, `CSAS-008-U9…U11` | Every check bears a behavioral name |
| SOLID | `CSAS-007-U2…U6` | One use case, segregated ports, parsers by composition |
| Cohesion threshold 200 LOC | `CSAS-007-U8`, `CSAS-008-U5` | Blocks classes >200 (TypeNode 110 after compaction) |
| Concurrency | `CSAS-009` | Fixed Thread Pool only in `application`, pure domain |
| Canonical physical layout | `CSAS-008-U2/U3/U8` | Production / test / configuration separated |

**Mutation:** *mutation-first* suites (`CSAS-TS-000 §4`). `pitest` deferred due to zero-dependency.

## 6. Known limits

- The parser is pragmatic: covers classes, interfaces, enums, records, sealed/non-sealed, permits, nested
  types, generics, varargs, arrays, annotations and text-blocks (`"""`). It does not build a full AST.
- `permits` generates a directional `permits` edge `Shape→Circle` and `Circle→Shape` remains as `extends`.
- Implicit interface members (`abstract` on methods) are not added as modifiers.

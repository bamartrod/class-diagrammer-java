# ClassDiagrammer — v2.0.0 (Java 11 LTS)

Tool **Java 11** that scans a project, interprets its sources and generates a
JSON graph shaped as a class diagram: folders, packages, hierarchy imports
(`extends`/`implements`/`permits` at node level), constructors, methods, fields,
visibility, modifiers (`sealed`/`non-sealed`/`final`) and per-member
`requiredImports` (field/method/constructor) resolved via `ImportResolver`.

**Supported languages:** Java code (`.java` 8/11/17/21/25), Apache Velocity
templates (`.vm`, `.vtl`) and XForms forms (`.xhtml`/`.xforms`/`.xml` with
namespace `http://www.w3.org/2002/xforms`). Each technology has its specialized
parser behind the `ArtifactParser` port; routing is by extension and, for
XForms, by content. The Java version to parse is chosen with `--java`
and is independent of the JDK running the tool (runtime 25 can parse
sources 8).

**Concurrency:** concurrent parsing with **Virtual Threads** (`Executors.newVirtualThreadPerTaskExecutor()`) in `GenerateClassDiagramUseCase` — 7k types from `xwiki-platform` in 14s without coupling the domain.

**External dependency origin.** References to types not in the analyzed tree
are enriched with their origin artifact (groupId, artifactId, version) by reading
the project’s own `pom.xml` / `build.gradle(.kts)` and matching published
packages in JARs from the local repository (`~/.m2/repository`, falling back to
Gradle cache). The transitive graph of libraries is not explored: only what the
analyzed code directly uses is tagged.

**Zero dependencies.** Only a **JDK 11** is needed. No Maven, Gradle or third-party
libraries: own parser, own JSON writer and own verification harness.

---

## 1. Requirements

* JDK 11 (`java-11-openjdk`), compiled with `javac --release 11` (or any JDK with `--release 11` support, e.g., 17/21/25)
* No remote repositories required

Full project verification:

```bash
./run-tests.sh          # 107 checks, 0 failures
```

## 2. Usage

```bash
./classdiagrammer.sh <source-folder> [-o output.json] [--java <8|11|17|21|25>] [--format <json|xml|yaml|toon>]

# examples
./classdiagrammer.sh ~/Projects/MyProject/src -o ~/Downloads/code.json --java 17
./classdiagrammer.sh ~/spring-framework/spring-core --java 17 -o diagrams/spring-core.json
./classdiagrammer.sh ~/spring-framework/spring-web --java 11 -o diagrams/spring-web.json --format xml
./classdiagrammer.sh ./src -o diagram.yaml --java 25
./classdiagrammer.sh ./src -o diagram.toon --java 25
./classdiagrammer.sh --help
```

`--java` selects the Java parser (default 8). `--format` selects the output format (`json`, `xml`, `yaml`, `toon`); inferred from output file extension if not given (default `json`). Runtime is always 11. The selected version is behaviorally effective: unsupported features for that version yield an explicit `UNSUPPORTED` evidence instead of being silently accepted.

## 3. Output format

```json
{
  "tool": "ClassDiagrammer",
  "version": "2.0.0",
  "sourceRoot": "...",
  "summary": { "types": 5, "relations": 4, "evidences": 12, "evaluation": "conformant" },
  "evaluation": "conformant",
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
      "imports": ["com.shop.domain.DomainObject"],
      "extends": ["DomainObject"],
      "implements": ["Validatable"],
      "permits": ["ExpressOrder"],
      "fields":     [{ "name": "customer", "type": "Customer", "visibility": "private", "requiredImports": ["com.shop.domain.Customer"] }],
      "constructors": [{ "name": "Order", "visibility": "public", "parameters": [], "requiredImports": [] }],
      "methods":    [{ "name": "total", "returnType": "Money", "visibility": "public", "parameters": [], "requiredImports": ["com.shop.domain.Money"] }]
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
  ],
  "evidences": [
    { "evidenceId": "ORIGIN-com.shop.domain.Order", "sourceFile": "com.shop.domain.Order",
      "locator": "com.shop.domain.Order", "derivation": "ArchitecturalOriginResolver",
      "factKind": "ARCHITECTURAL_ORIGIN", "subject": "com.shop.domain.Order->com.shop.domain.DomainObject",
      "value": "project", "ruleId": "CSAS-006-U4" }
  ]
}
```

`origin` semantics:

- `project` — destination exists in the analyzed graph (`resolved: true`).
- `external` — destination not in tree but its origin artifact was identified;
  `resolved` becomes `true` and `to` is rewritten with the qualified name
  confirmed by the corresponding JAR.
- `unknown` — could not be attributed; `resolved: false`, no `artifact` object.

Hierarchy `imports` and per-member `requiredImports` omit `java.*`/`javax.*` and wildcards (`foo.bar.*`) as noise. Node `imports` holds only hierarchy-qualified imports (`extends`/`implements`/`permits`); each `field`/`method`/`constructor` carries `requiredImports` — the sorted, deduplicated subset of that file's imports whose simple names appear in that member's type signature (via `ImportResolver`). `imports` edges are aggregated from per-member `requiredImports` (fallback to node `imports` when a type has no members), so `external` attribution is member-accurate.

**Output formats:** `json` (default), `xml` (well-formed, `<?xml?>` + `<diagram>`), `yaml` (YAML 1.2, `tool: ...`), `toon` (Token-Oriented, minimal, `tool: ClassDiagrammer` per line, `nodes[82]:`). All formats carry the same semantic model (`summary`, `nodes`, `edges`, `evidences`, `evaluation`) and are deterministic. Selection via `--format` or inferred from file extension (`.xml`, `.yaml`/`.yml`, `.toon`, `.json`).

Modeled content by technology:

| Technology | Node | Methods | Fields | Edges |
| --- | --- | --- | --- | --- |
| Java 8-25 | class / interface / enum / annotation / record (sealed/non-sealed/final preserved) | constructors + methods (`requiredImports` per member) | fields (`requiredImports` per field) | `extends` / `implements` / **permits** / `imports` (hierarchy-only at node, `requiredImports` per member → `imports` edges) |
| Velocity `.vm/.vtl` | `template` (id = file path) | each `#macro(name $args)` | each `#set($var)` global (outside macros) | imports towards `#parse`/`#include` |
| XForms `.xhtml/.xforms/.xml` | `form` (id = file path) | each `<xf:model>` and `<xf:submission>` | each `<xf:bind nodeset/ref>` | imports towards instances/submissions pointing to in-tree documents |

## 4. Architecture (hexagonal, Java 11)

```
src/com/classdiagrammer/
├── domain/                  CORE — zero outward dependencies
│   ├── model/               TypeNode (hierarchy `imports` only), Field/Method/Constructor with `requiredImports`, Edge, EdgeOrigin, ArtifactRef, CodeGraph… (records for Parameter/ArtifactRef, List.copyOf)
│   └── resolution/          EdgeResolver (extends/implements/permits + per-member `requiredImports` → `imports` edges), TypeQualifier, ArchitecturalOriginResolver
├── application/
│   ├── port/in/             GenerateClassDiagram (+ Command / Result)
│   ├── port/out/            SourceCodeReader, ArtifactParser, DependencyResolver,
│   │                        DiagramOutput, DiagramReport
│   └── usecase/             GenerateClassDiagramUseCase (Fixed Thread Pool, 11)
├── infrastructure/
│   ├── filesystem/          FileSystemSourceReader
│   ├── parsing/             LanguageCapabilities + JavaVersion (version = configuration, not copy)
│   │   ├── java/            JavaArtifactParser (core, `filterHierarchyImports` → node `imports`, `ImportResolver` per member via `SignatureInterpreter`/`MemberScanner` with `classImports`)
│   │   ├── java11/17/21/25/ Thin wrappers per LTS (composition)
│   │   ├── velocity/        VelocityArtifactParser, TemplateDirectives, DirectiveReader
│   │   ├── xforms/          XFormsArtifactParser, FormModelCollector
│   │   ├── hibernate/       HbmArtifactParser (.hbm.xml)
│   │   └── xml/             XmlTagScanner (shared kernel)
│   ├── dependencies/        BuildDependencyScanner, LocalRepositoryIndex, ClasspathArtifactResolver
│   ├── json/                JsonDiagramOutput, JsonWriter
│   └── output/              OutputFormat, DiagramOutputFactory, Xml/Yaml/ToonDiagramOutput (each emits `requiredImports` per field/method/constructor)
└── interfaces/cli/          Main, CliArgs (text blocks, Set.of, switch expression)
```

**Only registered exception:** `interfaces.cli.Main` acts as composition root
and imports `infrastructure.*` to wire adapters.

## 5. Implementation Model

```
ImplementationModel
  ├── StructuralModel (CodeGraph, TypeNode, Edge)
  ├── SemanticFacts (FactKind: TYPE_EXISTS, DEPENDENCY_EXISTS, LANGUAGE_FEATURE_USAGE, CONFIGURATION_*, ARCHITECTURAL_ORIGIN, etc.)
  ├── Evidence (attributable, addressable, reproducible per CSAS-002)
  ├── LanguageModel (LanguageCapabilities per JavaVersion)
  ├── DependencyModel (enriched with artifact origin)
  └── Evaluation (CONFORMANT, NON_CONFORMANT, UNSUPPORTED, UNDECIDABLE, REVIEW_REQUIRED)
```

The class diagram is a **view** over the semantic model, not the model itself.

**Language-version handling:** `JavaVersion` + `LanguageCapabilities` is now behaviorally effective. Selecting `--java 8` and analyzing a `record` yields `UNSUPPORTED` evidence instead of silent success. Capabilities: `RECORD`, `SEALED_TYPE`, `TEXT_BLOCK`, `PATTERN_MATCHING`, etc., mapped per LTS (8: none, 11: none, 17+: all).

**Evidence:** every fact carries `subject`, `locator` (file:line), `value`, `ruleId`, and `derivation`. Example: `ARCHITECTURAL_ORIGIN` for `A→B` at `src/A.java:1` via `ArchitecturalOriginResolver`.

**Determinism:** file traversal sorted, parsing futures joined in submission order, nodes/edges sorted by `qualifiedName`/`from→to` before JSON emission.

**Failure semantics:** `Main` distinguishes `INPUT_VALIDATION`/`CONFIGURATION` (exit 2), `EXPECTED_OPERATIONAL` (exit 1), `PROGRAMMER_DEFECT` (exit 3, stacktrace), `FATAL` (exit 4). `StackOverflowError` is no longer caught; `RegionScanner` is bounded to `MAX_DEPTH=100` and reports `REVIEW_REQUIRED`.

**Configuration effectiveness:** `JavaVersion` declared via `CliArgs`, consumed via `JavaParserFactory.forVersion`, affects `LanguageCapabilities` and parser behavior—verified by version-correctness tests.

## 6. Architecture Principles

This project follows hexagonal architecture, SOLID, and clean verification practices. `GenerateClassDiagramUseCase` now has a single cohesive responsibility via `TypeQualifier` and `ArchitecturalOriginResolver` extraction, and `Main` owns only composition.

## 7. Known limits

- The parser is pragmatic: covers classes, interfaces, enums, records, sealed/non-sealed, permits, nested
  types, generics, varargs, arrays, annotations and text-blocks (`"""`). It does not build a full AST.
- `permits` generates a directional `permits` edge `Shape→Circle` and `Circle→Shape` remains as `extends`.
- Implicit interface members (`abstract` on methods) are not added as modifiers.

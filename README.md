# ClassDiagrammer

Herramienta Java 8 que recorre un proyecto, interpreta sus fuentes y genera un
grafo JSON con forma de diagrama de clases: carpetas, paquetes, imports,
`extends`/`implements`, constructores, métodos, campos y visibilidad de cada
miembro.

**Lenguajes soportados:** código Java (`.java`), plantillas Apache Velocity
(`.vm`, `.vtl`) y formularios XForms (`.xhtml`/`.xforms`/`.xml` con el
namespace `http://www.w3.org/2002/xforms`). Cada tecnología tiene su parser
especializado detrás del puerto `ArtifactParser`; el enrutado es por extensión
y, para XForms, por contenido.

**Origen de dependencias externas.** Las referencias hacia tipos que no están
en el árbol analizado se enriquecen con su artefacto de origen (groupId,
artifactId, versión) leyendo los `pom.xml` / `build.gradle(.kts)` del propio
proyecto y contrastando los paquetes publicados en los JARs del repositorio
local (`~/.m2/repository`, con respaldo en la caché de Gradle). No se explora
el grafo transitivo de las librerías: solo se etiqueta aquello que el código
analizado usa directamente.

**Cero dependencias.** Solo se necesita un JDK. Sin Maven, Gradle ni librerías de
terceros: parser propio, escritor JSON propio y arnés de verificación propio.
Esto es deliberado — el proyecto debe compilar y ejecutarse en máquinas sin
acceso a repositorios remotos.

---

## 1. Uso

```bash
./classdiagrammer.sh <carpeta-fuente> [-o salida.json]

# ejemplos
./classdiagrammer.sh ~/Proyectos/MiProyecto/src -o ~/Descargas/code.json
./classdiagrammer.sh ~/Proyectos/MiProyecto            # -> ./code.json
./classdiagrammer.sh --help
```

Verificación completa del proyecto:

```bash
./run-tests.sh
```

## 2. Formato de salida

```json
{
  "tool": "ClassDiagrammer",
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
      "modifiers": ["public"],
      "imports": ["com.tienda.dominio.Cliente"],
      "extends": ["ObjetoDominio"],
      "implements": ["Validable"],
      "fields":     [{ "name": "...", "type": "...", "visibility": "private" }],
      "constructors": [{ "name": "Pedido", "visibility": "public", "parameters": [] }],
      "methods":    [{ "name": "total", "returnType": "double", "visibility": "public", "parameters": [] }]
    }
  ],
  "edges": [
    { "from": "com.tienda.dominio.Pedido", "to": "com.tienda.dominio.ObjetoDominio",
      "kind": "extends", "resolved": true, "origin": "project" },
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
| Java | clase / interfaz / enum / anotación / record | constructores + métodos | campos | extends / implements / imports |
| Velocity `.vm/.vtl` | `template` (id = ruta del archivo) | cada `#macro(name $args)` | cada `#set($var)` global (fuera de macros) | imports hacia `#parse`/`#include` |
| XForms `.xhtml/.xforms/.xml` | `form` (id = ruta del archivo) | cada `<xf:model>` y `<xf:submission>` | cada `<xf:bind nodeset/ref>` | imports hacia instancias/submissions que apuntan a documentos del árbol |

Los controles de formulario (`xf:input`, `xf:output`…) se ignoran
deliberadamente: son ruido estructural para un diagrama de clases.

## 3. Arquitectura (hexagonal)

```
src/com/classdiagrammer/
├── domain/                  NUCLEO — cero dependencias hacia afuera
│   ├── model/               TypeNode, Method, Field, Edge, EdgeOrigin, ArtifactRef, CodeGraph…
│   └── resolution/          EdgeResolver (herencia, realización, imports atribuibles)
├── application/
│   ├── port/in/             GenerateClassDiagram (+ Command / Result)
│   ├── port/out/            SourceCodeReader, ArtifactParser, DependencyResolver,
│   │                        DiagramOutput, DiagramReport
│   └── usecase/             GenerateClassDiagramUseCase (orquesta + enriquece orígenes)
├── infrastructure/
│   ├── filesystem/          FileSystemSourceReader (adaptador dirigido)
│   ├── parsing/java/        JavaArtifactParser + colaboradores del parser artesanal
│   ├── parsing/velocity/    VelocityArtifactParser, TemplateDirectives, DirectiveReader
│   ├── parsing/xforms/      XFormsArtifactParser, FormModelCollector
│   ├── xml/                 XmlTagScanner (kernel compartido de lexicon XML)
│   ├── dependencies/        BuildDependencyScanner, LocalRepositoryIndex,
│   │                        ClasspathArtifactResolver
│   └── json/                JsonDiagramOutput, JsonWriter
└── interfaces/cli/          Main, CliArgs (adaptador director)

test/java/com/classdiagrammer/tests/
├── unit/                    Verificación unitaria (dominio aislado)
├── usecase/                 Verificación de caso de uso (puertos con dobles)
├── adapter/                 Integración de adaptadores (fs real, json real, jars reales)
├── architecture/            Conformidad hexagonal ejecutable
└── support/                 Arnés mínimo + dobles explícitos
```

**Única excepción registrada:** `interfaces.cli.Main` actúa como raíz de
composición e importa `infrastructure.*` para cablear los adaptadores. Es una
decisión deliberada, localizada en un único archivo.

## 4. Trazabilidad CSAS

Conforme al Collaborative Software Architecture Standard:

| Regla | Unidad | Aplicación |
| --- | --- | --- |
| Inversión de dependencias | `CSAS-004-U13` | Todas las dependencias apuntan hacia el núcleo; verificada por prueba de arquitectura |
| Aislamiento del núcleo | `CSAS-004-U15` | El dominio no importa aplicación, infraestructura ni interfaces |
| Sin acoplamiento entre adaptadores | `CSAS-004-U18` | Adaptadores de parsing/filesystem/json/dependencies no se conocen entre sí; único kernel compartido declarado: `infrastructure.xml` (lexicon XML) |
| Puertos segregados por capacidad | `CSAS-007-U4/U5` | `ArtifactParser` y `DependencyResolver` como puertos dirigidos independientes; parsers especializados por tecnología como adaptadores intercambiables (OCP, `CSAS-007-U3`) |
| Aislamiento de tipos de infraestructura | `CSAS-004-U20` | Sin anotaciones ni tipos de framework en el núcleo (`QAL-002a`) |
| Ámbitos de verificación | `CSAS-005-U7…U12` | Suites separadas por scope: unit, usecase, adapter, architecture |
| Contaminación inversa prohibida | `CSAS-005-U14` | La producción jamás importa `tests`; verificado estáticamente |
| Nombrado conductual | `CSAS-005-U26`, `CSAS-008-U9…U11` | Toda verificación lleva nombre de comportamiento de negocio, sin vocabulario técnico |
| SOLID | `CSAS-007-U2…U6` | Un caso de uso, puertos segregados, sustitución vía puertos |
| Umbral de cohesión 200 LOC | `CSAS-007-U8`, `CSAS-008-U5` | Prueba de arquitectura que bloquea clases por encima del umbral |
| Pruebas no espejadas | `CSAS-007-U13`, `CSAS-008-U7` | Árbol de pruebas organizado por intención de verificación, no espejo de producción |
| Layout físico canónico | `CSAS-008-U2/U3/U8` | Producción / prueba / configuración separados; núcleo–casos de uso–adaptadores |
| Análisis estático sin ejecución | `QAL-002b/d` | Grafo de dependencias reconstruible leyendo fuentes; reglas aplicadas en build |

**Mutación:** las suites se construyen *mutation-first* (una condición por
verificación, resultado sensible a la alteración de la regla que ejercita, cf.
`CSAS-TS-000 §4`). La medición de *mutation score* exige una herramienta
(pitest), que rompería la restricción de cero dependencias; queda diferida como
actividad de tooling y no como deuda de construcción.

## 5. Límites conocidos

- El parser es pragmático: cubre clases, interfaces, enums, records, tipos
  anidados, genéricos, varargs, arreglos, anotaciones y literales con llaves.
  No construye un AST completo de Java.
- Miembros implícitos distintos de la visibilidad pública de interfaces
  (p. ej. `abstract` implícito en métodos de interfaz) no se añaden como
  modificadores.
- Las cláusulas `permits`/`sealed` se leen pero no alimentan aristas.

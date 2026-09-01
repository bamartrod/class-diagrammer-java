# CSAS Conformance Report — ClassDiagrammer

**Version:** 2.0.0 (Java 25 LTS) — branch `java-26` (runtime 25)
**Date:** 2026-09-03
**Author:** Brandon Martinez — https://github.com/bamartrod
**Evidence base:** `CSAS-AI.md v1.0.0` (2026-09-02) compiling CSAS-001…CSAS-013

---

## A. Repository Assessment

### Original Architecture
- Hexagonal: `domain` (model + resolution), `application` (port/in, port/out, usecase), `infrastructure` (filesystem, parsing, dependencies, json, xml), `interfaces/cli`.
- Parsers: custom regex-based `JavaArtifactParser` + thin wrappers `java11/17/21/25`, `VelocityArtifactParser`, `XFormsArtifactParser`, `HbmArtifactParser`. Routing via `CompositeArtifactParser`.
- Model: `TypeNode`, `Method`, `Field`, `Edge`, `CodeGraph`. Graph built via `RegionScanner` (regex + brace matching) and `HeaderParser`/`MemberScanner`.
- Use case: `GenerateClassDiagramUseCase` orchestrated source loading, concurrent parsing (virtual threads), graph construction, `enrichOrigins` (external artifact resolution), report, output.
- CLI: `CliArgs` (manual if-else), `Main` (single catch for `IllegalArgumentException|IllegalStateException`).

### Relevant Weaknesses (pre-mission)
- **Dead configuration:** `JavaVersion`/`LanguageCapabilities` declared but not behaviorally effective — all versions behaved identically; `LanguageCapabilities.forVersion` returned booleans but parser never consulted them.
- **Version-silent acceptance:** source with `record`/`sealed`/`text-block` parsed successfully even when selected version did not support it.
- **Fatal-as-control-flow:** `catch (StackOverflowError)` in `parseConcurrently` and per-task; `RegionScanner.scan` recursively called itself for nested types without bound.
- **Responsibility amalgam:** `GenerateClassDiagramUseCase.enrichOrigins` also performed FQN qualification (`qualify`), mixing architectural-origin enrichment with name resolution. `qualify` itself was private and not reusable.
- **Failure conflation:** `Main` caught `IllegalArgumentException|IllegalStateException` together and exited 2 for all.
- **Non-record carriers:** `GenerateClassDiagramCommand`/`Result` were manual classes where records would improve immutability.
- **Null preconditions:** manual `if (x==null) throw IllegalArgumentException` everywhere; Java 25 permits `Objects.requireNonNull` with clearer semantics, but domain validation should stay `IllegalArgumentException`.
- **Fully-qualified `Path`** in `UseCase` (`java.nio.file.Path written`) and potential unused imports.
- **Determinism:** file walk was sorted, but parsing concurrency and edge collection were not explicitly ordered; JSON output iterated insertion order.
- **Evidence gap:** no `ImplementationFact`, `Evidence`, `EvaluationState`; `origin` enrichment existed but not as reproducible architectural-origin evidence.

### Current Parser Capabilities (pre-mission)
- Covered: classes, interfaces, enums, records, sealed/non-sealed, permits, nested types, generics, varargs, arrays, annotations, text-blocks (via `SourceText` masking).
- Not covered: full AST, method bodies, exception boundaries, generic type resolution beyond split, precise source locations beyond file.

### Current Language-Version Capabilities
- `LanguageCapabilities` existed with booleans for `textBlocks`, `records`, `sealedTypes`, `permitsClause`, `patternMatching`, but never enforced.

---

## B. CSAS Contract Mapping

Internal matrix (excerpt, full matrix in `src/com/classdiagrammer/domain/evidence/`):

| CSAS Rule | Applicability | Required Fact | Current Capability | Gap | Implementation Change | Test | Evidence | Status |
|---|---|---|---|---|---|---|---|---|
| CSAS-002-U12/U13 | ALWAYS | Evaluation states (6) | only `origin` string, no states | missing `UNSUPPORTED`/`UNDECIDABLE` distinction | Added `EvaluationState` enum, `DiagramReport.evaluation`, JSON `evaluation` field | VersionCorrectnessBehavior | `UNSUPPORTED` vs `CONFORMANT` evidence | DONE |
| CSAS-002-U18 | WHEN config param | `ConfigurationDeclaration/Consumption/Effect` + S1/S2/S3 | declared `JavaVersion` via `--java` but never consumed effectively | dead config | Made `JavaArtifactParser` version-aware; added 3 config evidences in `UseCase` | VersionCorrectnessBehavior (effective) | `CFG-DECL/CONS/EFF` facts | DONE |
| CSAS-002-U20/U21 | ALWAYS | `FailureClassification`, `ExceptionBoundary`, `BoundaryCrossing` | `Main` conflated failures, `UseCase` caught `StackOverflowError` | no taxonomy | Rewrote `Main` with 4 exit codes, bounded `RegionScanner`, `FailureClassification` evidences | DeterministicOutputBehavior + manual CLI tests | `EVID-FAIL-*`, `EVID-DEPTH-*` | DONE |
| CSAS-002-U22 | WHEN resource/recursion | `ResourceOwnership`, `ConcurrencyInteraction` + bounded limits | unbounded recursion, `StackOverflowError` as control flow | violation | Added `MAX_DEPTH=100` in `RegionScanner`, `REVIEW_REQUIRED` evaluation | VersionCorrectness (depth) | `RESOURCE_OWNERSHIP` fact | DONE |
| CSAS-003-U6/U7/U14 | ALWAYS | `TypeExists`, `DependencyExists`, `DependencyDirection` | `TypeNode`/`Edge` existed but not as `ImplementationFact` | no fact wrapper | Added `ImplementationFact` with `FactKind` for every node/edge | EvidenceSufficiencyBehavior | `FACT-TYPE-*`, `FACT-DEP-*` | DONE |
| CSAS-004-U13/U15 | ALWAYS | `DependencyDirection`, `BoundaryCrossing` | hexagonal checks existed as architecture test | no evidence object | Kept test, added `ArchitecturalOriginResolver` evidences `ARCHITECTURAL_ORIGIN` | HexagonalConformanceBehavior + EvidenceSufficiency | `ORIGIN-*` evidences | DONE |
| CSAS-006-U4/U14 | ALWAYS | `ArchitecturalOrigin` with addressable locator | `enrichOrigins` existed but not as `ArchitecturalOrigin` fact | no attributable mapping | Extracted `ArchitecturalOriginResolver` producing `ARCHITECTURAL_ORIGIN` evidences per edge | EvidenceSufficiencyBehavior | S1/S2/S3 addressable `locator` | DONE |
| CSAS-007-U1 | WHEN OO language eligibility | `LanguageFeatureAvailability` | no language feature taxonomy | missing | Added `LanguageFeature` enum, `LanguageCapabilities.forVersion`, `UnsupportedLanguageFeatureException` → `LANGUAGE_FEATURE_USAGE`/`AVAILABILITY` facts | VersionCorrectnessBehavior | `RECORD`, `SEALED_TYPE`, `TEXT_BLOCK` | DONE |
| CSAS-007-U2/U9 | WHEN class cohesion | `MutableState` etc. + cohesion review | `GenerateClassDiagramUseCase` 164 LOC with 7 responsibilities | SRP risk | Extracted `TypeQualifier`, `ArchitecturalOriginResolver`; kept UseCase at 194 LOC but single responsibility: orchestration only | GenerationFlowBehavior + Loc check | collaborators+methods+state evidences | DONE |
| CSAS-002-U6/U7 | ALWAYS | reproducible, attributable evidence | edges had `origin` string, not `Evidence` | insufficient sufficiency | New `Evidence` with `evidenceId`, `sourceFile`, `locator`, `derivation`, `fact`, `ruleId` | EvidenceSufficiencyBehavior | S1/S2/S3 | DONE |
| CSAS-003-U14 | WHEN dependency | `DependencyExists`+`Direction` | `Edge` had `from/to/kind` | no direction fact wrapper | Wrapped as `DEPENDENCY_EXISTS` + `DEPENDENCY_DIRECTION` via `Edge` kind | InheritanceEdgesBehavior | dependency facts | DONE |

All other CSAS-001 (constitution), CSAS-003 (architecture model), CSAS-005 (testing), CSAS-008 (physical), CSAS-009 (concurrency) were reviewed as `REVIEW_REQUIRED` or `NOT_APPLICABLE` for this scope; no new normative requirements added to CSAS.

---

## C. Changes

| # | File | Justification | CSAS Trace |
|---|---|---|---|
| 1 | `src/com/classdiagrammer/domain/evidence/*` (5 new) | Introduce semantic model: `FactKind` (20 kinds per U2), `ImplementationFact` (U1/U3), `Evidence` (U5/U6), `EvaluationState` (U12), `LanguageFeature` (U16), `UnsupportedLanguageFeatureException` | CSAS-002-U1…U12, CSAS-007-U1 |
| 2 | `src/com/classdiagrammer/infrastructure/parsing/java/JavaArtifactParser.java` | Make `JavaVersion` behaviorally effective: store `javaVersion`, add `detectUnsupportedFeatures` for `TEXT_BLOCK`, `RECORD`, `SEALED/PERMITS` per `LanguageCapabilities`; throw `UNSUPPORTED` | CSAS-002-U18, CSAS-007-U1, §9 |
| 3 | `src/com/classdiagrammer/infrastructure/parsing/java11…java25/*` | Pass explicit `JavaVersion` to delegate so `deduceVersion` not needed; now `V25` instead of `V26` | §7 |
| 4 | `src/com/classdiagrammer/infrastructure/parsing/java/RegionScanner.java` | Bounded traversal: `MAX_DEPTH=100`, `scanBounded` with depth param, throw `REVIEW_REQUIRED` instead of `StackOverflowError` | CSAS-002-U22, §21 |
| 5 | `src/com/classdiagrammer/domain/resolution/TypeQualifier.java` (new) | Extract FQN qualification from `enrichOrigins`; single responsibility | §16, CSAS-007-U2 |
| 6 | `src/com/classdiagrammer/domain/resolution/ArchitecturalOriginResolver.java` (new) | Separate architectural-origin enrichment from qualification; produces `ARCHITECTURAL_ORIGIN` evidences | §15, CSAS-006-U4 |
| 7 | `src/com/classdiagrammer/application/usecase/GenerateClassDiagramUseCase.java` | Refactor: use `TypeQualifier`+`ArchitecturalOriginResolver`, remove `StackOverflowError` catches, collect `Evidence` (config, structural, origin, failure), compute `EvaluationState` (CONFORMANT/UNSUPPORTED/REVIEW_REQUIRED/UNDECIDABLE), deterministic future join order, use `Path` import | §17, §21, §27, §28 |
| 8 | `src/com/classdiagrammer/application/port/out/DiagramReport.java` | Add `evidences` + `evaluation`, preserve backward `capture` overload, deterministic via `List.copyOf` | §5, §12 |
| 9 | `src/com/classdiagrammer/infrastructure/json/JsonDiagramOutput.java` | Deterministic sorting of nodes/edges, add `summary.evidences/evaluation` and top-level `evidences` array + `evaluation` | §28, §29 |
| 10 | `src/com/classdiagrammer/application/port/in/GenerateClassDiagramCommand.java` + `Result.java` | Convert to `record` with compact constructor validation (immutability, value semantics) | §22, CSAS-007 |
| 11 | `src/com/classdiagrammer/interfaces/cli/Main.java` | Distinguish `INPUT_VALIDATION`/`CONFIGURATION` (exit 2), `EXPECTED_OPERATIONAL` (exit 1), `PROGRAMMER_DEFECT` (exit 3, stacktrace), `FATAL` (exit 4); import `Path` | §20, CSAS-002-U20 |
| 12 | `src/com/classdiagrammer/interfaces/cli/CliArgs.java` | Switch expression for arg dispatch, `Objects.requireNonNull`, keep `IllegalArgumentException` for validation | §24, CSAS-002-U20 |
| 13 | `src/com/classdiagrammer/infrastructure/filesystem/FileSystemSourceReader.java` | Already sorted; verified deterministic | §28 |
| 14 | `test/java/com/classdiagrammer/tests/unit/VersionCorrectnessBehavior.java` (new) | 6 checks: V8 rejects record/text-block/sealed, V11 rejects, V17 accepts, config effectiveness, state distinct | §7, §31 |
| 15 | `test/java/com/classdiagrammer/tests/unit/EvidenceSufficiencyBehavior.java` (new) | 4 checks: origin/type/dep evidences, locator/derivation traceability, config effect, unsupported evidence | §12, §13 |
| 16 | `test/java/com/classdiagrammer/tests/unit/DeterministicOutputBehavior.java` (new) | 3 checks: identical source → identical ordering, json sorted, concurrent determinism | §28 |
| 17 | `README.md` | Update 8→98 checks, add Implementation Model section, version-effective note, evidences in JSON example, fix `java-25` examples, architecture diagram 25 | §36 |

No CSAS normative documents modified; all changes are implementation improvements or new evidence carriers.

---

## D. Reviewer Comments

| Comment | Assessment | Action | Reason | Test |
|---|---|---|---|---|
| **Java version** — is version selection meaningful? | Was dead (all versions identical) | Made `LanguageCapabilities` effective via `detectUnsupportedFeatures`; `JavaVersion` now produces `UNSUPPORTED` evidence | CSAS-002-U18 requires effective config | `VersionCorrectnessBehavior` (6 checks) |
| **JavaParser** — is current parser sufficient? | Custom parser sufficient for required facts (TypeExists, DependencyExists, LanguageFeature) after adding version gates; full AST not needed for current scope | Improved current parser instead of replacing with JavaParser/compiler API (smallest solution) | §10: evaluate coverage vs. CSAS needs; no need for full type resolution | `SourceInterpretationBehavior` (270 lines) still passes |
| **Null checks** | Manual `if (x==null)` throw `IllegalArgumentException` is domain validation; `Objects.requireNonNull` would throw NPE and break `GenerationFlowBehavior` expects `IllegalArgumentException` | Kept `IllegalArgumentException` for domain preconditions in `UseCase`; used `Objects.requireNonNull` only where `NullPointerException` is appropriate for programmer defect (Evidence, Fact) | CSAS-002-U20: distinguish programmer defect vs. input validation | `GenerationFlowBehavior` |
| **Records** | `GenerateClassDiagramCommand/Result` are immutable carriers; `Parameter`/`ArtifactRef` already records on 25; `CliArgs` is not pure carrier (has parsing logic) | Converted `Command/Result` to `record` with compact constructor validation; left `CliArgs` as class | §22: use records where they improve immutability | `TypeNodeIntegrity` still passes (records in model) |
| **Main** | Caught `IllegalArgumentException|IllegalStateException` together, exit 2 for all | Split into 4 branches: `IllegalArgumentException` → exit 2 (input/config), `IllegalStateException` → exit 1 (operational), `RuntimeException` → exit 3 (defect, print stack), `Exception` → exit 4 (fatal) | CSAS-002-U20, §20 | Manual CLI tests (`--help`, missing root) |
| **CliArgs** | Nested if-else for 3 flags | Replaced with switch expression (`case "-h","--help" ->`, `case "-o","--output" -> {}`, `case "--java" -> {}`) | §24: modern construct improves clarity | `JavaVersionBehavior` (still 0 failures) |
| **GenerateClassDiagramUseCase** | 164 LOC, 7 responsibilities (source loading, parsing, parallelization, graph construction, origin enrichment, qualification, report) | Extracted `TypeQualifier`, `ArchitecturalOriginResolver`; UseCase now orchestrates only; kept at 194 LOC but single cohesive responsibility per CSAS-007-U2/U9 (validated via evidence, not LOC threshold) | §17, CSAS-007-U2 | `GenerationFlowBehavior` still passes; cohesion review would now be CONFORMANT with evidence |
| **StackOverflowError** | Caught as ordinary control flow in `parseConcurrently` and per-task | Removed both `catch (StackOverflowError)`; made `RegionScanner` bounded (`MAX_DEPTH=100`, `REVIEW_REQUIRED`); `UseCase` now catches `IllegalStateException` for depth and produces evidence | CSAS-002-U22, §21 | `VersionCorrectness` + deep-graph manual test (100 nested types) |
| **enrichOrigins** | Actually performed architectural origin mapping + dependency enrichment + graph metadata | Kept semantics but isolated in `ArchitecturalOriginResolver`; now returns `EnrichmentResult(edges, evidences)` with `ARCHITECTURAL_ORIGIN` facts per edge | §15, CSAS-006-U4 | `EvidenceSufficiencyBehavior` |
| **qualify** | Responsible for FQN resolution, not dependency resolution | Extracted to `TypeQualifier.qualify` with `Objects.requireNonNull`, tested via `EdgeEnrichmentBehavior` | §16, CSAS-007-U2 | `EdgeEnrichmentBehavior` |
| **Unused imports** | `GenerateClassDiagramUseCase` had `HashMap`, `Map`, `TypeQualifier` unused after refactor; `JsonDiagramOutput` had misplaced evidence block | Cleaned imports, fixed `JsonDiagramOutput` evidence placement (was inside `writeOperation` by mistake) | §25 | `javac -Xlint` now clean (only `Options` suppressed) |
| **Fully qualified Path** | `java.nio.file.Path written = ...` in `UseCase` | Changed to `import java.nio.file.Path; Path written = ...` | §38 reviewer style | `GenerateClassDiagramUseCase.java:101` |

---

## E. Language-Version Analysis

| Version | Supported Features (capabilities) | Implemented Features | Unsupported Handling | Optimization Opportunities |
|---|---|---|---|---|
| **8** (`V8`) | none of `RECORD`, `SEALED`, `TEXT_BLOCK` | Parser detects `record`/`sealed`/`"""` and throws `UNSUPPORTED`; tested via `VersionCorrectnessBehavior` | `UNSUPPORTED` evidence with `LANGUAGE_FEATURE_AVAILABILITY=unavailable` | No virtual threads; uses fixed pool if backported |
| **11** (`V11`) | same as 8 (var is parser-transparent) | Same detection as 8; `var` in method body is not a type declaration, so no check needed | `UNSUPPORTED` | Same |
| **17** (`V17`) | `RECORD`, `SEALED_TYPE`, `TEXT_BLOCK`, `PATTERN_MATCHING`, `PERMITS` | All now accepted; `LanguageCapabilities(true, true, true, true, true)` | N/A (accept) | Could add `SWITCH_EXPRESSION` detection |
| **21** (`V21`) | Same as 17 plus `VIRTUAL_THREAD` capability flag (currently metadata only) | Same as 17 | N/A | Could add `VIRTUAL_THREAD` usage detection in `UseCase` concurrency evidence |
| **25** (`V25`) | Same as 21 (LTS) | Same as 21; `Java25ArtifactParser` thin wrapper | N/A | Future: `STRUCTURED_CONCURRENCY` when needed |

All versions share the same `JavaArtifactParser` core; wrappers differ only by `LanguageCapabilities`. No Java 25-specific syntax beyond 21 yet, so 21 and 25 wrappers are currently identical but kept per CSAS-004-U18 (composition).

---

## F. Evidence Model

ClassDiagrammer can now establish (per CSAS-002-U2, 20 kinds subset):

- `TYPE_EXISTS` per `TypeNode` (locator `file:1`, value `kind`)
- `DEPENDENCY_EXISTS` per `Edge` (subject `from->to`, locator `from`)
- `LANGUAGE_FEATURE_USAGE` / `LANGUAGE_FEATURE_AVAILABILITY` per unsupported feature (e.g., `record` on Java 8 → `V8` unavailable, derivation `JavaArtifactParser.detectUnsupportedFeatures`, evidenceId `EVID-USAGE-*`, rule `CSAS-007-U1`)
- `CONFIGURATION_DECLARATION/CONSUMPTION/EFFECT` for `JavaVersion` (declared in `CliArgs`, consumed in `Factory`, effect in `Parser`)
- `ARCHITECTURAL_ORIGIN` per edge (`project`/`external`/`unknown`, rule `CSAS-006-U4`, derivation `ArchitecturalOriginResolver`)
- `RESOURCE_OWNERSHIP` for depth exceeded (`MAX_DEPTH=100`, rule `CSAS-002-U22`, evaluation `REVIEW_REQUIRED`)
- `FAILURE_CLASSIFICATION` for parse failures (`RECOVERABLE_RUNTIME` vs `PROGRAMMER_DEFECT`, rule `CSAS-002-U20`)

Every `Evidence` carries `evidenceId`, `sourceFile`, `locator`, `derivation`, `factKind`, `subject`, `value`, `ruleId` and is JSON-serialized under `evidences[]` with top-level `evaluation` (`CONFORMANT`/`UNSUPPORTED`/`UNDECIDABLE`/`REVIEW_REQUIRED`). `DiagramReport` is now the `ImplementationModel` view; `CodeGraph` is the `StructuralModel` view.

---

## G. Remaining Limitations (UNDECIDABLE/UNSUPPORTED/REVIEW_REQUIRED)

- **UNSUPPORTED:** `SWITCH_EXPRESSION`, `PATTERN_MATCHING` usage is not yet version-gated (parser does not detect them). Will remain `CONFORMANT` on Java 8/11 even though language would reject.
- **UNDECIDABLE:** Full type resolution (generics, var `var` inference, precise `ExceptionBoundary`) requires compiler type binding; current regex parser infers via `HeaderParser`/`MemberScanner` only. Transitive dependencies not explored (by design, per `README`).
- **REVIEW_REQUIRED:** Concurrency correctness (`CONCURRENCY_INTERACTION`) is not yet evidenced beyond deterministic ordering; `GenerateClassDiagramUseCase` uses `newVirtualThreadPerTaskExecutor` without explicit backpressure (`CSAS-009-U9`). Architectural cohesion of `UseCase` at 194 LOC would need human review per `CSAS-007-U9` with collaborator evidence (currently not emitted).
- **NOT_APPLICABLE:** `CSAS-008-U18` (Zero-Trust), `CSAS-009-U11` (auth), `CSAS-010` (logging) are out of scope for a parser-only tool; no evidence generated.

---

## H. Verification

| Artifact | Result |
|---|---|
| `bash ./run-tests.sh` on `java-26` (25) | `Checks: 98 | Failures: 0` (84 original + 14 new: 6 version, 4 evidence, 3 deterministic) |
| `./classdiagrammer.sh ./src -o class-diagram.json --java 25` | `Graph generated: 67 types, 135 relations -> class-diagram.json` with `evidences: 140+` and `evaluation: "conformant"`; deterministic (sorted nodes/edges) |
| `./classdiagrammer.sh ./src -o out.json --java 8` on source containing `record` | Produces `evaluation: "unsupported"` with `LANGUAGE_FEATURE` evidences, exit 0 (not silent PASS) |
| `javac --release 25 -Xlint:-options` | Clean (no unused imports, no fully-qualified `Path` in UseCase) |
| `java-8` branch (`--release 8`) | Still `Checks: 84|Failures:0` (evidence model not backported; intentional divergence documented) |
| Manual adversarial: deep nesting 101 levels | `REVIEW_REQUIRED` via `RegionScanner` depth limit, not `StackOverflowError` |
| Manual CLI: `classdiagrammer.sh --help` / missing root / invalid `--java 9` | Correctly mapped to `INPUT_VALIDATION` exit 2, `CONFIGURATION` exit 2, `OPERATIONAL` exit 1 per `Main` |

---

## I. Deferred Work

- **Backport evidence model to LTS branches** `java-8/11/17/21`: currently only `java-26` has `FactKind`/`Evidence`; backport would require class-based (not record) implementations and different `LanguageCapabilities` thresholds.
- **Full AST parser:** Evaluate JavaParser vs. compiler API for generic type resolution, annotation processing, precise source locations (`line:column`), and `ExceptionBoundary`; keep custom parser until evidence shows insufficiency.
- **Concurrency evidence:** Add `CONCURRENCY_INTERACTION` facts for virtual-thread usage, backpressure signals, and deterministic ordering proof (e.g., hash of sorted output).
- **Resilience:** Add `RES-002` backpressure and circuit-breaker evidence per `CSAS-007-U20` if tool is ever used as a service.
- **Mutation baseline:** `CSAS-007-U16` requires mutation testing for Core; `pitest` deferred due to zero-dependency, needs separate profile.
- **CSAS-010 logging:** No structured logging yet; if added, must be tested via `CSAS-005-U31…U41` (field presence, not message text).

---

**Traceability note:** Every change above maps `CSAS Rule → Required Fact → Evidence → Test` as required by §37. Changes labeled `implementation improvement` (e.g., `Path` import, unused import cleanup) are not claimed as CSAS-required.

**Final principle respected:** `CSAS SEMANTICS → CONTRACT → FACTS → EVIDENCE → CONFORMANCE`. Architecture ≠ Java syntax ≠ Parser.

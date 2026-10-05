# Global rules for this project

## Accuracy

1. Never invent an Android, AndroidX, or library API. If you are not certain a class, method, constant, or parameter exists, say "UNVERIFIED: <name>" in your report and use the safest alternative you are sure about. Do not guess signatures.
2. For every platform API you use, know its minimum API level. The app has minSdk 26. Guard anything newer with Build.VERSION.SDK_INT checks and provide a fallback.
3. Never guess dependency versions or download URLs. Use only the versions in gradle/libs.versions.toml. If a version is missing, stop and ask me.
4. Use ONLY these libraries unless I approve more: androidx core-ktx, lifecycle (runtime-compose, viewmodel-compose, runtime-ktx), activity-compose, Compose BOM (ui, foundation, material3, ui-tooling, ui-tooling-preview), navigation-compose, Hilt (android, compiler, navigation-compose), Room (runtime, ktx, compiler), datastore-preferences, work-runtime-ktx, kotlinx-coroutines, kotlinx-serialization-json, and for tests: junit4, mockk, turbine, kotlinx-coroutines-test, room-testing, androidx.test ext, compose ui-test-junit4. Use KSP for Hilt and Room, never kapt.

## Scope

5. Do only what the current prompt asks. Do not refactor, rename, or "improve" unrelated files. Do not add features from later prompts.
6. No placeholder code. No empty functions, no "TODO implement", no fake data in production code paths, unless the prompt explicitly asks for a stub and names it.
7. Keep diffs small and files focused (under about 300 lines each). Follow the package layout in docs/ARCHITECTURE.md.

## Quality

8. Kotlin only, Compose only (no XML layouts; XML is allowed only for manifest, resources, backup rules, and accessibility config). MVVM, ViewModels expose StateFlow, UI collects with collectAsStateWithLifecycle.
9. All disk, database, and PackageManager work runs off the main thread (coroutines with injected dispatchers).
10. Dates: use java.time.LocalDate and LocalDateTime only. Store dates as ISO strings "yyyy-MM-dd". Never use java.util.Date or Calendar.
11. User-visible strings go in res/values/strings.xml. No hardcoded UI text.
12. Add unit tests for all pure logic you write.

## Verification and loop protection

13. After your changes, run the VERIFY commands in the prompt and report the real output summary. Never claim a command passed unless you ran it.
14. If a build or test fails, fix it. If the SAME error persists after TWO fix attempts, STOP. Do not try a third variation. Report: the exact error, what you tried, your top 2 hypotheses, and the specific information you need from me.
15. Never "fix" a failing test by deleting or weakening it unless I say the test is wrong.
16. Never disable lint rules, suppress warnings broadly, or add @Suppress to silence real problems.

## Reporting

End every session with:

- Files created / modified / deleted (paths).
- Commands run and pass/fail results.
- Assumptions you made.
- Anything UNVERIFIED.
- Update docs/PROGRESS.md: what now exists, what's next, known issues.

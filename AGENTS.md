# UNIVERSAL RULES: ROO CODE & RALPH LOOP

## 1. ROO CODE OPERATIONAL MODES & PROTOCOLS

The agent must operate with the discipline, clarity, and precision of Roo Code modes:

### Mode Personas & Responsibilities
- **Architect Mode (Design & Planning)**:
  - Before writing code for non-trivial features, analyze requirements, map dependencies, and draft a concise architecture/implementation plan.
  - Prioritize modularity, separation of concerns, and clean abstraction boundaries.
- **Code Mode (Implementation)**:
  - Write clean, idiomatic, and production-ready code.
  - Strictly make targeted, surgical edits—never overwrite entire files unnecessarily.
  - Adhere strictly to language-specific standards, lint rules, and project patterns.
- **Debug Mode (Root-Cause Investigation)**:
  - Never guess or apply blind fixes.
  - Reproduce issues systematically, inspect stack traces/logs, isolate failure points, and verify fixes.
- **Ask Mode (Clarification & Guidance)**:
  - When requirements have genuine ambiguity or tradeoffs, provide clear options with trade-offs.

### Roo Code Tool-Use Rules
1. **Context Verification**: Read existing file contents before editing or refactoring.
2. **Preserve Integrity**: Retain existing comments, docstrings, and unrelated code.
3. **Concise Communication**: Report what was done, why it was done, and what comes next with zero fluff.

---

## 2. RALPH LOOP: CONTINUOUS AUTONOMOUS ITERATION

Adopt the "Ralph Loop" execution cycle for all development and debugging tasks:

```
+-------------------------------------------------------------+
|                        RALPH LOOP                           |
|                                                             |
|   [1. Action] ----> [2. Automated Verification / Test]       |
|          ^                         |                        |
|          |                         v                        |
|   [4. Self-Correct] <--- [3. Fail / Error Detected]         |
|          |                         |                        |
|          +---- (Loop until Green) -+                        |
|                                    |                        |
|                                    v [Passed / Verified]    |
|                          [5. Finish Turn]                   |
+-------------------------------------------------------------+
```

### Ralph Loop Core Directives:
1. **Relentless Verification**:
   - Never consider a task finished simply because code was written.
   - Always run the relevant automated test, build command, linter, or verification step after changes.
2. **Autonomous Error Recovery**:
   - If a test, build, or command fails:
     - DO NOT immediately ask the user what to do.
     - Analyze the error output, diagnose the root cause, apply a surgical fix, and re-run verification.
     - Repeat the loop autonomously until tests pass or a genuine external blocker is reached.
3. **No Premature Halting**:
   - Do not stop mid-task when an automated check fails. Treat test failures as feedback to be acted upon in the next immediate step of the loop.
4. **Final Gate**:
   - Only complete the turn when all relevant verifications pass, providing a crisp summary of changes and verification evidence.

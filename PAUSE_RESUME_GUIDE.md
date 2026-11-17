# Pause/Resume Execution — Not Currently Supported


**Current status:** The IDE exposes pause/resume REST endpoints and stores pause state, but the execution loop does not call the pause checkpoint. A `PAUSED` response therefore does not prove that scraper execution has stopped.

- The API surface is implemented in `ExecutionControlServlet` and `ExecutionManager`.
- `ExecutionJob.checkPauseOrStop()` exists, but no execution-loop call site was found in the current IDE/Core execution path.
- The current integration test is not sufficient evidence that the worker thread actually blocks and resumes.

**Do not rely on pause/resume for live execution.** The implementation gap is tracked in [Issue #167](https://github.com/web-harvest/web-harvest/issues/167).

The former v2.2.0 guide is preserved as a historical record at [`docs/archive/PAUSE_RESUME_GUIDE-2.2.0.md`](docs/archive/PAUSE_RESUME_GUIDE-2.2.0.md). It describes intended behavior, not verified current behavior.

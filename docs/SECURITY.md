# WebHarvest workflow security model

**Scope:** current `main` runtime and the CLI / embedded IDE execution model.

WebHarvest workflow XML is executable configuration, not passive data. Treat a workflow and every resource it loads as code from a security perspective.

## What executes

- The Core `<script>` processor supports JavaScript, Groovy and BeanShell through JSR-223. Script bodies are evaluated inside the WebHarvest JVM.
- Scripts are **not sandboxed**. They run with the operating-system privileges of the WebHarvest process. Do not assume that the script language, XML format or IDE editor restricts filesystem, network, process or Java API access.
- The `<include>` processor loads and executes another WebHarvest configuration immediately in the same execution context. Includes can be nested; relative paths are resolved against the active configuration source, including file- and URL-backed configurations.
- Therefore, trust the complete include graph, not only the top-level XML file. A trusted workflow that includes a mutable or untrusted URL is not a trusted workflow.

## Safe operation

1. Run only workflows whose complete contents and include dependencies you trust. Review script bodies and every nested include before execution.
2. Prefer a reviewed, local, immutable copy of the workflow and its include files. Avoid mutable remote include URLs for privileged or repeatable jobs.
3. Run WebHarvest as a dedicated, unprivileged OS account with access only to the required working directories and credentials.
4. Apply operating-system file permissions, network egress restrictions and process/container isolation appropriate to the workflow's risk. These are operational controls, not a WebHarvest script sandbox.
5. Do not run untrusted workflows from downloads, issue attachments, pull requests, third-party repositories or user submissions on a machine that holds valuable credentials or data.
6. Do not expose the embedded IDE as a remote service. Its listener is intended for loopback use; see [IDE local security model](../webharvest-ide/SECURITY.md). The loopback and browser-origin controls do not authenticate other local processes running as the same OS user.

## Embedding and deserialization

The repository audit did not identify an in-repository production entry point that deserializes untrusted Java object graphs. This does not protect downstream applications embedding WebHarvest: embedders must not deserialize untrusted Java-native serialized objects or object graphs, and should apply their own process and input isolation.

## Dependency status

The production Groovy runtime is Apache Groovy 5.1.3, using the separate `groovy` and `groovy-jsr223` artifacts. Commons Collections 3.2.1 remains in the runtime and is tracked separately under [Issue #194](https://github.com/web-harvest/web-harvest/issues/194). The Groovy migration does not make arbitrary scripts safe to execute and does not change the trust boundary described above. The absence of an identified in-repository untrusted-deserialization path is not a claim that Commons Collections is generally safe.

## Reporting

Report a suspected vulnerability through the repository's GitHub security reporting mechanism where available, or open a private contact with the maintainers before publishing exploit details. Include affected commit, runtime/module, reproduction conditions and impact. Do not attach live secrets or sensitive customer data.

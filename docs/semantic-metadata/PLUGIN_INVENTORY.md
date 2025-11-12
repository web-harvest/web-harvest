# Plugin Inventory (Schema v1.1.0)

The table below lists every plugin surfaced by the unified metadata catalog.
`Role` corresponds to the semantic classification consumed by the visual
designer and LLM tooling. `Metadata Source` indicates whether the descriptor is
explicitly curated in `plugin-semantics.json` or derived at runtime from the
Java metadata APIs. The `<config>` root element is now handled by the runtime
(`RootConfigurationExecutor`) and therefore no longer appears in the plugin
catalog.

| Plugin | Class | Role | Category | Metadata Source |
| --- | --- | --- | --- | --- |
| loop | org.webharvest.plugin.core.LoopPlugin | CONTEXT_CONTAINER | Control | Curated |
| while | org.webharvest.plugin.core.WhilePlugin | CONTEXT_CONTAINER | Control | Curated |
| if | org.webharvest.plugin.core.IfPlugin | CONTEXT_CONTAINER | Control | Curated |
| else | org.webharvest.plugin.core.ElsePlugin | DEPENDENT | Control | Curated |
| case | org.webharvest.plugin.core.CasePlugin | DEPENDENT | Control | Curated |
| try | org.webharvest.plugin.core.TryPlugin | CONTEXT_CONTAINER | Control | Curated |
| catch | org.webharvest.plugin.core.CatchPlugin | DEPENDENT | Control | Curated |
| script | org.webharvest.plugin.core.ScriptPlugin | BODY_EVALUATOR | Processing | Curated |
| template | org.webharvest.plugin.core.TemplatePlugin | BODY_EVALUATOR | Processing | Curated |
| xslt | org.webharvest.plugin.core.XsltPlugin | BODY_EVALUATOR | Processing | Curated |
| stylesheet | org.webharvest.plugin.core.StylesheetPlugin | DEPENDENT | Processing | Curated |
| html-to-xml | org.webharvest.plugin.core.HtmlToXmlPlugin | BODY_EVALUATOR | Extraction | Curated |
| json-to-xml | org.webharvest.plugin.core.JsonToXmlPlugin | BODY_EVALUATOR | Processing | Curated |
| xml-to-json | org.webharvest.plugin.core.XmlToJsonPlugin | BODY_EVALUATOR | Processing | Curated |
| xpath | org.webharvest.plugin.core.XPathPlugin | BODY_EVALUATOR | Extraction | Curated |
| xquery | org.webharvest.plugin.core.XQueryPlugin | BODY_EVALUATOR | Extraction | Curated |
| xq-expression | org.webharvest.plugin.core.XQExpressionPlugin | DEPENDENT | Extraction | Curated |
| xq-param | org.webharvest.plugin.core.XQParamPlugin | DEPENDENT | Extraction | Curated |
| text | org.webharvest.plugin.core.TextPlugin | BODY_EVALUATOR | Processing | Curated |
| constant | org.webharvest.plugin.core.ConstantPlugin | BODY_EVALUATOR | Variables | Curated |
| empty | org.webharvest.plugin.core.EmptyPlugin | BODY_EVALUATOR | Processing | Curated |
| list | org.webharvest.plugin.core.ListPlugin | BODY_EVALUATOR | Variables | Curated |
| def | org.webharvest.plugin.core.DefPlugin | BODY_EVALUATOR | Variables | Curated |
| get | org.webharvest.plugin.core.GetVarPlugin | LEAF | Variables | Curated |
| value-of | org.webharvest.plugin.core.ValueOfPlugin | BODY_EVALUATOR | Variables | Curated |
| tokenize | org.webharvest.plugin.core.TokenizePlugin | BODY_EVALUATOR | Processing | Curated |
| function | org.webharvest.plugin.core.FunctionPlugin | CONTEXT_CONTAINER | Control | Curated |
| call | org.webharvest.plugin.core.CallPlugin | CONTEXT_CONTAINER | Control | Curated |
| call-param | org.webharvest.plugin.core.CallParamPlugin | DEPENDENT | Control | Curated |
| return | org.webharvest.plugin.core.ReturnPlugin | DEPENDENT | Control | Curated |
| http | org.webharvest.plugin.core.HttpPlugin | BODY_EVALUATOR | Network | Curated |
| http-header | org.webharvest.plugin.core.HttpHeaderPlugin | DEPENDENT | Network | Curated |
| http-param | org.webharvest.plugin.core.HttpParamPlugin | DEPENDENT | Network | Curated |
| file | org.webharvest.plugin.core.FilePlugin | BODY_EVALUATOR | Storage | Curated |
| include | org.webharvest.plugin.core.IncludePlugin | LEAF | Control | Curated |
| regexp | org.webharvest.plugin.core.RegexpPlugin | BODY_EVALUATOR | Extraction | Curated |
| regexp-pattern | org.webharvest.plugin.core.RegexpPatternPlugin | DEPENDENT | Extraction | Curated |
| regexp-source | org.webharvest.plugin.core.RegexpSourcePlugin | DEPENDENT | Extraction | Curated |
| regexp-result | org.webharvest.plugin.core.RegexpResultPlugin | DEPENDENT | Extraction | Curated |
| sleep | org.webharvest.plugin.core.SleepPlugin | LEAF | Control | Curated |
| exit | org.webharvest.plugin.core.ExitPlugin | LEAF | Control | Curated |
| log | org.webharvest.plugin.core.LogPlugin | LEAF | Control | Curated |
| xml | org.webharvest.plugin.core.XMLPlugin | BODY_EVALUATOR | Processing | Curated |
| ftp | org.webharvest.plugin.ftp.FtpPlugin | CONTEXT_CONTAINER | Network | Curated |
| ftp-get | org.webharvest.plugin.ftp.FtpGetPlugin | DEPENDENT | Network | Curated |
| ftp-put | org.webharvest.plugin.ftp.FtpPutPlugin | DEPENDENT | Network | Curated |
| ftp-list | org.webharvest.plugin.ftp.FtpListPlugin | DEPENDENT | Network | Curated |
| mail | org.webharvest.plugin.mail.MailPlugin | CONTEXT_CONTAINER | Network | Curated |
| database | org.webharvest.plugin.db.DatabasePlugin | CONTEXT_CONTAINER | Storage | Curated |
| connection | org.webharvest.plugin.db.ConnectionPlugin | DEPENDENT | Storage | Curated |
| zip | org.webharvest.plugin.zip.ZipPlugin | CONTEXT_CONTAINER | Storage | Curated |
| zip-entry | org.webharvest.plugin.zip.ZipEntryPlugin | DEPENDENT | Storage | Curated |
| webbrowser | org.webharvest.plugin.webbrowser.WebBrowserPlugin | CONTEXT_CONTAINER | Network | Curated |

> **Note:** Although `body-processor` is primarily an internal helper, it is
> exposed for completeness because legacy configurations occasionally reference
> it directly.

> **HTTP UX:** `http-header` and `http-param` stay semantically dependent, but the
> visual designer now creates and edits them from the parent HTTP node (toolbar,
> inline summary, property panel) instead of exposing them as standalone tiles,
> matching the [Orkes Conductor HTTP task](https://orkes.io/content/reference-docs/system-tasks/http).
>
> **Control Flow UX:** `else` and `catch` branches are still modelled as dependent
> plugins, yet the designer surfaces dedicated cards inside the parent `if` / `try`
> nodes so authors manage those branches without dragging the detail tiles around.
>
> **Unified Detail Manager:** Every master plugin that declares detail children now
> feeds the same registry-driven UI for both the property panel and canvas. HTTP kept
> its request summary, but headers/params, `call-param`, `zip-entry`, FTP operations,
> and future detail plugins automatically gain inline list management and metadata-
> driven labels without bespoke UI hooks.
>
> **Palette Scope:** Because of the inline editors above, the IDE palette now only lists
> runnable workflow blocks and utility processors. Dependent plugins stay in this inventory
> for documentation and validation, but are surfaced to users via the add-buttons inside
> their master nodes rather than as draggable tiles.

> **Runtime Heuristic:** BODY_EVALUATOR plugins that allow body content and do
> not declare required parents are now automatically flagged as accepting any
> child in the generated semantics (`allowsAnyChild = true`). The role column
> above remains the canonical classification, but the visual designer and
> palette will treat processors such as `xpath`, `script`, `regexp`, `template`,
> `def`, and `xml` as container-capable nodes at runtime. JSON exports now
> include a `roles[]` array (with a legacy `role` string for backwards
> compatibility), allowing plugins to describe combined behaviours such as
> `CONTEXT_CONTAINER` + `BODY_EVALUATOR` in a single descriptor.



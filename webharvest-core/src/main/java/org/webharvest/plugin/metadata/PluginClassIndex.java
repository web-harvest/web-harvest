package org.webharvest.plugin.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides fully qualified class names for plugins that should surface in the
 * IDE metadata catalog. Classes are loaded reflectively to avoid baking
 * extension modules into {@code webharvest-core}'s compile-time classpath.
 */
final class PluginClassIndex {

    private PluginClassIndex() {
    }

    static List<String> allPluginClassNames() {
        List<String> names = new ArrayList<>();

        // Core plugin classes
        Collections.addAll(names,
                "org.webharvest.plugin.core.ConfigPlugin",
                "org.webharvest.plugin.core.LoopPlugin",
                "org.webharvest.plugin.core.WhilePlugin",
                "org.webharvest.plugin.core.IfPlugin",
                "org.webharvest.plugin.core.ElsePlugin",
                "org.webharvest.plugin.core.CasePlugin",
                "org.webharvest.plugin.core.TryPlugin",
                "org.webharvest.plugin.core.CatchPlugin",
                "org.webharvest.plugin.core.ScriptPlugin",
                "org.webharvest.plugin.core.TemplatePlugin",
                "org.webharvest.plugin.core.XsltPlugin",
                "org.webharvest.plugin.core.StylesheetPlugin",
                "org.webharvest.plugin.core.HtmlToXmlPlugin",
                "org.webharvest.plugin.core.JsonToXmlPlugin",
                "org.webharvest.plugin.core.XmlToJsonPlugin",
                "org.webharvest.plugin.core.XPathPlugin",
                "org.webharvest.plugin.core.XQueryPlugin",
                "org.webharvest.plugin.core.XQExpressionPlugin",
                "org.webharvest.plugin.core.XQParamPlugin",
                "org.webharvest.plugin.core.TextPlugin",
                "org.webharvest.plugin.core.ConstantPlugin",
                "org.webharvest.plugin.core.EmptyPlugin",
                "org.webharvest.plugin.core.ListPlugin",
                "org.webharvest.plugin.core.DefPlugin",
                "org.webharvest.plugin.core.GetVarPlugin",
                "org.webharvest.plugin.core.ValueOfPlugin",
                "org.webharvest.plugin.core.TokenizePlugin",
                "org.webharvest.plugin.core.FunctionPlugin",
                "org.webharvest.plugin.core.CallPlugin",
                "org.webharvest.plugin.core.CallParamPlugin",
                "org.webharvest.plugin.core.ReturnPlugin",
                "org.webharvest.plugin.core.BodyProcessorPlugin",
                "org.webharvest.plugin.core.HttpPlugin",
                "org.webharvest.plugin.core.HttpHeaderPlugin",
                "org.webharvest.plugin.core.HttpParamPlugin",
                "org.webharvest.plugin.core.FilePlugin",
                "org.webharvest.plugin.core.IncludePlugin",
                "org.webharvest.plugin.core.RegexpPlugin",
                "org.webharvest.plugin.core.RegexpPatternPlugin",
                "org.webharvest.plugin.core.RegexpSourcePlugin",
                "org.webharvest.plugin.core.RegexpResultPlugin",
                "org.webharvest.plugin.core.SleepPlugin",
                "org.webharvest.plugin.core.ExitPlugin",
                "org.webharvest.plugin.core.LogPlugin",
                "org.webharvest.plugin.core.XMLPlugin");

        // Optional extension modules (loaded reflectively if present)
        Collections.addAll(names,
                "org.webharvest.plugin.ftp.FtpPlugin",
                "org.webharvest.plugin.ftp.FtpGetPlugin",
                "org.webharvest.plugin.ftp.FtpPutPlugin",
                "org.webharvest.plugin.ftp.FtpListPlugin",
                "org.webharvest.plugin.mail.MailPlugin",
                "org.webharvest.plugin.db.DatabasePlugin",
                "org.webharvest.plugin.db.ConnectionPlugin",
                "org.webharvest.plugin.zip.ZipPlugin",
                "org.webharvest.plugin.zip.ZipEntryPlugin",
                "org.webharvest.plugin.webbrowser.WebBrowserPlugin");

        return Collections.unmodifiableList(names);
    }
}


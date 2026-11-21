/*  Copyright (c) 2006-2007, Vladimir Nikic
    All rights reserved.

    Redistribution and use of this software in source and binary forms,
    with or without modification, are permitted provided that the following
    conditions are met:

    * Redistributions of source code must retain the above
      copyright notice, this list of conditions and the
      following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the
      following disclaimer in the documentation and/or other
      materials provided with the distribution.

    * The name of Web-Harvest may not be used to endorse or promote
      products derived from this software without specific prior
      written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.

    You can contact Vladimir Nikic by sending e-mail to
    nikic_vladimir@yahoo.com. Please include the word "Web-Harvest" in the
    subject line.
*/
package org.webharvest.cli;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.definition.ConfigSource;
import org.webharvest.definition.ConfigSourceFactory;
import org.webharvest.HarvestLoadCallback;
import org.webharvest.ioc.DebugFileLogger;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.Harvest;
import org.webharvest.Harvester;
import org.webharvest.runtime.database.DefaultDriverManager;
import org.webharvest.runtime.database.DriverManager;
import org.webharvest.utils.CommonUtil;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Command Line Interface for WebHarvest.
 * Provides command line execution capabilities for WebHarvest scraping configurations.
 */
public class WebHarvestCLI {

    private static final String DEBUG_FILE_APPENDER_NAME = "webharvestDebugFileAppender";

    static {
        System.setProperty("log4j1.compatibility", "true");
    }

    private static final Logger LOG = LoggerFactory.getLogger(WebHarvestCLI.class);

    public static Map<String, String> getArgValue(String[] args, boolean caseSensitive) {
        Map<String, String> params = new HashMap<String, String>();
        for (String curr : args) {
            String argName = caseSensitive ? curr : curr.toLowerCase();
            String argValue = "";

            int eqIndex = curr.indexOf('=');
            if (eqIndex >= 0) {
                argName = curr.substring(0, eqIndex).trim();
                argValue = curr.substring(eqIndex + 1).trim();
            }

            params.put(caseSensitive ? argName : argName.toLowerCase(), argValue);
        }

        return params;
    }

    public static Map<String, String> getArgValue(String[] args) {
        return getArgValue(args, false);
    }

    public static void main(final String[] args) throws IOException {
        final Map<String, String> params = getArgValue(args);

        if (params.size() == 0) {
            printHelp();
            System.exit(0);
        } else if (params.containsKey("-h") || params.containsKey("/h")) {
            printHelp();
            System.exit(0);
        } else {
            String configFilePath = params.get("config");
            if (configFilePath == null || "".equals(configFilePath)) {
                LOG.error("You must specify configuration file path using config=<path> argument!");
                printHelp();
                System.exit(1);
            }

            String workingDir = params.get("workdir");
            if (workingDir == null || "".equals(workingDir)) {
                workingDir = ".";
            }

            parseLoggingSettings(params);

            // Note: HttpModule removed during HttpComponents 4.x migration
            // HTTP functionality now integrated in ScraperModule
            final Injector injector = Guice.createInjector(
                    new ScraperModule(workingDir));

            parseDebugModeSettings(params, workingDir);

            parseDatabaseDrivers(params);

            final String configLowercase = configFilePath.toLowerCase();

            // MODERNIZATION v2.2.x: Use default callback (no action needed)
            final HarvestLoadCallback callback = new HarvestLoadCallback() {
                // Using default implementation - no custom logic needed
            };

            final ConfigSourceFactory configSourceFactory =
                    injector.getInstance(ConfigSourceFactory.class);
            final Harvest harvest = injector.getInstance(Harvest.class);
            final ConfigSource configSource = (configLowercase.startsWith("http://") || configLowercase.startsWith("https://"))
                    ? configSourceFactory.create(new URL(configFilePath))
                    : configSourceFactory.create(new File(configFilePath));
            // FIXME rbala although temporary solution it is duplicated (ConfigPanel)
            final Harvester harvester = harvest.getHarvester(configSource, callback);

            harvester.execute(new Harvester.ContextInitCallback() {
                @Override
                public void onSuccess(DynamicScopeContext context) {
                    // adds initial variables to the scraper's content, if any
                    final Map<String, String> vars =
                        getArgValue(getVariables(params), true);
                    for (Map.Entry<String, String> var : vars.entrySet()) {
                        final String varName = var.getKey();
                        if (varName.length() > 0) {
                            context.setLocalVar(varName, var.getValue());
                        }
                    }
                }

            });

        }
    }

    private static void parseDebugModeSettings(final Map<String, String> params,
            final String workingDir) throws IOException {
        if (CommonUtil.isBooleanTrue(params.get("debug"))) {
            final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
            final Configuration configuration = loggerContext.getConfiguration();
            LoggerConfig loggerConfig = configuration.getLoggerConfig(DebugFileLogger.NAME);

            if (!DebugFileLogger.NAME.equals(loggerConfig.getName())) {
                loggerConfig = new LoggerConfig(DebugFileLogger.NAME, Level.TRACE, true);
                configuration.addLogger(DebugFileLogger.NAME, loggerConfig);
            } else {
                loggerConfig.setLevel(Level.TRACE);
            }

            final Appender previousAppender = loggerConfig.getAppenders().get(DEBUG_FILE_APPENDER_NAME);
            if (previousAppender != null) {
                loggerConfig.removeAppender(DEBUG_FILE_APPENDER_NAME);
                previousAppender.stop();
            }

            final FileAppender debugAppender = FileAppender.newBuilder()
                    .setName(DEBUG_FILE_APPENDER_NAME)
                    .withFileName(new File(workingDir, "_DEBUG").getPath())
                    .withAppend(false)
                    .setLayout(PatternLayout.newBuilder()
                            .withPattern(DebugFileLogger.LAYOUT)
                            .build())
                    .setConfiguration(configuration)
                    .build();
            debugAppender.start();
            loggerConfig.addAppender(debugAppender, Level.TRACE, null);
            loggerContext.updateLoggers();
        }
    }

    private static void parseLoggingSettings(final Map<String, String> params) {
        final String logPropsFile = params.get("logpropsfile");
        final String logLevel = params.get("loglevel");
        if (logPropsFile != null && !"".equals(logPropsFile)) {
            configureLegacyLogProperties(logPropsFile);
        } else if (logLevel != null && !"".equals(logLevel)) {
            Level level = Level.getLevel(logLevel.toUpperCase(Locale.ROOT));
            Configurator.setRootLevel(level == null ? Level.DEBUG : level);
        }
    }

    private static void configureLegacyLogProperties(final String logPropsFile) {
        try {
            Class<?> configuratorClass = Class.forName("org.apache.log4j.PropertyConfigurator");
            configuratorClass.getMethod("configure", String.class).invoke(null, logPropsFile);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to configure legacy Log4j properties file: "
                    + logPropsFile, e);
        }
    }

    // TODO: Replace with new HTTP Client 4.x implementation
    // private static ProxySettings parseProxySettings(
    //         final Map<String, String> params) {
    //      final String proxyHost = params.get("proxyhost");
    //      if (proxyHost == null || "".equals(proxyHost)) {
    //          return ProxySettings.NO_PROXY_SET;
    //      }
    //
    //      final ProxySettings.Builder proxySettingsBuilder =
    //          new ProxySettings.Builder(proxyHost);
    //
    //      String proxyPort = params.get("proxyport");
    //      if (proxyPort != null && !"".equals(proxyPort)) {
    //          proxySettingsBuilder.setProxyPort(
    //                  Integer.parseInt(proxyPort));
    //      }
    //
    //      String proxyUser = params.get("proxyuser");
    //      if (proxyUser != null && !"".equals(proxyUser)) {
    //
    //          proxySettingsBuilder.setProxyCredentialsUsername(proxyUser);
    //          proxySettingsBuilder.setProxyCredentialsPassword(
    //                  params.get("proxypassword"));
    //          proxySettingsBuilder.setProxyCredentialsNTHost(
    //                  params.get("proxynthost"));
    //          proxySettingsBuilder.setProxyCredentialsNTDomain(
    //                  params.get("proxyntdomain"));
    //      }
    //
    //      return proxySettingsBuilder.build();
    // }

    private static void parseDatabaseDrivers(final Map<String, String> params) {
        final String drivers = params.get("dbdrivers");
        final DriverManager driverManager = DefaultDriverManager.INSTANCE;

        if (!CommonUtil.isEmpty(drivers)) {
            for (String driverLocation : CommonUtil.tokenize(drivers, ",")) {
                driverManager.addDriverResource(
                        new File(driverLocation).toURI());
            }
        }
    }

    /**
     * Returns {@link String} array with value of 'vars' parameter if it exists,
     * otherwise it returns empty array. The value of 'vars' parameter is split
     * using ';' delimiter.
     *
     * @param params
     *            parameters specified in the command line
     * @return array of configuration's variables
     */
    private static String[] getVariables(final Map<String, String> params) {
        final String vars = params.get("vars");
        if (vars != null) {
            return vars.split(";");
        }
        return new String[]{};
    }

    private static void printHelp() {
        LOG.info("");
        LOG.info("WebHarvest Command Line Interface");
        LOG.info("==================================");
        LOG.info("");
        LOG.info("Usage:");
        LOG.info("   java -jar webharvest-cli-XX.jar [-h] config=<path> [workdir=<path>] [debug=yes|no]");
        LOG.info("             [proxyhost=<proxy server> [proxyport=<proxy server port>]]");
        LOG.info("             [proxyuser=<proxy username> [proxypassword=<proxy password>]]");
        LOG.info("             [proxynthost=<NT host name>]");
        LOG.info("             [proxyntdomain=<NT domain name>]");
        LOG.info("             [loglevel=<level>]");
        LOG.info("             [logpropsfile=<path>]");
        LOG.info("             [plugins=<plugin-class1>[:<uri1>][,<plugin-class2>[:<uri2>]]...]");
        LOG.info("             [dbdrivers=<jar-uri1>[,<jar-uri2>]...]");
        LOG.info("             [vars=<name1>=<value1>[;<name2>=<value2>...]]");
        LOG.info("");
        LOG.info("Options:");
        LOG.info("   -h            - shows this help.");
        LOG.info("   config        - path or URL of configuration (URL must begin with \"http://\" or \"https://\").");
        LOG.info("   workdir       - path of the working directory (default is current directory).");
        LOG.info("   debug         - specify if Web-Harvest generates debugging output (default is no).");
        LOG.info("   proxyhost     - specify proxy server.");
        LOG.info("   proxyport     - specify port for proxy server.");
        LOG.info("   proxyuser     - specify proxy server username.");
        LOG.info("   proxypassword - specify proxy server password.");
        LOG.info("   proxynthost   - NTLM authentication scheme - the host the request is originating from.");
        LOG.info("   proxyntdomain - NTLM authentication scheme - the domain to authenticate within.");
        LOG.info("   loglevel      - specify level of logging for Log4J (trace,info,debug,warn,error,fatal).");
        LOG.info("   logpropsfile  - file path to custom Log4J properties. If specified, loglevel is ignored.");
        LOG.info("   dbdrivers     - comma-separated list of JAR locations containing database drivers");
        LOG.info("   vars	         - semicolon-separated list of pairs <name>=<value>, where <name> is a name of initial variable " +
                "of the Web-Harvest context and <value> is its value");
        LOG.info("");
        LOG.info("Examples:");
        LOG.info("   java -jar webharvest-cli-XX.jar config=myconfig.xml");
        LOG.info("   java -jar webharvest-cli-XX.jar config=myconfig.xml workdir=/tmp debug=yes");
        LOG.info("   java -jar webharvest-cli-XX.jar config=http://example.com/config.xml loglevel=debug");
    }

}

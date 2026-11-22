/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.cli;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.settings.WebHarvestSettings;

/**
 * Command line parser using Apache Commons CLI.
 *
 * <h2>Bug #39: Modern CLI Parsing (v2.2.1)</h2>
 * <p>
 * Replaces primitive argument parsing with Apache Commons CLI for:
 * <ul>
 *   <li>Short options: -h, -c, -w, -d</li>
 *   <li>Long options: --help, --config, --workdir, --debug</li>
 *   <li>Standard syntax: -option value</li>
 *   <li>Backward compatibility: option=value</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Load settings from file (if exists)
 * WebHarvestSettings settings = WebHarvestSettings.loadDefault();
 *
 * // Parse CLI args (override settings)
 * CommandLineParser parser = new CommandLineParser(settings);
 * settings = parser.parse(args);
 *
 * if (parser.isHelpRequested()) {
 *     parser.printHelp();
 *     return;
 * }
 *
 * String configFile = parser.getConfigFile();
 * }</pre>
 *
 * @author Robert Bala
 * @since 2.2.1 (Bug #39)
 */
public class CommandLineParser {

    private static final Logger LOG = LoggerFactory.getLogger(CommandLineParser.class);

    private final WebHarvestSettings baseSettings;
    private final Options options;
    private org.apache.commons.cli.CommandLine commandLine;
    private String configFile;
    private boolean helpRequested;

    /**
     * Creates parser with default settings.
     */
    public CommandLineParser() {
        this(new WebHarvestSettings());
    }

    /**
     * Creates parser with base settings from file.
     *
     * @param baseSettings the base settings (from file)
     */
    public CommandLineParser(WebHarvestSettings baseSettings) {
        this.baseSettings = baseSettings;
        this.options = createOptions();
        this.helpRequested = false;
    }

    /**
     * Parses command line arguments.
     *
     * @param args the command line arguments
     * @return settings with CLI overrides applied
     */
    public WebHarvestSettings parse(String[] args) {
        // Check if any arg has "=" without "-" prefix -> legacy format
        boolean hasLegacyFormat = false;
        for (String arg : args) {
            if (arg.contains("=") && !arg.startsWith("-")) {
                hasLegacyFormat = true;
                break;
            }
        }

        if (hasLegacyFormat) {
            return parseLegacyFormat(args);
        }

        // Modern Commons CLI format
        try {
            return parseWithCommonsCLI(args);
        } catch (Exception e) {
            LOG.debug("Failed to parse with Commons CLI", e);
            // Last resort: try legacy
            return parseLegacyFormat(args);
        }
    }

    private WebHarvestSettings parseWithCommonsCLI(String[] args) throws ParseException {
        org.apache.commons.cli.CommandLineParser cliParser = new DefaultParser();
        commandLine = cliParser.parse(options, args);

        // Check for help
        if (commandLine.hasOption("h")) {
            helpRequested = true;
            return baseSettings;
        }

        // Clone base settings
        WebHarvestSettings settings = new WebHarvestSettings(baseSettings.getProperties());

        // Override with CLI arguments
        if (commandLine.hasOption("c")) {
            configFile = commandLine.getOptionValue("c");
        }

        if (commandLine.hasOption("w")) {
            settings.setWorkDir(commandLine.getOptionValue("w"));
        }

        if (commandLine.hasOption("d")) {
            settings.setDebug(true);
        }

        if (commandLine.hasOption("loglevel")) {
            settings.setLogLevel(commandLine.getOptionValue("loglevel"));
        }

        if (commandLine.hasOption("logpropsfile")) {
            settings.setLogPropsFile(commandLine.getOptionValue("logpropsfile"));
        }

        if (commandLine.hasOption("proxyhost")) {
            settings.setProxyHost(commandLine.getOptionValue("proxyhost"));
        }

        if (commandLine.hasOption("proxyport")) {
            settings.setProxyPort(Integer.parseInt(commandLine.getOptionValue("proxyport")));
        }

        if (commandLine.hasOption("proxyuser")) {
            settings.setProxyUser(commandLine.getOptionValue("proxyuser"));
        }

        if (commandLine.hasOption("proxypassword")) {
            settings.setProxyPassword(commandLine.getOptionValue("proxypassword"));
        }

        if (commandLine.hasOption("proxynthost")) {
            settings.setProxyNtHost(commandLine.getOptionValue("proxynthost"));
        }

        if (commandLine.hasOption("proxyntdomain")) {
            settings.setProxyNtDomain(commandLine.getOptionValue("proxyntdomain"));
        }

        if (commandLine.hasOption("dbdrivers")) {
            settings.setDbDrivers(commandLine.getOptionValue("dbdrivers"));
        }

        if (commandLine.hasOption("plugins")) {
            settings.setPlugins(commandLine.getOptionValue("plugins"));
        }

        return settings;
    }

    private WebHarvestSettings parseLegacyFormat(String[] args) {
        // Legacy format: option=value
        WebHarvestSettings settings = new WebHarvestSettings(baseSettings.getProperties());

        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("/h")) {
                helpRequested = true;
                return settings;
            }

            int eqIndex = arg.indexOf('=');
            if (eqIndex >= 0) {
                String key = arg.substring(0, eqIndex).trim();
                String value = arg.substring(eqIndex + 1).trim();

                switch (key.toLowerCase()) {
                    case "config":
                        configFile = value;
                        break;
                    case "workdir":
                        settings.setWorkDir(value);
                        break;
                    case "debug":
                        settings.setDebug(Boolean.parseBoolean(value));
                        break;
                    case "loglevel":
                        settings.setLogLevel(value);
                        break;
                    case "logpropsfile":
                        settings.setLogPropsFile(value);
                        break;
                    case "proxyhost":
                        settings.setProxyHost(value);
                        break;
                    case "proxyport":
                        settings.setProxyPort(Integer.parseInt(value));
                        break;
                    case "proxyuser":
                        settings.setProxyUser(value);
                        break;
                    case "proxypassword":
                        settings.setProxyPassword(value);
                        break;
                    case "proxynthost":
                        settings.setProxyNtHost(value);
                        break;
                    case "proxyntdomain":
                        settings.setProxyNtDomain(value);
                        break;
                    case "dbdrivers":
                        settings.setDbDrivers(value);
                        break;
                    case "plugins":
                        settings.setPlugins(value);
                        break;
                }
            }
        }

        return settings;
    }

    private Options createOptions() {
        Options opts = new Options();

        opts.addOption("h", "help", false, "Show help message");
        opts.addOption("c", "config", true, "Configuration file path or URL");
        opts.addOption("w", "workdir", true, "Working directory (default: current directory)");
        opts.addOption("d", "debug", false, "Enable debug mode");

        opts.addOption(Option.builder().longOpt("loglevel")
                .hasArg().argName("LEVEL")
                .desc("Log level (TRACE, DEBUG, INFO, WARN, ERROR, FATAL)")
                .build());

        opts.addOption(Option.builder().longOpt("logpropsfile")
                .hasArg().argName("FILE")
                .desc("Legacy Log4j 1 properties file (compatibility mode; overrides --loglevel)")
                .build());

        opts.addOption(Option.builder().longOpt("proxyhost")
                .hasArg().argName("HOST")
                .desc("Proxy server host")
                .build());

        opts.addOption(Option.builder().longOpt("proxyport")
                .hasArg().argName("PORT")
                .desc("Proxy server port")
                .build());

        opts.addOption(Option.builder().longOpt("proxyuser")
                .hasArg().argName("USER")
                .desc("Proxy username")
                .build());

        opts.addOption(Option.builder().longOpt("proxypassword")
                .hasArg().argName("PASSWORD")
                .desc("Proxy password")
                .build());

        opts.addOption(Option.builder().longOpt("proxynthost")
                .hasArg().argName("HOST")
                .desc("NTLM authentication host")
                .build());

        opts.addOption(Option.builder().longOpt("proxyntdomain")
                .hasArg().argName("DOMAIN")
                .desc("NTLM authentication domain")
                .build());

        opts.addOption(Option.builder().longOpt("dbdrivers")
                .hasArg().argName("JARS")
                .desc("Comma-separated list of database driver JARs")
                .build());

        opts.addOption(Option.builder().longOpt("plugins")
                .hasArg().argName("PLUGINS")
                .desc("Comma-separated list of plugin classes")
                .build());

        opts.addOption(Option.builder().longOpt("vars")
                .hasArg().argName("VARS")
                .desc("Semicolon-separated list of name=value pairs")
                .build());

        return opts;
    }

    /**
     * Checks if help was requested.
     *
     * @return true if -h or --help was specified
     */
    public boolean isHelpRequested() {
        return helpRequested;
    }

    /**
     * Gets the configuration file path.
     *
     * @return config file path, or null if not specified
     */
    public String getConfigFile() {
        return configFile;
    }

    /**
     * Prints help message.
     */
    public void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);

        System.out.println();
        System.out.println("WebHarvest Command Line Interface v2.2.1");
        System.out.println("=========================================");
        System.out.println();

        formatter.printHelp("java -jar webharvest-cli.jar [OPTIONS]", options);

        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar webharvest-cli.jar -c myconfig.xml");
        System.out.println("  java -jar webharvest-cli.jar -c config.xml -w /tmp -d");
        System.out.println("  java -jar webharvest-cli.jar --config config.xml --loglevel DEBUG");
        System.out.println();

        System.out.println("Legacy format (still supported):");
        System.out.println("  java -jar webharvest-cli.jar config=myconfig.xml workdir=/tmp debug=yes");
        System.out.println();

        System.out.println("Settings File:");
        System.out.println("  Settings can be stored in ~/.webharvest/webharvest.properties");
        System.out.println("  CLI arguments override settings from file");
        System.out.println();
    }
}

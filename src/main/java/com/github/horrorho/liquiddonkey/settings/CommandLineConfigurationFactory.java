/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.horrorho.liquiddonkey.settings;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * CommandLineConfigurationFactory.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class CommandLineConfigurationFactory {

    private static final CommandLineConfigurationFactory instance = new CommandLineConfigurationFactory();

    public static CommandLineConfigurationFactory getInstance() {
        return instance;
    }

    CommandLineConfigurationFactory() {
    }

    public Configuration configuration(
            CommandLineOptions commandLineOptions,
            String[] args,
            String appName,
            String version,
            boolean requiresAuthenticationProperties
    ) throws ParseException {

        CommandLineParser parser = new DefaultParser();
        Options options = commandLineOptions.options();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commandLineOptions.help())) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.setOptionComparator(null);
            helpFormatter.printHelp(appName + " appleid password [OPTION]...", options);
            helpFormatter.printHelp(appName + " authentication_token [OPTION]...", options);
            return null;
        }

        if (cmd.hasOption(commandLineOptions.version())) {
            System.out.println(version);
            return null;
        }

        Configuration configuration = Configuration.newInstance();

        switch (cmd.getArgList().size()) {
            case 0:
                if (requiresAuthenticationProperties) {
                    throw new ParseException("Missing appleid/ password or authentication_token.");
                }
            case 1:
                // Authentication token
                configuration.setProperty(Property.AUTHENTICATION_TOKEN.key(), cmd.getArgList().get(0));
            case 2:
                // AppleId/ password pair
                configuration.setProperty(Property.AUTHENTICATION_APPLEID.key(), cmd.getArgList().get(0));
                configuration.setProperty(Property.AUTHENTICATION_PASSWORD.key(), cmd.getArgList().get(1));
                break;
            default:
                throw new ParseException(
                        "Too many non-optional arguments, expected appleid/ password or authentication_token only.");
        }

        Iterator<Option> it = cmd.iterator();

        while (it.hasNext()) {
            Option option = it.next();
            String opt = commandLineOptions.opt(option);
            String key = commandLineOptions.property(option).key();

            if (option.hasArgs()) {
                configuration.setProperty(
                        key,
                        joined(cmd.getOptionValues(opt)));
            } else if (option.hasArg()) {
                configuration.setProperty(
                        key,
                        cmd.getOptionValue(opt));
            } else {
                configuration.setProperty(
                        key,
                        Boolean.toString(cmd.hasOption(opt)));
            }
        }
        return configuration;
    }

    String joined(String... list) {
        return list == null
                ? ""
                : Arrays.asList(list).stream().collect(Collectors.joining(" "));
    }
}

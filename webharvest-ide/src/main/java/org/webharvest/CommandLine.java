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
package org.webharvest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.webharvest.gui.Ide;

/**
 * Startup class  for Web-Harvest.
 */
public class CommandLine {

    private static Map<String, String> getArgValue(String[] args, boolean caseSensitive) {
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

    private static Map<String, String> getArgValue(String[] args) {
        return getArgValue(args, false);
    }

    public static void main(final String[] args) throws IOException {
        final Map<String, String> params = getArgValue(args);

        if (params.size() == 0) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    new Ide().createAndShowGUI();
                }
            });
        } else if (params.containsKey("-h") || params.containsKey("/h")) {
            printHelp();
            System.exit(0);
        } else {
            // CLI functionality has been moved to webharvest-cli module
            System.err.println("Command line functionality has been moved to webharvest-cli module.");
            System.err.println("Please use: java -jar webharvest-cli-XX.jar <arguments>");
            System.err.println("");
            System.err.println("To open Web-Harvest GUI, run without arguments:");
            System.err.println("   java -jar webharvest-ide-XX.jar");
            System.exit(1);
        }
    }


    private static void printHelp() {
        System.out.println("");
        System.out.println("Web-Harvest IDE");
        System.out.println("===============");
        System.out.println("");
        System.out.println("To open Web-Harvest GUI:");
        System.out.println("   java -jar webharvest-ide-XX.jar");
        System.out.println("or just double-click webharvest-ide-XX.jar from the file manager.");
        System.out.println("");
        System.out.println("For command line usage, please use the webharvest-cli module:");
        System.out.println("   java -jar webharvest-cli-XX.jar <arguments>");
        System.out.println("");
        System.out.println("For more information, visit: https://github.com/org/webharvest");
    }

}
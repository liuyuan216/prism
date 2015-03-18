//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de>
//	* David Mueller <david.mueller@tcs.inf.tu-dresden.de>
//	
//------------------------------------------------------------------------------
//	
//	This file is part of the jhoafparser library, http://www.ltl2dstar.de/jhoafparser/
//
//	The jhoafparser library is free software; you can redistribute it and/or
//	modify it under the terms of the GNU Lesser General Public
//	License as published by the Free Software Foundation; either
//	version 2.1 of the License, or (at your option) any later version.
//	
//	The jhoafparser library is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//	Lesser General Public License for more details.
//	
//	You should have received a copy of the GNU Lesser General Public
//	License along with this library; if not, write to the Free Software
//	Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
//	
//==============================================================================

package jhoafparser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;

public class JHOAFCmdLine
{
	private static final String version = "1.0";

	public static void main(String[] args)
	{
		HOAConsumer c = null;
		InputStream input = null;
		String filename = null;
		
		PrintStream errorStream = System.err;
		PrintStream outputStream = System.out;

		Boolean resolveAliases = null;
		Boolean validate = null;
		Boolean trace = null;
		boolean verbose = false;
		
		try {
			ArrayList<String> arguments = new ArrayList<String>(args.length);
			for (String arg : args) arguments.add(arg);
			
			String command = (arguments.size() > 0) ? arguments.remove(0) : null;
			if (command == null || command.equals("help") || command.equals("--help")) {
				usage(null);
				System.exit(1);
			}
			if (command.equals("version")) {
				printVersion(outputStream, true);
				System.exit(0);
			}

			while (arguments.size() > 0 && arguments.get(0).startsWith("--")) {
				String flag = arguments.remove(0);
				if (flag.equals("--")) break;

				switch (flag) {
				case "--resolve-aliases=yes":
					resolveAliases = true;
					break;
				case "--resolve-aliases=no":
					resolveAliases = false;
					break;
				case "--validatey=yes":
					validate = true;
					break;
				case "--validate=no":
					validate = false;
					break;
				case "--trace=yes":
					trace = true;
					break;
				case "--trace=no":
					trace = false;
					break;
				case "--verbose":
					verbose = true;
					break;
				default:
					usage("Unknown/unsupported argument: "+flag);
					System.exit(1);
				}
			}
			
			// defaults

			if (resolveAliases == null) {
				resolveAliases = false;
			}
			if (validate == null) {
				validate = true;
			}
			
			if (trace == null) {
				trace = false;
			}

			switch (command) {
			case "print":
				c = new HOAConsumerPrint(System.out);
				break;
			case "parse": {
				errorStream = outputStream;
				final PrintStream err = errorStream;

				c = new HOAConsumerNull() {
					@Override
					public void notifyWarning(String warning) {
						err.println("Warning: "+warning);
					}
				};

				break;
			}
			default:
				usage("Unknown command: "+command);
				System.exit(1);
			}

			if (arguments.isEmpty()) {
				usage("Missing filename (or - for standard input)");
			}
			if (arguments.size() > 1) {
				usage("More than one filename, currently only supports single file");
			}
			filename = arguments.remove(0);
			if (filename.equals("-")) {
				input = System.in;
				filename = null;
			} else {
				input = new FileInputStream(filename);
			}
			if (verbose) {
				errorStream.println("Reading from " + (filename != null ? "file "+filename : "standard input"));
			}
			
			if (resolveAliases) {
				c = new HOAIntermediateResolveAliases(c);
			}
			
			if (trace) {
				c = new HOAIntermediateTrace(c, errorStream);
			}
			
			HOAFParserSettings settings = new HOAFParserSettings();
			settings.setFlagValidate(validate);

			HOAFParser.parseHOA(input, c, settings);
			
			switch (command) {
			case "parse":
				outputStream.println("Parsing OK");
			}
		}
		catch (AbortedException e) {
			c.notifyAbort();
			System.exit(1);
		}
		catch (ParseException e) {
			errorStream.println(e); System.exit(1);
		}
		catch (FileNotFoundException e) {
			errorStream.println(e); System.exit(1);
		}
	}
	
	private static void printVersion(PrintStream out, boolean verbose) {
		out.println("jhoafparser library - command line tool (version "+version + ")");
		out.println(" (C) Copyright 2014- Joachim Klein, David Mueller");
		out.println(" http://www.ltl2dstar.de/jhoafparser/");
		out.println();

		if (verbose) {
			out.println("The jhoafparser library is free software; you can redistribute it and/or");
			out.println("modify it under the terms of the GNU Lesser General Public");
			out.println("License as published by the Free Software Foundation; either");
			out.println("version 2.1 of the License, or (at your option) any later version.");

			out.println();
			out.println("The jhoafparser library is distributed in the hope that it will be useful,");
			out.println("but WITHOUT ANY WARRANTY; without even the implied warranty of");
			out.println("MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU");
			out.println("Lesser General Public License for more details.");
		}
	}

	private static void usage(String error)
	{
		printVersion(System.err, false);

		if (error != null) {
			System.err.println("Command-line arguments error: "+error);
			System.err.println("Use --help to get usage information.");
			return;
		}

		System.err.println("Arguments:");
		System.err.println("  command [flags]* file");
		System.err.println();
		System.err.println(" Valid commands:");
		System.err.println("  parse            Parse the HOAF file and check for errors");
		System.err.println("  print            Parse the HOAF file and print the parsed automaton to standard out");
		System.err.println("  version          Print the version and exit");
		System.err.println();
		System.err.println(" Valid flags:");
		System.err.println("  --resolve-aliases=yes/no           Should aliases be resolved? (default = no)");
		System.err.println("  --validate=yes/no                  Should semantic validation be performed? (default = yes)");
		System.err.println();
		System.err.println("  --trace=yes/no                     Debugging: Trace the function calls to HOAConsumer? (default = no");
		System.err.println("  --verbose                          Increase verbosity");
		System.err.println();
		System.err.println(" file can be a filename or - to request reading from standard input");
	}
}

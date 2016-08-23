package com.verivital.hyst.main;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Localizable;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

import com.verivital.hyst.generators.BuildGenerator;
import com.verivital.hyst.generators.IntegralChainGenerator;
import com.verivital.hyst.generators.ModelGenerator;
import com.verivital.hyst.generators.NavigationGenerator;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.importer.ConfigurationMaker;
import com.verivital.hyst.importer.SpaceExImporter;
import com.verivital.hyst.importer.TemplateImporter;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.passes.basic.AddIdentityResetPass;
import com.verivital.hyst.passes.basic.RemoveSimpleUnsatInvariantsPass;
import com.verivital.hyst.passes.basic.ShortenModeNamesPass;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.passes.basic.SplitDisjunctionGuardsPass;
import com.verivital.hyst.passes.basic.SubstituteConstantsPass;
import com.verivital.hyst.passes.basic.TimeScalePass;
import com.verivital.hyst.passes.complex.ContinuizationPass;
import com.verivital.hyst.passes.complex.ConvertLutFlowsPass;
import com.verivital.hyst.passes.complex.FlattenAutomatonPass;
import com.verivital.hyst.passes.complex.OrderReductionPass;
import com.verivital.hyst.passes.complex.hybridize.HybridizeMixedTriggeredPass;
import com.verivital.hyst.passes.complex.pi.PseudoInvariantPass;
import com.verivital.hyst.passes.complex.pi.PseudoInvariantSimulatePass;
import com.verivital.hyst.printers.DReachPrinter;
import com.verivital.hyst.printers.FlowPrinter;
import com.verivital.hyst.printers.HyCompPrinter;
import com.verivital.hyst.printers.PySimPrinter;
import com.verivital.hyst.printers.PythonQBMCPrinter;
import com.verivital.hyst.printers.SimulinkStateflowPrinter;
import com.verivital.hyst.printers.SpaceExPrinter;
import com.verivital.hyst.printers.ToolPrinter;
import com.verivital.hyst.printers.hycreate2.HyCreate2Printer;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;
import com.verivital.hyst.util.StringOperations;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;

/**
 * Main start class for Hyst If run without args, a GUI will be used. If run
 * with args, the command-line version is assumed.
 */
public class Hyst
{
	// list of supported tool printers (add new ones here)
	private final ToolPrinter[] printers = { new FlowPrinter(), new DReachPrinter(),
			new HyCreate2Printer(), new HyCompPrinter(), new PythonQBMCPrinter(),
			new SpaceExPrinter(), new SimulinkStateflowPrinter(), new PySimPrinter(), };

	// list of supported model transformation passes (add new ones here)
	private final TransformationPass[] passes = { new AddIdentityResetPass(),
			new PseudoInvariantPass(), new PseudoInvariantSimulatePass(), new TimeScalePass(),
			new SubstituteConstantsPass(), new SimplifyExpressionsPass(),
			new SplitDisjunctionGuardsPass(), new RemoveSimpleUnsatInvariantsPass(),
			new ShortenModeNamesPass(), new ContinuizationPass(), new HybridizeMixedTriggeredPass(),
			new FlattenAutomatonPass(), new OrderReductionPass(), new ConvertLutFlowsPass(), };

	// list of supported model generators (add new ones here)
	private final ModelGenerator[] generators = { new IntegralChainGenerator(),
			new NavigationGenerator(), new BuildGenerator() };

	public static String TOOL_NAME = "Hyst v1.3";

	// all program arguments as a single string
	public static String programArguments;

	// should usage printing be omitted (for unit testing)
	public static boolean IS_UNIT_TEST = false;

	// non-null if gui mode enabled, used for logging
	private static HystFrame guiFrame = null;

	// these should be statically accessible
	public static boolean verboseMode = false;
	public static boolean debugMode = false;

	// localizable object for use in args error reporting
	public static Localizable hystLocalizable = new Localizable()
	{
		@Override
		public String formatWithLocale(Locale locale, Object... args)
		{
			return format(args);
		}

		@Override
		public String format(Object... args)
		{
			StringBuilder sb = new StringBuilder();

			for (Object a : args)
				sb.append(a.toString());

			return sb.toString();
		}
	};

	/////////////////////////////////////////////////////

	private CmdLineParser parser = new CmdLineParser(this);

	// extracted from arguments
	private ArrayList<String> xmlFilenames = new ArrayList<String>();
	private String cfgFilename = null;

	// command line options
	@Option(name = "-help", aliases = { "-h" }, usage = "print command-line usage")
	boolean doHelp = false;

	@Option(name = "-help_printers", aliases = {
			"-hp" }, usage = "print usage information on tool printers")
	boolean doHelpTools = false;

	@Option(name = "-help_passes", aliases = {
			"-hp" }, usage = "print usage information on transformation passes")
	boolean doHelpPasses = false;

	@Option(name = "-help_generators", aliases = {
			"-hg" }, usage = "print usage information on model generators")
	boolean doHelpGenerators = false;

	@Option(name = "-input", aliases = {
			"-i" }, usage = "input filenames", metaVar = "FILE1 FILE2 ...", handler = StringArrayOptionHandler.class)
	public void setInput(String[] files) throws CmdLineException
	{
		for (String file : files)
		{
			if (file.endsWith(".xml"))
				xmlFilenames.add(file);
			else if (file.endsWith(".cfg"))
			{
				if (cfgFilename == null)
					cfgFilename = file;
				else
					throw new CmdLineException(parser, hystLocalizable,
							"Multiple .cfg input files are not allowed.");
			}
			else
				throw new CmdLineException(parser, hystLocalizable,
						"Unrecognized input file extension (expected .cfg or .xml): '" + file
								+ "'");
		}
	}

	@Option(name = "-output", required = true, aliases = {
			"-o" }, usage = "output filename", metaVar = "FILENAME")
	String outputFilename;

	// the chosen tool printer (dynamic parameter)
	ToolPrinter toolPrinter = null;
	String toolParamsString = null;

	@Option(name = "-tool", required = true, aliases = {
			"-t" }, usage = "target tool and tool params", metaVar = "TOOLNAME TOOLPARAMS")
	public void setTool(String[] params) throws CmdLineException
	{
		if (params.length != 2)
			throw new CmdLineException(parser, hystLocalizable,
					"-tool expected exactly two follow-on arguments: TOOL_NAME TOOL_PARAMS (params can be explicit empty string). See -help_printers.");

		toolParamsString = params[1];

		// look through all the model generators for the right one
		for (ToolPrinter tp : printers)
		{
			if (tp.getCommandLineFlag().equalsIgnoreCase(params[0]))
			{
				toolPrinter = tp;
				break;
			}
		}

		if (toolPrinter == null)
			throw new CmdLineException(parser, hystLocalizable,
					"-tool parameter '" + params[0] + "' was invalid.");
	}

	ModelGenerator modelGenerator = null;
	String modelGenParam = null; // parameter for model generator

	@Option(name = "-generate", aliases = {
			"-gen" }, usage = "generate a model (rather than loading from a file)", metaVar = "GEN_NAME GEN_PARAMS", handler = StringArrayOptionHandler.class)
	public void setGenerate(String[] params) throws CmdLineException
	{
		if (params.length != 2)
			throw new CmdLineException(parser, hystLocalizable,
					"-generate expected two follow-on arguments: GEN_NAME GEN_PARAMS (params can be explicit empty string). See -help_gen.");

		modelGenParam = params[1];

		// look through all the model generators for the right one
		for (ModelGenerator mg : generators)
		{
			if (mg.getCommandLineFlag().equalsIgnoreCase(params[0]))
			{
				modelGenerator = mg;
				break;
			}
		}

		if (modelGenerator == null)
			throw new CmdLineException(parser, hystLocalizable,
					"-generate parameter '" + params[0] + "' was invalid.");
	}

	// passes that the user has selected
	private ArrayList<RequestedTransformationPass> requestedPasses = new ArrayList<RequestedTransformationPass>();

	@Option(name = "-passes", aliases = {
			"-p" }, usage = "run a sequence of model transformation apsses", metaVar = "PASS1 PARAMS1 PASS2 PARAMS2 ...", handler = StringArrayOptionHandler.class)
	public void setPasses(String[] params) throws CmdLineException
	{
		if (params.length % 2 != 0)
			throw new CmdLineException(parser, hystLocalizable,
					"-passes expected multiple of two follow-on arguments: PASS_NAME PASS_PARMAS (params can be excplicit empty string). See -help_gen.");

		for (int i = 0; i < params.length; i += 2)
		{
			String passName = params[i];
			String passParam = params[i + 1];
			boolean found = false;

			for (TransformationPass tp : passes)
			{
				if (tp.getCommandLineFlag().equals(passName))
				{
					// create new instances here since we may use the same pass
					// multiple times with different parmeters
					TransformationPass instance = newTransformationPassInstance(tp);
					requestedPasses.add(new RequestedTransformationPass(instance, passParam));
					found = true;
					break;
				}
			}

			if (!found)
				throw new CmdLineException(parser, hystLocalizable,
						"Couldn't find transformation pass with name '" + passName
								+ "'. See -help_gen.");
		}
	}

	@Option(name = "-verbose", aliases = { "-v" }, usage = "print verbose output")
	public void setVerbose()
	{
		Hyst.verboseMode = true;
	}

	@Option(name = "-debug", aliases = { "-d" }, usage = "print debug (and verbose) output")
	public void setDebug()
	{
		Hyst.debugMode = Hyst.verboseMode = true;
	}

	///////// hidden options ///////////////

	@Option(name = "-novalidate", hidden = true, usage = "disable model validation")
	public void setNoValidate()
	{
		Configuration.DO_VALIDATION = false;
		Hyst.log("Internal model validatation disabled.");
	}

	@Option(name = "-testpython", hidden = true, usage = "test if python exists on system")
	boolean doTestPython = false;

	/////////////////////////////////////////////////////

	public enum ExitCode
	{
		SUCCESS, // 0
		EXPORT_EXCEPTION, // 1
		PRECONDITIONS_EXCEPTION, // 2
		ARG_PARSE_ERROR, // 3
		GUI_QUIT, // 4
		EXPORT_AUTOMATON_EXCEPTION, // 5
		NOPYTHON // 6, exit code if -checkpython fails
	};

	public static void main(String[] args)
	{
		programArguments = makeSingleArgument(args);

		new Hyst(args);
	}

	/**
	 * Check command-line arguments for inconsistancies.
	 */
	private void checkArguments() throws CmdLineException
	{
		if (modelGenerator == null)
		{
			if (cfgFilename == null)
				throw new CmdLineException(parser, hystLocalizable,
						"No input .cfg files were provided.");

			if (xmlFilenames.size() == 0)
				throw new CmdLineException(parser, hystLocalizable,
						"No input .xml files were provided.");
		}
		else
		{
			// model generator exists, shouldn't have xml or cfg files
			if (cfgFilename != null || xmlFilenames.size() > 0)
				throw new CmdLineException(parser, hystLocalizable,
						"Cannot both use model generation and provide input cfg/xml files.");
		}
	}

	private Hyst(String[] args)
	{
		final String FLAG_GUI = "-gui";

		if (args.length > 0 && !args[0].equals(FLAG_GUI))
		{
			// try parse args
			ExitCode code = ExitCode.SUCCESS;

			try
			{
				parser.parseArgument(args);

				checkArguments();

				if (doHelp)
					showHelp();

				if (doHelpTools)
					showHelpTools();

				if (doHelpPasses)
					showHelpPasses();

				if (doHelpGenerators)
					showHelpGenerators();

				if (!doHelp && !doHelpTools && !doHelpPasses && !doHelpGenerators)
					code = runCommandLine();
			}
			catch (CmdLineException e)
			{
				System.out.println(e.getMessage() + "\nUse -help for command-line options.");
				code = ExitCode.ARG_PARSE_ERROR;
			}

			System.exit(code.ordinal());
		}
		else
		{
			final String loadFilename = (args.length >= 2) ? args[1] : null;

			// use gui
			System.out.println("Started in GUI mode. For command-line help use the -help flag.");

			fixLookAndFeel();

			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					guiFrame = new HystFrame(printers, passes);

					if (loadFilename != null)
						guiFrame.guiLoad(loadFilename);

					guiFrame.setVisible(true);
				}
			});
		}
	}

	private void showHelp()
	{
		parser.printUsage(System.out);
	}

	private void showHelpTools()
	{
		for (ToolPrinter printer : printers)
			System.out.println(printer.getParamHelp());
	}

	private void showHelpPasses()
	{
		for (TransformationPass pass : passes)
			System.out.println(pass.getParamHelp());
	}

	private void showHelpGenerators()
	{
		for (ModelGenerator gen : generators)
			System.out.println(gen.getParamHelp());
	}

	/**
	 * Main Hyst converter method. Assumes arguments have been correctly parsed.
	 * 
	 * @return the status exit code
	 */
	private ExitCode runCommandLine()
	{
		ExitCode rv = ExitCode.SUCCESS;
		Exception ex = null;

		if (debugMode)
			log("Debug mode (even more verbose) printing enabled.\n");
		else if (verboseMode)
			log("Verbose mode printing enabled.\n");

		try
		{
			long startMs = System.currentTimeMillis();
			Configuration config = null;

			if (modelGenerator != null)
			{
				Expression.expressionPrinter = null; // should be assigned in
														// geneartor
				config = modelGenerator.generate(modelGenParam);
			}
			else
			{
				// 1. import the SpaceExDocument
				SpaceExDocument spaceExDoc = SpaceExImporter.importModels(cfgFilename,
						xmlFilenames.toArray(new String[xmlFilenames.size()]));

				// 2. convert the SpaceEx data structures to template automata
				Map<String, Component> componentTemplates = TemplateImporter
						.createComponentTemplates(spaceExDoc);

				// 3. run any component template passes here (future)

				// 4. instantiate the component templates into a networked
				// configuration
				config = ConfigurationMaker.fromSpaceEx(spaceExDoc, componentTemplates);
			}

			// 5. run passes
			runPasses(config);

			// 6. run printer
			runPrinter(toolPrinter, config);

			long difMs = System.currentTimeMillis() - startMs;

			toolPrinter.flush();
			Hyst.logInfo("\nFinished converting in " + difMs + " ms");
		}
		catch (AutomatonExportException e)
		{
			logError("Automaton Export Exception while exporting.");
			ex = e;
			rv = ExitCode.EXPORT_AUTOMATON_EXCEPTION;
		}
		catch (PreconditionsFailedException e)
		{
			logError("Preconditions not met for exporting.");
			ex = e;
			rv = ExitCode.PRECONDITIONS_EXCEPTION;
		}
		catch (Exception e)
		{
			logError("Exception in Hyst while exporting.");
			ex = e;
			rv = ExitCode.EXPORT_EXCEPTION;
		}

		if (ex != null)
		{
			if (verboseMode)
			{
				String message = ex.getLocalizedMessage() != null ? ex.getLocalizedMessage()
						: ex.toString();

				log(message);
				log("Stack trace from exception:");

				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				ex.printStackTrace(pw);
				log(sw.toString());
			}
			else
				logError("For more information about the error, use the -verbose or -debug flag.");
		}

		return rv;
	}

	private void runPrinter(ToolPrinter printer, Configuration config)
	{
		Expression.expressionPrinter = null; // should be assigned in printer

		String originalFilename = StringOperations.join(" ", xmlFilenames.toArray(new String[] {}));

		if (outputFilename != null)
			printer.setOutputFile(outputFilename);
		else if (guiFrame != null)
			printer.setOutputGui(guiFrame);

		printer.print(config, toolParamsString, originalFilename);
	}

	private void runPasses(Configuration config)
	{
		for (RequestedTransformationPass rp : requestedPasses)
		{
			Hyst.log("Running pass " + rp.tp.getName() + " with params " + rp.params);

			Expression.expressionPrinter = null; // should be assigned in pass
			rp.tp.runTransformationPass(config, rp.params);

			Hyst.logDebug("\n----------After running pass " + rp.tp.getName()
					+ ", configuration is:\n" + config);
		}
	}

	private static void fixLookAndFeel()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
		}
	}

	private static String makeSingleArgument(String[] ar)
	{
		String rv = "";

		for (String s : ar)
		{
			if (rv.length() != 0)
				rv += " ";

			// escape spaces and special handling of empty string
			if (s.length() == 0)
				rv += "\"\"";
			else if (s.contains(" "))
				rv += "\"" + s + "\"";
			else
				rv += s;
		}

		return rv;
	}

	private static TransformationPass newTransformationPassInstance(TransformationPass tp)
	{
		// create a new instance of the transformation pass to give it fresh
		// state
		Class<? extends TransformationPass> cl = tp.getClass();
		Constructor<? extends TransformationPass> ctor;
		TransformationPass instance = null;

		try
		{
			ctor = cl.getConstructor();
			instance = ctor.newInstance();
		}
		catch (NoSuchMethodException e)
		{
			throw new AutomatonExportException("Error instantiating TransformationPass", e);
		}
		catch (InstantiationException e2)
		{
			throw new AutomatonExportException("Error instantiating TransformationPass", e2);
		}
		catch (IllegalArgumentException e3)
		{
			throw new AutomatonExportException("Error instantiating TransformationPass", e3);
		}
		catch (IllegalAccessException e4)
		{
			throw new AutomatonExportException("Error instantiating TransformationPass", e4);
		}
		catch (InvocationTargetException e5)
		{
			throw new AutomatonExportException("Error instantiating TransformationPass", e5);
		}

		return instance;
	}

	/**
	 * Print an info message to stderr, if the -v flag has been set (verbose
	 * mode is enabled)
	 * 
	 * @param message
	 *            the message to print
	 */
	public static void log(String message)
	{
		if (verboseMode || debugMode)
		{
			if (guiFrame != null)
				guiFrame.addOutput(message);

			System.err.println(message);
		}
	}

	/**
	 * Print an info message to stderr, regardless of verbose / debug flags
	 * 
	 * @param message
	 *            the message to print
	 */
	public static void logInfo(String message)
	{
		if (guiFrame != null)
			guiFrame.addOutput(message);
		else
			System.err.println(message);
	}

	/**
	 * Print an info message to stderr, if the -d flag has been set (debug mode
	 * is enabled). This is even more verbose
	 * 
	 * @param message
	 *            the message to print
	 */
	public static void logDebug(String message)
	{
		if (debugMode)
		{
			if (guiFrame != null)
				guiFrame.addOutput(message);

			System.err.println(message);
		}
	}

	/**
	 * Print an error message to stderr
	 * 
	 * @param message
	 *            the message to print
	 */
	public static void logError(String message)
	{
		if (guiFrame != null)
			guiFrame.addOutput(message);

		System.err.println(message);
	}
}

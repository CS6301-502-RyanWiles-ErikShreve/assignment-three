// ***************************************************************************
// Assignment: 2
// Team : 2
// Team Members: Ryan Wiles, Erik Shreve
//
// Code reuse/attribution notes:
// args4j (for command line parsing) based on example code from:
// https://github.com/kohsuke/args4j/blob/master/args4j/examples/SampleMain.java
// walkFolder method based on example code from:
// https://lucene.apache.org/core/5_4_1/demo/src-html/org/apache/lucene/demo/IndexFiles.html
// ***************************************************************************
package edu.utdallas.cs6301_502;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

class Runner {
	@Option(name = "-d", usage = "print debug information to console")
	private boolean debug = false;

	//	@Option(name = "-c", usage = "create new index")
	private static boolean create = false;

	//	@Option(name = "-src", usage = "base folder containing .java source to index")
	private String baseFolder = "";

	//	@Option(name = "-i", usage = "index location")
	private String indexPath = "";

	//	@Option(name = "-g", usage = "gold set file")
	private String goldSetFile = "";

	// receives other command line parameters than options
	@Argument
	private List<String> arguments = new ArrayList<String>();

	private static LuceneUtil luceneUtil;
	private TextScrubber textScrubber;

	public static void main(String... args) throws Exception {
		Runner r = new Runner();
		r.doMain(args);
		r.run();
	}

	public void doMain(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);

		try {
			// parse the arguments.
			parser.parseArgument(args);

			//			// check if enough arguments are given.
			//			if (goldSetFile.isEmpty())
			//				throw new CmdLineException(parser, "No gold set file was provided");
			//
			//			if (indexPath.isEmpty())
			//				throw new CmdLineException(parser, "No index location was provided");
			//
			//			if (create && baseFolder.isEmpty())
			//				throw new CmdLineException(parser, "No base folder was provided, but create new index was specified");

		} catch (CmdLineException e) {
			// report an error message.
			System.err.println(e.getMessage());
			System.err.println("java Runner [options...] arguments...");
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();

			return;
		}
	}

	public Runner() {
		super();
	}

	public void run() {
		try {
			// SETUP
			luceneUtil = new LuceneUtil(create, indexPath);

			String text = "This World is an amazing place";
			Properties props = new Properties();
			props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

			Annotation annotation = pipeline.process(text);
			List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
			for (CoreMap sentence : sentences) {
				String sentiment = sentence.get(SentimentCoreAnnotations.SentimentClass.class);
				System.out.println(sentiment + "\t" + sentence);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void debug(String line) {
		if (this.debug) {
			System.out.println(line);
		}
	}

	private String readResourceFile(String resourceName) {
		ClassLoader classLoader = this.getClass().getClassLoader();
		File file = new File(classLoader.getResource(resourceName).getFile());
		StringBuilder builder = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while (reader.ready()) {
				builder.append(reader.readLine());
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return builder.toString();
	}
}
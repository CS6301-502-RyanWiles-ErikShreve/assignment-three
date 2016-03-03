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
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.auth.AnonymousAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.ServerVersionConstants;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

class Runner {
	@Option(name = "-d", usage = "print debug information to console")
	private boolean debug = false;

	@Option(name = "-n", usage = "maximum number of issues to download. use -1 for all.")
	private int maxNumIssues = 10;

	@Option(name = "-s", usage = "URL of JIRA server")
	private String jiraServerURL = "https://issues.apache.org/jira";

	@Option(name = "-p", usage = "name of project on JIRA server")
	private String projectName = "SPARK";
	
	// receives other command line parameters than options
	@Argument
	private List<String> arguments = new ArrayList<String>();

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

			CoreNLPExample();
			jiraExample();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void CoreNLPExample()
	{
		// Begin NLP example code
		String text = "This World is an amazing place, I was so amazed.";
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		Annotation annotation = pipeline.process(text);
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
		//	String sentiment = sentence.get(SentimentCoreAnnotations.SentimentClass.class);
		//	System.out.println("Sentiment: " + sentiment + "\t" + sentence);
			
			for (CoreLabel token: sentence.get(TokensAnnotation.class))
			{
				String pos = token.get(PartOfSpeechAnnotation.class);
				System.out.println("POS: " + pos + "\t" + token.originalText());
			}
			
		}

		for (String lemmas : lemmatize(pipeline, text)) {
			System.out.println("Lemmas: " + lemmas);
		}
	}

	private void jiraExample() throws InterruptedException, ExecutionException, IOException
	{
		URI jiraServerUri = URI.create(jiraServerURL);
		final AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
		final JiraRestClient restClient = factory.create(jiraServerUri, new AnonymousAuthenticationHandler());

		try {
			Promise<Project> promiseProject = restClient.getProjectClient().getProject(projectName);
			Project project = promiseProject.get();
			System.out.println(project.getName() + ": " + project.getDescription());

			printAllIssues(restClient, projectName);
		} finally {
			restClient.close();
		}
	}
	
	public void printAllIssues(JiraRestClient restClient, String projectKey) {
		int startIndex = 0;
		int maxResults = 0;
		int totalIssuesRead = 0;				
		boolean done = false;
		
		do {
			
			if (maxNumIssues > 0)
			{
				int toRead = maxNumIssues-totalIssuesRead;
				if (toRead < 0)
				{
					maxResults = 100;
				}
				else
				{
					maxResults = Math.min(100, toRead);
				}
			}
						
			Promise<SearchResult> searchJqlPromise = restClient.getSearchClient().searchJql("project = " + projectKey, maxResults, startIndex, null);
			System.out.println(searchJqlPromise.claim().getTotal());
			
			SearchResult searchResult = searchJqlPromise.claim();
			for (Issue issue : searchResult.getIssues()) {
				totalIssuesRead++;
				System.out.println(issue.getKey() + " - " + issue.getSummary());
			}
			
			if (totalIssuesRead == searchResult.getTotal() ||
				totalIssuesRead >= maxNumIssues)
			{
				done = true;
			}
			
			startIndex = totalIssuesRead;
			System.out.println(totalIssuesRead + " / " + searchResult.getTotal() + ": " + done);
		} while (!done);
	}
	
	public void printAllProjects(JiraRestClient restClient) {
		final int buildNumber = restClient.getMetadataClient().getServerInfo().claim().getBuildNumber();

		// first let's get and print all visible projects (only jira4.3+)
		System.out.println("Print all print all visible projects:");
		if (buildNumber >= ServerVersionConstants.BN_JIRA_4_3) {
			final Iterable<BasicProject> allProjects = restClient.getProjectClient().getAllProjects().claim();
			for (BasicProject project : allProjects) {
				System.out.println(project.getKey() + ": " + project.getName());
			}
		}

	}

	public List<String> lemmatize(StanfordCoreNLP pipeline, String documentText) {
		List<String> lemmas = new LinkedList<String>();

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);

		// run all Annotators on this text
		pipeline.annotate(document);

		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// Retrieve and add the lemma for each word into the
				// list of lemmas
				lemmas.add(token.lemma());
			}
		}

		return lemmas;
	}

	class IssueData
	{
		public int numSentences;
		public int numTerms;
		public int numNouns;
		public int numVerbs;
		public int numAdjectives;
		public int numAdverbs;
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
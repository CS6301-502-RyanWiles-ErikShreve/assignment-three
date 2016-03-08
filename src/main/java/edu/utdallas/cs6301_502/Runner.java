// ***************************************************************************
// Assignment: 3
// Team : 1
// Team Members: Ryan Wiles, Erik Shreve
//
// Code reuse/attribution notes:
// args4j (for command line parsing) based on example code from:
// https://github.com/kohsuke/args4j/blob/master/args4j/examples/SampleMain.java
//
// ***************************************************************************
package edu.utdallas.cs6301_502;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.auth.AnonymousAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.ServerVersionConstants;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.utdallas.cs6301_502.jira.CorpusData;
import edu.utdallas.cs6301_502.jira.IssueComparator;
import edu.utdallas.cs6301_502.jira.IssueData;
import edu.utdallas.cs6301_502.jira.JiraMarkupScrubber;

class Runner {
	@Option(name = "-d", usage = "print debug information to console")
	private boolean debug = false;

	@Option(name = "-n", usage = "maximum number of issues to download. use -1 for all.")
	private int maxNumIssues = 10;

	@Option(name = "-s", usage = "URL of JIRA server")
	private String jiraServerURL = "https://issues.apache.org/jira";

	@Option(name = "-p", usage = "name of project on JIRA server")
	private String projectName = "SPARK";

	@Option(name = "-i", usage = "a specific issue number to process. If specified, -n is ignored.")
	private int anIssue = 0;
	
	// receives other command line parameters than options
	@Argument
	private List<String> arguments = new ArrayList<String>();

	
	private CorpusData titleCorpusData;
	private CorpusData descriptionCorpusData;
	private CorpusData titleAndDescriptionCorpusData;
	
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
			textScrubber = new TextScrubber(loadWords("stop_words.xml"), 2)
					.addStopWords(loadWords("java_keywords.xml"))
					.addStopWords(loadWords("java_common_classes.xml"))
					.setPreserveHyphenatedWords(true);
			
			titleCorpusData = new CorpusData("Title Corpus");
			descriptionCorpusData = new CorpusData("Description Corpus");
			titleAndDescriptionCorpusData = new CorpusData("Title & Description Corpus");
			
			
			processJiraProject();
			printStats();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void processJiraProject()
	{
		URI jiraServerUri = URI.create(jiraServerURL);
		final AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
		final JiraRestClient restClient = factory.create(jiraServerUri, new AnonymousAuthenticationHandler());

		try {
			if (anIssue < 1)
			{
				processAllIssues(restClient, projectName);
			}
			else
			{
				proccessOneIssue(restClient, projectName, anIssue);
			}
		} finally {
			try {
				restClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void proccessOneIssue(JiraRestClient restClient, String projectKey, int anIssue) {
		
		try
		{
			Promise<Issue> issuePromise = restClient.getIssueClient().getIssue(projectKey + "-" + anIssue);
			Issue issue = issuePromise.get();


			String summary = textScrubber.scrubToString(JiraMarkupScrubber.scrub(issue.getSummary())); 
			String description = textScrubber.scrubToString(JiraMarkupScrubber.scrub(issue.getDescription()));

			processData(titleCorpusData, issue.getId(), new String[] {summary});				
			processData(descriptionCorpusData, issue.getId(), new String[] {description});				
			processData(titleAndDescriptionCorpusData, issue.getId(), new String[] {summary, description});				

			System.out.println(issue.getKey());
			System.out.println(issue.getDescription());
			System.out.println("----------------------------");
			System.out.println(description);
			System.out.println("----------------------------");
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		
	}

	private void processAllIssues(JiraRestClient restClient, String projectKey) {
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
			else
			{
				maxResults = 100;
			}
						
			Promise<SearchResult> searchJqlPromise = restClient.getSearchClient().searchJql("project = " + projectKey, maxResults, startIndex, null);
			
			SearchResult searchResult = searchJqlPromise.claim();
			for (Issue issue : searchResult.getIssues()) {
				totalIssuesRead++;
							
				System.out.println(issue.getKey());
				
				String summary = textScrubber.scrubToString(JiraMarkupScrubber.scrub(issue.getSummary())); 
				String description = textScrubber.scrubToString(JiraMarkupScrubber.scrub(issue.getDescription()));
				
				processData(titleCorpusData, issue.getId(), new String[] {summary});				
				processData(descriptionCorpusData, issue.getId(), new String[] {description});				
				processData(titleAndDescriptionCorpusData, issue.getId(), new String[] {summary, description});				
					

			}
			
			if (totalIssuesRead == searchResult.getTotal() ||
				(maxNumIssues > 0 && totalIssuesRead >= maxNumIssues))
			{
				done = true;
			}
			
			startIndex = totalIssuesRead;
		} while (!done);
	}
	
	private void processData(CorpusData corpusData, Long idNum, String... textArr) {
		String text = "";
		for (String s : textArr) {
			if (s != null) {
				text += s + " ";
			}
		}
		text = text.trim();
		if (!text.isEmpty()) {
			collectStats(corpusData, idNum, text);
		} else {
			corpusData.numEmptyIssues++;
		}
	}
	
	private void collectStats(CorpusData corpusData, Long idNum, String text)
	{
		IssueData id = new IssueData();
		id.issueNumber = idNum;
		
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		Annotation annotation = pipeline.process(text);
		List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
		
			id.numSentences++;
			
			for (CoreLabel token: sentence.get(TokensAnnotation.class))
			{
				id.numTerms++;
				
				String pos = token.get(PartOfSpeechAnnotation.class);
				
				if (pos.startsWith("NN"))
				{
					id.numNouns++;
					addToWordCountMap(corpusData.nounMap, token.lemma());
				}
				else if (pos.startsWith("VB"))
				{
					id.numVerbs++;
					addToWordCountMap(corpusData.verbMap, token.lemma());
				}				
				else if (pos.startsWith("JJ"))
				{
					id.numAdjectives++;
					addToWordCountMap(corpusData.adjectiveMap, token.lemma());
				}
				else if (pos.startsWith("RB"))
				{
					id.numAdverbs++;
					addToWordCountMap(corpusData.adverbMap, token.lemma());
				}
				
			}
			
		}
		
		corpusData.issues.add(id);
	}

	private void addToWordCountMap(Map<String, Long> map, String string) {
		if (map.containsKey(string))
		{
			map.put(string, map.get(string) + 1);
		}
		else
		{
			map.put(string, 1l);
		}
	}

	private void printStats()
	{
		System.out.println("Results for " + projectName + " on " + jiraServerURL);
			
		printCorpusStats(titleCorpusData);
		System.out.println("--------------------------------------------------");
		printCorpusStats(descriptionCorpusData);
		System.out.println("--------------------------------------------------");
		printCorpusStats(titleAndDescriptionCorpusData);
		System.out.println("--------------------------------------------------");
		
		System.out.println("Top 10 For System (aka Title And Description Corpus):");
		
		System.out.println("Nouns: " + toStringTopX(titleAndDescriptionCorpusData.nounMap, 10));
		System.out.println("Verbs: " + toStringTopX(titleAndDescriptionCorpusData.verbMap, 10));
		System.out.println("Adjectives: " + toStringTopX(titleAndDescriptionCorpusData.adjectiveMap, 10));
		System.out.println("Adverbs: " + toStringTopX(titleAndDescriptionCorpusData.adverbMap, 10));
	}
	
	private <K, V extends Comparable<? super V>> String toStringTopX(Map<K,V> m, int x)
	{
		String output = "";
		
		Map<K, V> sorted = MapUtil.sortByValue(m);
		
		Iterator<Entry<K, V>> itr = sorted.entrySet().iterator();
		for (int i = 0; i < x && i < sorted.size(); i++)
		{
			output +=  itr.next().getKey() + " ";
		}
		
		return output;
	}
	
	private void printCorpusStats(CorpusData corpusData)
	{
		System.out.println("For " + corpusData.getName());
		System.out.println("Total # Bug Reports on Server: " + (corpusData.numEmptyIssues + corpusData.issues.size()));
		System.out.println("Number of Bug Reports with no data after preprocessing: " + corpusData.numEmptyIssues);
		System.out.println("Number of Bug Reports in corpus: " + corpusData.issues.size());
	
		long totalSentences = 0;
		long totalTerms = 0;
		long totalNouns = 0;
		long totalVerbs = 0;
		long totalAdjectives = 0;
		long totalAdverbs = 0;
		
		for (IssueData issue : corpusData.issues)
		{
			totalSentences += issue.numSentences;
			totalTerms += issue.numTerms;
			totalNouns += issue.numNouns;
			totalVerbs += issue.numVerbs;
			totalAdjectives += issue.numAdjectives;
			totalAdverbs += issue.numAdverbs;
		}
		
		int numIssues = corpusData.issues.size();
		double medSentences = calculateMedian(corpusData.issues, "numSentences");
		double medTerms = calculateMedian(corpusData.issues, "numTerms");
		double medNouns = calculateMedian(corpusData.issues, "numNouns");
		double medVerbs = calculateMedian(corpusData.issues, "numVerbs");
		double medAdjectives = calculateMedian(corpusData.issues, "numAdjectives");
		double medAdverbs = calculateMedian(corpusData.issues, "numAdverbs");

		System.out.println("Avg(Med) Sentences: " + totalSentences/numIssues + "(" + new DecimalFormat("#0.00").format(medSentences)  + ")");		
		System.out.println("Avg(Med) Terms: " + totalTerms/numIssues + "(" + new DecimalFormat("#0.00").format(medTerms)  + ")");		
		System.out.println("Avg(Med) Nouns: " + totalNouns/numIssues + "(" + new DecimalFormat("#0.00").format(medNouns)  + ")");
		System.out.println("Avg(Med) Verbs: " + totalVerbs/numIssues + "(" + new DecimalFormat("#0.00").format(medVerbs)  + ")");
		System.out.println("Avg(Med) Adjectives: " + totalAdjectives/numIssues + "(" + new DecimalFormat("#0.00").format(medAdjectives)  + ")");
		System.out.println("Avg(Med) Adverbs: " + totalAdverbs/numIssues + "(" + new DecimalFormat("#0.00").format(medAdverbs)  + ")");
	}
	
	private Double calculateMedian(List<IssueData> issues, String fieldName) {
		Collections.sort(issues, new IssueComparator(fieldName));

		int numIssues = issues.size();
		double medianValue = 0;
		try {
			if (numIssues % 2 == 0) {
				long lower = Long.parseLong(BeanUtils.getSimpleProperty(issues.get(numIssues / 2), fieldName));
				long upper = Long.parseLong(BeanUtils.getSimpleProperty(issues.get(1 + (numIssues / 2)), fieldName));
				medianValue = (lower + upper) / 2;
			} else {
				medianValue = Long.parseLong(BeanUtils.getSimpleProperty(issues.get(numIssues / 2), fieldName));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return medianValue;
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


	@SuppressWarnings("unused")
	private void debug(String line) {
		if (this.debug) {
			System.out.println(line);
		}
	}

	private Set<String> loadWords(String resource) {
		Set<String> words = new HashSet<String>();

		ClassLoader classLoader = getClass().getClassLoader();
		//System.out.println("classloader == null: " + (classLoader == null));
		File file = new File(classLoader.getResource(resource).getFile());

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while (reader.ready()) {
				String line = reader.readLine().trim();
				if (line.startsWith("<word>") && line.endsWith("</word>"))
				{
					words.add(line.substring(6, line.length() - 7).toLowerCase());
				}

			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return words;
	}

	@SuppressWarnings("unused")
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
package edu.utdallas.cs6301_502;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TextScrubber {

	private Set<String> preProcessStopWords = new HashSet<String>();
	private Set<String> stopWords = new HashSet<String>();
	private int minWordSize;
	private boolean stemWords = false;
	private boolean includeCamelCase = false;

	public TextScrubber(Set<String> stopWords, int minWordSize) {
		super();
		this.stopWords.addAll(stopWords);
		this.minWordSize = minWordSize;
	}

	public TextScrubber addStopWords(Set<String> stopWords) {
		this.stopWords.addAll(stopWords);
		return this;
	}

	public TextScrubber addPreProcessStopWords(Set<String> preProcessStopWords) {
		this.preProcessStopWords.addAll(preProcessStopWords);
		return this;
	}

	public TextScrubber setStemWord(boolean stemWords) {
		this.stemWords = stemWords;
		return this;
	}

	public TextScrubber setIncludeCamelCase(boolean includeCamelCase) {
		this.includeCamelCase = includeCamelCase;
		return this;
	}

	public String scrubToString(String text)
	{
		String output = "";
		
		for(String s: scrub(text))
		{
			output += " " + s;
		}
		
		return output.trim();
	}
	
	public List<String> scrub(String text) {

		PorterStemmer stemmer = new PorterStemmer();

		List<String> output = new ArrayList<String>();

		text = text.trim();

		if (text.isEmpty()) {
			return output;
		}

		for (String preProcessWord : preProcessStopWords) {
			text = text.replaceAll(preProcessWord, " ");
		}
		
		// check for line with only // comments
		if (text.startsWith("//")) {
			text = text.substring(2).trim();
		}

		// check for javadoc style comments
		if (text.startsWith("/**")) {
			text = text.substring(3).trim();
		}

		// check for c style comments
		if (text.startsWith("/*")) {
			text = text.substring(2).trim();
		}

		// explode punctuation to a space
		text = text.replaceAll("[\\{|\\}|\\(|\\)|;|,|=|+|\\-|*|\"|'|/|\\?|:|<|\\[|\\]|!|\\>|\\^|\\$|\\&\\&|\\|\\||\\.|`|#|~|_]", " ").trim();
		text = text.replaceAll("\\\\t", " ").trim();
		text = text.replaceAll("\\\\r", " ").trim();
		text = text.replaceAll("\\\\n", " ").trim();
		text = text.replaceAll("\\\\", " ").trim();
		text = text.replaceAll("(^\\s)[0-9]+\\.[0-9]+", " ").trim(); // decimal numbers
		text = text.replaceAll("(^\\s)[0-9]+f", " ").trim(); // integer numbers as a float
		text = text.replaceAll("(^\\s)[0-9]+d", " ").trim(); // integer numbers as a double
		text = text.replaceAll("(^\\s)0[x|X][0-9a-fA-F]+", " ").trim(); // integer numbers as hex
		text = text.replaceAll("(^\\s)[0-9]+", " ").trim(); // integer numbers
		text = text.replaceAll("\\s+", " ");

		// Split CamelCase
		if (!text.isEmpty()) {
			for (String word : text.split("\\s+")) {
				if (word.length() >= minWordSize) {
					if (!stopWords.contains(word.toLowerCase())) {

						String[] explodedWord = word.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"); // CamelCase splitter

						if (explodedWord.length > 1) {
							if (includeCamelCase) {
								output.add((stemWords) ? stemmer.stem(word) : word);
							}

							for (String w : explodedWord) {
								if (w.length() >= minWordSize && !stopWords.contains(w.toLowerCase())) {
									output.add((stemWords) ? stemmer.stem(w) : w);
								}
							}
						} else {
							output.add((stemWords) ? stemmer.stem(word) : word);
						}
					}
				}
			}
		}

		return output;
	}

	public static void main(String... args) {
		for (String s : "nonCamelCased".split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
			System.out.println(s);
		}
	}
}

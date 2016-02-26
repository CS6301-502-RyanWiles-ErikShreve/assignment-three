package edu.utdallas.cs6301_502;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TextScrubber {

	private Set<String> keywordSet = new HashSet<String>();
	private Set<String> stopWords = new HashSet<String>();
	private int minWordSize;
	

	public TextScrubber(Set<String> keywordSet, Set<String> stopWords, int minWordSize) {
		super();
		this.keywordSet = keywordSet;
		this.stopWords = stopWords;
		this.minWordSize = minWordSize;		
	}
	
	public List<String> scrub(String text)  {

		PorterStemmer stemmer = new PorterStemmer();
		
		List<String> output = new ArrayList<String>();

		text = text.trim();

		if (text.isEmpty()) {
			return output;
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
					if (!keywordSet.contains(word.toLowerCase()) && !stopWords.contains(word.toLowerCase())) {
						output.add(stemmer.stem(word));

						String[] explodedWord = word.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");

						if (explodedWord.length > 1) {
							for (String w : explodedWord) {
								if (w.length() >= minWordSize && !stopWords.contains(word.toLowerCase())) { 
									output.add(stemmer.stem(w));
								}
							}
						}
					}
				}
			}
		}

		return output;
	}
	
}

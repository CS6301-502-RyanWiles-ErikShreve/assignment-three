package edu.utdallas.cs6301_502.jira;

public class IssueData {
	public long issueNumber;
	public long numSentences;
	public long numTerms;
	public long numNouns;
	public long numVerbs;
	public long numAdjectives;
	public long numAdverbs;

	public long getIssueNumber() {
		return issueNumber;
	}

	public long getNumSentences() {
		return numSentences;
	}

	public long getNumTerms() {
		return numTerms;
	}

	public long getNumNouns() {
		return numNouns;
	}

	public long getNumVerbs() {
		return numVerbs;
	}

	public long getNumAdjectives() {
		return numAdjectives;
	}

	public long getNumAdverbs() {
		return numAdverbs;
	}

	@Override
	public String toString() {
		return "IssueData [issueNumber=" + issueNumber + ", numSentences=" + numSentences + ", numTerms=" + numTerms
				+ ", numNouns=" + numNouns + ", numVerbs=" + numVerbs + ", numAdjectives=" + numAdjectives
				+ ", numAdverbs=" + numAdverbs + "]";
	}

}

package edu.utdallas.cs6301_502.jira;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.utdallas.cs6301_502.jira.IssueData;

public class CorpusData
{
	private String corpusName;
	public List<IssueData> issues;
	public int numEmptyIssues;
	
	public HashMap<String, Long> nounMap;
	public HashMap<String, Long> verbMap;
	public HashMap<String, Long> adjectiveMap;
	public HashMap<String, Long> adverbMap;
	
	public CorpusData(String name) {
		super();
		
		corpusName = name;
		
		issues = new ArrayList<IssueData>();
		numEmptyIssues = 0;
		nounMap = new HashMap<String, Long>();
		verbMap = new HashMap<String, Long>();
		adjectiveMap = new HashMap<String, Long>();
		adverbMap = new HashMap<String, Long>();
	}
	
	public String getName()
	{
		return corpusName;
	}
	
	
}
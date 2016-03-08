package edu.utdallas.cs6301_502.jira;

public class JiraMarkupScrubber {
	

	static public String scrub(String text)
	{
		String output = text;
		
		if (text == null) {return "";}
				
		// Misc - done first as these are very specific sequences and some
		// use characters used elsewhere.
		output = output.replaceAll("\\:\\)", "");
		output = output.replaceAll("\\:\\(", "");
		output = output.replaceAll("\\:P", "");
		output = output.replaceAll("\\:D", "");
		output = output.replaceAll("\\;\\)", "");
		output = output.replaceAll("\\(y\\)", "");
		output = output.replaceAll("\\(n\\)", "");
		output = output.replaceAll("\\(i\\)", "");
		output = output.replaceAll("\\(\\/\\)", "");
		output = output.replaceAll("\\(x\\)", "");
		output = output.replaceAll("\\(\\!\\)", "");
		output = output.replaceAll("\\(\\+\\)", "");
		output = output.replaceAll("\\(-\\)", "");
		output = output.replaceAll("\\(\\?\\)", "");
		output = output.replaceAll("\\(on\\)", "");
		output = output.replaceAll("\\(off\\)", "");
		output = output.replaceAll("\\(\\*\\)", "");
		output = output.replaceAll("\\(\\*r\\)", "");
		output = output.replaceAll("\\(\\*g\\)", "");
		output = output.replaceAll("\\(\\*b\\)", "");
		output = output.replaceAll("\\(\\*y\\)", "");
		output = output.replaceAll("\\(flag\\)", "");
		output = output.replaceAll("\\(flagoff\\)", "");
		
		// Headers
		output = output.replaceAll("h[1-6]\\. *", "");
		
		// Text Effects
		output = output.replaceAll("\\{\\{|\\}\\}|\\?\\?", ""); // citation and monospaced
		output = output.replaceAll("\\*(.*?)\\*", "$1"); // Bold pattern must avoid emoticon usage
		output = output.replaceAll("\\_(.*?)\\_", "$1");
		output = output.replaceAll("(\\W)\\-(.*?)\\-(\\W)", "$1$2$3");
		output = output.replaceAll("\\+(.*?)\\+", "$1");
		output = output.replaceAll("\\^(.*?)\\^", "$1");
		output = output.replaceAll("\\~(.*?)\\~", "$1");
		
		output = output.replaceAll("(\n|\\|)bq\\. *", "$1"); // bq. must start at new line or be in column
		
		output = output.replaceAll("\\{quote\\}", "");
		
		output = output.replaceAll("\\s+\\{color\\}\\s+", " ");
		output = output.replaceAll("\\s+\\{color\\:.*\\}\\s+", " ");
		
		// New line
		output = output.replaceAll("\\\\", "\n");
		
		// Horizontal rule, en-dash, and em-dash.
		output = output.replaceAll("\\-{2,4}", "");
		
		// Links
		output = output.replaceAll("\\[(.*?)\\|.*?\\]", "$1"); // Remove links, but keep link text
		output = output.replaceAll("\\[(.*?)\\]", ""); // Remove links, that don't have link text
		output = output.replaceAll("\\{anchor:.*?\\}", "");		
		
		output = output.replaceAll("(\n|\\|)(\\*|-|#)+\\s+", "$1");  // list must start at new line or be in column

		output = output.replaceAll("!(.*)!", " "); // Replace images and attachments that can be embedded
		
		// Replace Table notations
		output = output.replaceAll("\\|\\|", " ");
		output = output.replaceAll("\\|", " "); // Don't do this one until after links are processed since the pipe is part of the link notation

		// Advanced formatting (except noformat)
		output = output.replaceAll("\\s+\\{panel\\}\\s+", " ");
		output = output.replaceAll("\\s+\\{panel\\:.*\\}\\s+", " ");
		output = output.replaceAll("\\s+\\{code\\}\\s+", " ");
		output = output.replaceAll("\\s+\\{code\\:.*\\}\\s+", " ");
		
		// noformat
		output = output.replaceAll("\\s+\\{noformat\\}\\s+", " ");
		


		
		return output;
	}
}

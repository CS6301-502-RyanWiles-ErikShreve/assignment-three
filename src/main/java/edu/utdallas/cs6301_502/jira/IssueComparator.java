package edu.utdallas.cs6301_502.jira;

import java.util.Comparator;

import org.apache.commons.beanutils.BeanUtils;

public class IssueComparator implements Comparator<IssueData> {

	String fieldName = null;

	public IssueComparator(String fieldNameToCompare) {
		this.fieldName = fieldNameToCompare;
	}

	public int compare(IssueData a, IssueData b) {
		try {
			long aValue = Long.parseLong(BeanUtils.getSimpleProperty(a, fieldName));
			long bValue = Long.parseLong(BeanUtils.getSimpleProperty(b, fieldName));

			if (aValue > bValue) {
				return 1;
			} else if (aValue < bValue) {
				return -1;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return 0;
	}
}

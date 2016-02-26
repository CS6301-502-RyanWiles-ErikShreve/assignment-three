package edu.utdallas.cs6301_502;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class LuceneUtil {
	private boolean createMode;
	private Path indexPath;
	private Directory directory;

	public LuceneUtil(boolean createMode, String indexDir) throws IOException {
		this.createMode = createMode;
		indexPath = new File(indexDir).toPath();

		directory = FSDirectory.open(indexPath);
	}

	public static void main(String[] args) {
		LuceneUtil parser;
		try {
			parser = new LuceneUtil(true, "/Users/rwiles/lucene/index");

			parser.indexDocument("my file", "document title or id goes here", "document body that you'll query on goes here");
			List<Document> docs = parser.queryLucene("your query goes here", 20);
			for (Document doc : docs) {
				System.out.println("Found matching document with the following title: " + doc.getField("title").stringValue());

			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private IndexWriter indexWriter;
	
	public void openIndexForAdd() throws IOException
	{
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

		if (createMode) {
			// Create a new index in the directory, removing any previously indexed documents:
			iwc.setOpenMode(OpenMode.CREATE);
		} else {
			// Add new documents to an existing index:
			iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		}

		indexWriter = new IndexWriter(directory, iwc);//  to write the document to the index database  
	}
	
	public void closeIndexForAdd() throws IOException
	{
		indexWriter.close();
	}
	
	public void indexDocument(String fileName, String title, String body) throws IOException {


		String id = fileName + "_" + title;
		
		Document document = new Document();
		Field idField = new StringField("id", id, Store.NO);
		document.add(idField);

		Field fileField = new StoredField("fileName", fileName);
		document.add(fileField);

		Field titleField = new StringField("title", title, Store.YES);
		document.add(titleField);

		Field bodyField = new TextField("body", body, Store.YES);
		document.add(bodyField);
		
		if (indexWriter.getConfig().getOpenMode() == OpenMode.CREATE) {
			indexWriter.addDocument(document);
		} else {
			indexWriter.updateDocument(new Term("id", id), document);
		}
	}

	public List<Document> queryLucene(String queryString, int limit) throws IOException, ParseException {
		IndexSearcher searcher;
		List<Document> docs = new ArrayList<Document>();

		IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
		searcher = new IndexSearcher(reader);
		Analyzer analyzer = new StandardAnalyzer();

		MultiFieldQueryParser mfqp = new MultiFieldQueryParser(new String[] {"fileName", "title", "body"}, analyzer);

		Query query = mfqp.parse(queryString);
		TopDocs results = searcher.search(query, limit);
		ScoreDoc[] hits = results.scoreDocs;

		for (ScoreDoc scoreDoc : hits) {
			docs.add(searcher.doc(scoreDoc.doc));
//			System.out.println(scoreDoc.score);
		}

		reader.close();
		return docs;
	}

}

import org.aksw.sdw.input.warc.WarcInfoRecord;
import org.aksw.sdw.input.warc.WarcReader;
import org.aksw.sdw.input.warc.WarcRecord;
import org.aksw.sdw.search.solr.SolrHandler;
import org.aksw.sdw.search.solr.WarcSolrDocument;
import org.apache.solr.common.SolrDocument;

/**
 * Test class which can be used to read WARC files.
 * 
 * @author kay
 *
 */
public class Main {
	
	static public void main(String args[]) {
		System.out.println("Hello World");
		
		int max = -1;
		int count = 0;
		try {
			//String filePath = "0013wb-88.warc.gz";
			//String filePath = "0013wb-88.warc";
			String filePath = "test.warc";
			WarcReader warcReader = new WarcReader(filePath);
			
			SolrHandler solrHandler = new SolrHandler("http://localhost:9983/solr/warc");
			if (false) {
				// make sure nothing is in the store
				solrHandler.deleteAllDocuments();
				solrHandler.close();
				return;				
			}
			
			while (warcReader.hasNext()) {
				WarcRecord warcRecord = warcReader.next();
				System.out.println("Date: " + warcRecord.getWarcDate());
				count++;
				
				WarcInfoRecord warcInfoRecord = warcReader.getWarcInfoRecord();
				if (null != warcInfoRecord) {
					max = warcInfoRecord.getWarcNumberOfDocuments();
				}
				
				WarcSolrDocument solrDocument = new WarcSolrDocument(warcRecord);
				solrHandler.addSolrDocument(solrDocument);
			}
			
			solrHandler.close();
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			System.out.println("Counted: " + count + " of " + max);
		}
	}
}

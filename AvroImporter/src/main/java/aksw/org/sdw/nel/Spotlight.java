package aksw.org.sdw.nel;

import aksw.org.sdw.importer.avro.annotations.GlobalConfig;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.solr.client.solrj.cloud.autoscaling.Row;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.beans.XMLEncoder;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.logging.Logger;

public class Spotlight {
    private String hostUrl;

    public Spotlight(String hostUrl) {
        this.hostUrl = hostUrl;
    }

    public String getLink(String entity) {
        String link = "";
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\"?>");
            xml.append("<annotation text=\"");
            xml.append(StringEscapeUtils.escapeXml(entity));
            xml.append("\">");
            xml.append("<surfaceForm  name=\"");
            xml.append(StringEscapeUtils.escapeXml(entity));
            xml.append("\" offset=\"0\"/>");
            xml.append("</annotation>");

            String getUrl = hostUrl+"?types=DBpedia%3AOrganisation&confidence=0.5&text="+URLEncoder.encode(xml.toString());//Siemens%20Opel%20Aktiengesellschaft";
            HttpGet request = new HttpGet(getUrl);
            request.addHeader("accept", "application/json");
            HttpResponse result = httpClient.execute(request);
            String json = EntityUtils.toString(result.getEntity(), "UTF-8");
            try {
                JSONParser jsonParser = new JSONParser();
                JSONObject jsonObject = (JSONObject) jsonParser.parse(json);
                JSONArray jsonArray =  (JSONArray) jsonObject.get("Resources");
                if ( null != jsonArray) {
                    JSONObject firstresult = (JSONObject) jsonArray.get(0);
                    link = (String) firstresult.get("@URI");
                }
            } catch (Exception e) {
                Logger.getGlobal().warning(xml.toString());
                e.printStackTrace();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return link;
    }
}

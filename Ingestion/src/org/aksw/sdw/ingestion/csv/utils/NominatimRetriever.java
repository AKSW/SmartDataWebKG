package org.aksw.sdw.ingestion.csv.utils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.aksw.sdw.ingestion.IngestionException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * This class can be used to retrieve information from a
 * Nominatim Server
 * 
 * @author kay
 *
 */
public class NominatimRetriever implements Closeable {
	
	/** Nominatim server address */
	final String nominatimServerAddress;
	
	/** client instance which is used to communicate with Nominatim server */
	CloseableHttpClient client = HttpClients.createDefault();
	
	public NominatimRetriever(final String nominatimServerAddress) {
		this.nominatimServerAddress = nominatimServerAddress;
	}
	
	/**
	 * This method can be called after close has been called
	 * or if a new connection should be established
	 */
	public void open() {
		try {
			this.close();
		} catch (Exception e) {
			// ignore
		}
		
		this.client = HttpClients.createDefault();
	}
	
	@Override
	public void close() throws IOException {
		if (null != this.client) {
			this.client.close();			
		}
		
		this.client = null;
	}
	
	/**
	 * This method can be used to reverse geo code cooridinates to an address
	 * 
	 * @param longitude
	 * @param latitude
	 * @return Address instance
	 * @throws IngestionException
	 */
	public AddressInformation getAddressInformationFromCoordinates(final String longitude, final String latitude) throws IngestionException{
		if (null == longitude || null == latitude) {
			return null;
		}
		
		String searchParameters = "/reverse?format=json&lat=" + latitude +
				"&lon=" + longitude + "&zoom=18&addressdetails=1";
		
		final String queryString = String.format("%s%s", this.nominatimServerAddress, searchParameters);
		HttpGet get = new HttpGet(queryString);
		JSONObject response = null;
		int retries = 10;
		do {			
			try {
				response = client.execute(get, new ResponseHandler<JSONObject>() {
					
				@Override
				public JSONObject handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
					// if no response or empty response is returned --> null
					if (null == response || 2 == response.getEntity().getContentLength()) {
						return null;
					}
					
					InputStream contentStream = response.getEntity().getContent();
					if (null == contentStream) {
						return null;
					}
					InputStreamReader contentStreamReader = new InputStreamReader(contentStream);
					BufferedReader reader = new BufferedReader(contentStreamReader);
	
					// get the response json string
					String line = reader.readLine();
					try {
						JSONObject jsonArray = new JSONObject(line);
						if (1 <= jsonArray.length()) {
							return jsonArray;
						} else {
							return null;
						}
						
					} catch (JSONException e) {
						throw new ClientProtocolException(e);
					}
				}});
			} catch (Exception e) { 
				try {Thread.sleep(300);} catch (Exception e1) {}
				continue;
			}
			
			break; // get out of loop
		} while (0 < retries--); 
		
		if (null == response) {
			return null;
		}
		
		try {
			JSONObject address = (JSONObject) response.get("address");
			if (null == address) {
				return null;
			}
			
			AddressInformation addressInformation = new AddressInformation(address);
			return addressInformation;
		} catch (JSONException e) {
			throw new IngestionException("Problem retrieving address data", e);
		}

	}
	

	/**
	 * This class can be used to record address information
	 */
	public static class AddressInformation {
		
		final public String road;
		final public String suburb;
		final public String town;
		final public String county;
		final public String stateDistrict;
		final public String state;
		final public String postcode;
		final public String country;		
		final public String countryCode;
		
		public AddressInformation(final JSONObject addressObject) throws IngestionException {
			try {
				if (addressObject.has("road"))
					this.road = (String) addressObject.get("road");
				else
					this.road = null;
				
				if (addressObject.has("suburb"))
					this.suburb = (String) addressObject.get("suburb");
				else
					this.suburb = null;
				
				if (addressObject.has("town"))
					this.town = (String) addressObject.get("town");
				else
					this.town = null;
				
				if (addressObject.has("county"))
					this.county = (String) addressObject.get("county");
				else
					this.county = null;
				
				if (addressObject.has("state_district"))
					this.stateDistrict = (String) addressObject.get("state_district");
				else
					this.stateDistrict = null;
				
				if (addressObject.has("state"))
					this.state = (String) addressObject.get("state");
				else
					this.state = null;
				
				if (addressObject.has("postcode"))
					this.postcode = (String) addressObject.get("postcode");
				else
					this.postcode = null;
				
				if (addressObject.has("country"))
					this.country = (String) addressObject.get("country");
				else
					this.country = null;
				
				if (addressObject.has("country_code"))
					this.countryCode = (String) addressObject.get("country_code");
				else
					this.countryCode = null;
			} catch (Exception e) {
				throw new IngestionException("Was not able to read address JSON", e);
			}			
		}		
	}


	
}

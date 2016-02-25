package com.resource.arduino;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/* HTTP request via GET method*/
public class HttpRequest {

	public static String requestGET(String URL) {
		String response = null;
		int timeoutConnection = 5000;
		int timeoutSocket = 7000;
		
		try {
			// Define connection and socket timeouts
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			
			// Prepare client object
			HttpClient client = new DefaultHttpClient(httpParameters);
			HttpGet get = new HttpGet(URL);
			
			// Make GET request
			HttpResponse responseGet = client.execute(get);
			HttpEntity resEntityGet = responseGet.getEntity();
			if (resEntityGet != null) {
				response = readResponse(resEntityGet.getContent());
			}
		} catch (Exception e) {
			response = null;
		}
		return response;
	}

	/* Convert response InputStream in a readable string type */
	private static String readResponse(InputStream responseStream) {
		String response = null;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					responseStream), 4096);
			String line;
			StringBuilder stringBuilder = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
			}
			reader.close();
			response = stringBuilder.toString();
		} catch (IOException e) {
			response = null;
		}
		return response;
	}
}

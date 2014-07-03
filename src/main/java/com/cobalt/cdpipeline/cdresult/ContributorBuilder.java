package com.cobalt.cdpipeline.cdresult;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class ContributorBuilder {
	private String hostname;
	private String loginname;
	private String password;
	
	public ContributorBuilder(String hostname, String username, String password){
		this.hostname = hostname;
		this.password = password;
		this.loginname = username;
	}
	
	public Contributor createContributor(String username, Date lastCommitDate){				
		try {
			URL url = new URL("https://" + hostname + "/rest/api/2/user?username=" + username);			
			URLConnection conn = url.openConnection();
			String loginPassword = loginname + ":" + password;	
			String encoded = new sun.misc.BASE64Encoder().encode (loginPassword.getBytes());
			conn.setRequestProperty ("Authorization", "Basic " + encoded);		
			InputStream is = conn.getInputStream();
			
			// get all info needed to construct a Contributor obj (fullname, picurl, pageurl)
			String userJsonStr = IOUtils.toString(is, "UTF-8");
			JSONObject userJsonObj = new JSONObject(userJsonStr);
			String fullname = userJsonObj.getString("displayName"); 
			JSONObject picsAllSizesJsonObj = new JSONObject(userJsonObj.getString("avatarUrls")); // same pic w/ various sizes
			String pictureUrl = picsAllSizesJsonObj.getString("48x48");			
			String profilePageUrl = "https://" + hostname + "/secure/ViewProfile.jspa?name=" + username; // page on Jira
			
			return new Contributor(username, lastCommitDate, fullname, pictureUrl, profilePageUrl); // TODO change date
		} catch(MalformedURLException e) {
			e.printStackTrace();
			return new Contributor(username, lastCommitDate, null, null, null);
		} catch(IOException e) {
			e.printStackTrace();
			return new Contributor(username, lastCommitDate, null, null, null);
		} catch(JSONException e) {
			e.printStackTrace();
			return new Contributor(username, lastCommitDate, null, null, null);
		}
	}
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.lm.server.social;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.Blog;
import com.google.api.services.blogger.model.BlogList;
import com.google.api.services.blogger.model.Post;
import com.jstakun.lm.server.config.Commons;
import com.jstakun.lm.server.config.Commons.Property;
import com.jstakun.lm.server.config.ConfigurationManager;
import com.jstakun.lm.server.persistence.Landmark;
import com.jstakun.lm.server.utils.UrlUtils;
import com.jstakun.lm.server.utils.memcache.CacheUtil;
import com.jstakun.lm.server.utils.persistence.LandmarkPersistenceUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class GoogleBloggerUtils {

    private static final Logger logger = Logger.getLogger(GoogleBloggerUtils.class.getName());
    private static final String CACHE_KEY = "BloggerUsageLimitsMarker";
    
    protected static void sendMessage(String key, String landmarkUrl, String token, String secret, boolean isServer) {
        Landmark landmark = LandmarkPersistenceUtils.selectLandmarkById(key);
        if (landmark != null && token != null && secret != null) {
            String message = null;
            String url = landmarkUrl;
            if (url == null) {
                url = UrlUtils.getShortUrl(UrlUtils.getLandmarkUrl(landmark));
            }

            if (isServer) {
                String username = landmark.getUsername();
                String userMask = UrlUtils.createUsernameMask(username);
                if (username != null) {
                    userMask = "<a href=\"" + ConfigurationManager.SERVER_URL + "showUser/" + username + "\">" + userMask + "</a>";
                }
                message = userMask + " has just posted new point of interest " + landmark.getName() + " to GMS World. <a href=\"" + url + "\">Check it out</a>.";
            } else {
                if (landmark.getLayer().equals("Social")) {
                    message = "I've just posted new geo message to Blogeo. <a href=\"" + url + "\">Check it out</a>.";
                } else {
                    message = "I've just posted point of interest " + landmark.getName() + " to GMS World. Please check <a href=\"" + url + "\">here</a>.";
                }
            }
            if (message != null) {
                //createPost(getBloggerService(), landmark.getName(), message, false);

                createPost(getBlogger(), landmark.getName(), message);
            }
        } else {
            logger.log(Level.SEVERE, "Landmark or token is empty! Key: {0}, token: {1}, secret: {2}", new Object[]{key, token, secret});
        }
    }

    protected static void sendImageMessage(String showImageUrl, String username, String imageUrl) {
        String userMask = UrlUtils.createUsernameMask(username);
        if (StringUtils.isNotEmpty(username)) {
            userMask = "<a href=\"" + ConfigurationManager.SERVER_URL + "showUser/" + username + "\">" + userMask + "</a>";
        }
        String prefix = "<a href=\"" + showImageUrl + "\" "
                + "imageanchor=\"1\" style=\"clear: left; cssfloat: left; float: left; margin-bottom: 1em; margin-right: 1em;\">"
                + "<img border=\"0\" src=\"" + imageUrl + "\" ya=\"true\" /></a>";
        String message = prefix + userMask + " has just posted new screenshot to GMS World. <a href=\"" + showImageUrl + "\">Check it out</a>.";

        //createPost(getBloggerService(), "GMS World screenshot", message, false);

        createPost(getBlogger(), "GMS World screenshot", message);
    }

    private static void createPost(Blogger blogger, String title, String content) {
        try {
            BlogList blogList = blogger.blogs().listByUser("self").execute();
            List<Blog> blogs = blogList.getItems();
            Blog blog = null;
            if (!blogs.isEmpty()) {
                blog = blogs.get(0);
            }

            if (blog != null) {
                if (!CacheUtil.containsKey(CACHE_KEY)) {
                	Post post = new Post();
                	post.setTitle(title);
                	post.setContent(content);
                	Post postResp = blogger.posts().insert(blog.getId(), post).execute();
                	logger.log(Level.INFO, "Successfully created post: {0} at blog {1}", new Object[]{postResp.getId(), blog.getId()});
                } else {
                	logger.log(Level.WARNING, "Blogger Rate Limit Exceeded");
                }
            } else {
                logger.log(Level.INFO, "No blogs found for the user!");
            }
        } catch (GoogleJsonResponseException ex) {
        	int status = ex.getStatusCode();
        	if (status == 403) {
        		CacheUtil.put(CACHE_KEY, "1");
        	}
        	logger.log(Level.SEVERE, "GoogleBloggerUtils.createPost() exception with error " + status, ex);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "GoogleBloggerUtils.createPost() exception", ex);
        }
    }

    private static Blogger getBlogger() {
        HttpTransport httpTransport = new UrlFetchTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        GoogleCredential requestInitializer = new GoogleCredential.Builder().setClientSecrets(Commons.getProperty(Property.GL_PLUS_KEY), Commons.getProperty(Property.GL_PLUS_SECRET)).setJsonFactory(jsonFactory).setTransport(httpTransport).build();
        requestInitializer.setAccessToken(Commons.getProperty(Property.gl_plus_token)).setRefreshToken(Commons.getProperty(Property.gl_plus_refresh));
        Blogger blogger = new Blogger.Builder(httpTransport, jsonFactory, requestInitializer).setApplicationName("Landmark Manager").build();

        return blogger;
    }

	protected static int checkin(String reference) {
	    int result = -1;
	    
	    try {
	    	HttpTransport httpTransport = new UrlFetchTransport();
	        GenericUrl url = new GenericUrl("https://maps.googleapis.com/maps/api/place/check-in/json?sensor=false&key=" + Commons.getProperty(Property.GOOGLE_API_KEY));
	        Map<String, String> data = new HashMap<String, String>();
	        data.put("reference", reference);
	        JsonHttpContent content = new JsonHttpContent(new JacksonFactory(), data);
	        HttpRequest request = httpTransport.createRequestFactory().buildPostRequest(url, content);
	        result = request.execute().getStatusCode();
	    } catch (Throwable e) {
	        logger.log(Level.SEVERE, "GoogleBloggerUtils.checkin() exception", e);   
	        result = 500;
	    }
	
	    return result;
	}
}

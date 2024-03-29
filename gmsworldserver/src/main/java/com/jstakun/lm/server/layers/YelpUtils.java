/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.lm.server.layers;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;
import com.google.gdata.client.authn.oauth.OAuthParameters;
import com.google.gdata.client.authn.oauth.OAuthUtil;
import com.jstakun.gms.android.deals.Deal;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkFactory;
import com.jstakun.lm.server.config.Commons;
import com.jstakun.lm.server.config.Commons.Property;
import com.jstakun.lm.server.utils.AuthUtils;
import com.jstakun.lm.server.utils.HttpUtils;
import com.jstakun.lm.server.utils.JSONUtils;
import com.jstakun.lm.server.utils.NumberUtils;
import com.jstakun.lm.server.utils.StringUtil;
import com.jstakun.lm.server.utils.ThreadUtil;
import com.jstakun.lm.server.utils.memcache.CacheUtil;
import com.openlapi.AddressInfo;
import com.openlapi.QualifiedCoordinates;

/**
 *
 * @author jstakun
 */
public class YelpUtils extends LayerHelper {

	private static final String CACHE_KEY = "YelpUsageLimitsMarker";
	
    @Override
    protected JSONObject processRequest(double lat, double lng, String query, int radius, int version, int limit, int stringLimit, String hasDeals, String language) throws Exception {
        int normalizedRadius = NumberUtils.normalizeNumber(radius, 1000, 40000);
        int normalizedLimit = NumberUtils.normalizeNumber(limit, 20, 100);
        String key = getCacheKey(getClass(), "processRequest", lat, lng, query, normalizedRadius, version, normalizedLimit, stringLimit, hasDeals, language);

        String cachedResponse = CacheUtil.getString(key);
        if (cachedResponse == null) {
        	List<Object> venueArray = new ArrayList<Object>();
            
        	if (!CacheUtil.containsKey(CACHE_KEY)) {
        		Map<Integer, Thread> venueDetailsThreads = new ConcurrentHashMap<Integer, Thread>();
        		boolean isDeal = Boolean.parseBoolean(hasDeals);
        		int offset = 0;

        		while (offset < normalizedLimit) {
        			Thread venueDetailsRetriever = ThreadUtil.newThread(new VenueDetailsRetriever(venueDetailsThreads, venueArray,
                        lat, lng, query, normalizedRadius, offset, isDeal, stringLimit, language, "json", null));
        			venueDetailsThreads.put(offset, venueDetailsRetriever);
        			venueDetailsRetriever.start();
        			offset += 20;
        		}

        		ThreadUtil.waitForLayers(venueDetailsThreads);
            
        		if (venueArray.size() > normalizedLimit) {
        			venueArray = venueArray.subList(0, normalizedLimit);
        		}
            
        	} else {
            	logger.log(Level.WARNING, "Yelp Rate Limit Exceeded");
            }

            JSONObject json = new JSONObject().put("ResultSet", venueArray);

            if (!venueArray.isEmpty()) {
                CacheUtil.put(key, json.toString());
                logger.log(Level.INFO, "Adding YP landmark list to cache with key {0}", key);
            }

            return json;
        } else {
            logger.log(Level.INFO, "Reading YP landmark list from cache with key {0}", key);
            return new JSONObject(cachedResponse);
        }
    }

    private static String processRequest(double latitude, double longitude, String query, int radius, boolean hasDeals, int offset, String language) throws OAuthException, IOException {
    	String responseBody = null;
    	
    	if (!CacheUtil.containsKey(CACHE_KEY)) {
    	
    		OAuthHmacSha1Signer hmacSigner = new OAuthHmacSha1Signer();
    		OAuthParameters parameters = new OAuthParameters();
    		parameters.setOAuthConsumerKey(Commons.getProperty(Property.YELP_Consumer_Key));
    		parameters.setOAuthConsumerSecret(Commons.getProperty(Property.YELP_Consumer_Secret));
    		parameters.setOAuthToken(Commons.getProperty(Property.YELP_Token));
    		parameters.setOAuthTokenSecret(Commons.getProperty(Property.YELP_Token_Secret));
    		parameters.setOAuthTimestamp(Long.toString(System.currentTimeMillis()));
    		int nonce = (int) (Math.random() * 1e8);
    		parameters.setOAuthNonce(Integer.toString(nonce));
    		parameters.setOAuthSignatureMethod("HMAC-SHA1");

    		//sort: Sort mode: 0=Best matched (default), 1=Distance, 2=Highest Rated
    		String urlString = "http://api.yelp.com/v2/search?ll=" + StringUtil.formatCoordE6(latitude) + "," + StringUtil.formatCoordE6(longitude) + "&radius_filter=" + radius; // + "&sort=1";

    		if (StringUtils.isNotEmpty(query)) {
    			urlString += "&term=" + URLEncoder.encode(query, "UTF-8");
    		}
        
    		if (offset >= 0) {
    			urlString += "&offset=" + offset;
    		}
        
    		if (hasDeals) {
    			urlString += "&deals_filter=true";
    		}
        
    		if (StringUtils.isNotEmpty(language)) {
    			urlString += "&lang=" + language + "&cc=" + language;
    		}

    		//System.out.println("Calling: " + urlString);

    		String baseString = OAuthUtil.getSignatureBaseString(urlString, "GET", parameters.getBaseParameters());
    		String signature = hmacSigner.getSignature(baseString, parameters);
    		parameters.addCustomBaseParameter("oauth_signature", signature);

    		responseBody = HttpUtils.processFileRequestWithAuthn(new URL(urlString), AuthUtils.buildAuthHeaderString(parameters));

    		//System.out.println(responseBody);
    	}

        return responseBody;
    }

    protected static boolean hasNeighborhoods(double lat, double lng) throws IOException, JSONException {
        String url = "http://api.yelp.com/neighborhood_search?lat=" + StringUtil.formatCoordE6(lat) + "&long=" + StringUtil.formatCoordE6(lng) + "&ywsid=" + Commons.getProperty(Property.YELP_ywsid);
        String json = HttpUtils.processFileRequest(new URL(url));
        boolean hasNeighborhood = false;
        if (StringUtils.startsWith(json, "{")) {
            JSONObject jsonRoot = new JSONObject(json);
            JSONArray neighborhoods = jsonRoot.optJSONArray("neighborhoods");
            if (neighborhoods != null && neighborhoods.length() > 0) {
                hasNeighborhood = true;
            } else {
                logger.log(Level.INFO, "Location [{0},{1}] has no neighborhood.", new Object[]{lat, lng});
            }
        }
        return hasNeighborhood;
    }

    private static int createCustomJsonYelpList(String yelpJson, List<Object> jsonArray, int stringLimit, boolean hasDeals) throws JSONException {
        int total = 0;
        if (StringUtils.startsWith(yelpJson, "{")) {
            JSONObject jsonRoot = new JSONObject(yelpJson);
            if (jsonRoot.has("total")) {
                total = jsonRoot.getInt("total");
                if (total > 0) {
                    //System.out.println("total: " + total);
                    JSONArray businesses = jsonRoot.getJSONArray("businesses");
                    for (int i = 0; i < businesses.length(); i++) {
                        JSONObject business = businesses.getJSONObject(i);

                        Map<String, Object> jsonObject = new HashMap<String, Object>();

                        JSONObject location = business.getJSONObject("location");
                        JSONObject coordinate = location.getJSONObject("coordinate");

                        jsonObject.put("name", business.getString("name"));
                        jsonObject.put("lat", Double.toString(coordinate.getDouble("latitude")));
                        jsonObject.put("lng", Double.toString(coordinate.getDouble("longitude")));
                        jsonObject.put("url", business.getString("mobile_url"));

                        Map<String, String> desc = new HashMap<String, String>();

                        //add desc
                        JSONUtils.putOptValue(desc, "phone", business, "display_phone", false, stringLimit, false);
                        if (business.has("rating")) {
                            desc.put("rating", Double.toString(business.getDouble("rating")));
                        }
                        if (business.has("review_count")) {
                            desc.put("numberOfReviews", Integer.toString(business.getInt("review_count")));
                        }

                        if (location.has("display_address")) {
                            JSONArray displayAddressArray = location.getJSONArray("display_address");
                            String display_address = "";
                            for (int j = 0; j < displayAddressArray.length(); j++) {
                                if (display_address.length() > 0) {
                                    display_address += ", ";
                                }
                                display_address += displayAddressArray.getString(j);
                            }
                            desc.put("address", display_address);
                        }

                        String icon = business.optString("image_url");
                        if (StringUtils.isNotEmpty(icon)) {
                            desc.put("icon", icon);
                        }
                        
                        JSONUtils.putOptValue(desc, "description", business, "snippet_text", false, stringLimit, false);

                        //categories
                        String category = "";
                        JSONArray categories = business.optJSONArray("categories");
                        if (categories != null && categories.length() > 0) {
                        	String[] categoryCodes = new String[categories.length()];
                        	for (int j = 0; j < categories.length() ; j++) {
                        		JSONArray cat = categories.getJSONArray(j);
                        		category += cat.getString(0);
                        		categoryCodes[j] = cat.getString(1);
                        		if (j <  categories.length()-1) {
                        			category += ", ";
                        		}
                        	}
                        	if (StringUtils.isNotEmpty(category)) {
                        		desc.put("category", category);
                        	}
                        	if (hasDeals) {
                        		String[] categoryCode = YelpCategoryMapping.findMapping(categoryCodes);
                        		jsonObject.put("categoryID", categoryCode[0]);
                        		String subcat = categoryCode[1];
                        		if (StringUtils.isNotEmpty(subcat)) {
                        			jsonObject.put("subcategoryID", subcat);
                        		}
                        	}
                        }
                        
                        //deals
                        JSONArray deals = business.optJSONArray("deals");
                        if (deals != null && deals.length() > 0) {
                        	for (int d = 0; d < deals.length(); d++) {
                                JSONObject deal = deals.getJSONObject(d);
                                
                                desc.put("start_date", Long.toString(deal.getLong("time_start")*1000));
                                if (deal.has("time_end")) {
                                	desc.put("end_date", Long.toString(deal.getLong("time_end")*1000));
                                }
                                jsonObject.put("url", deal.getString("url"));
                                jsonObject.put("name", deal.getString("title") + " Deal At " + business.getString("name"));
                                //desc.put("icon", deal.getString("image_url"));
                                String description = ""; 
                                if (deal.has("what_you_get")) {
                                	description = "<b>What You Get</b><br/>" + deal.getString("what_you_get") + "<br/>";
                                } if (deal.has("important_restrictions")) {
                                	description += "<b>Important Restrictions</b><br/>" + deal.getString("important_restrictions") + "<br/>";
                                } if (deal.has("additional_restrictions")) {
                                	description += "<b>Additional Restrictions</b><br/>" + deal.getString("additional_restrictions") + "<br/>";
                                }
                                desc.put("description", description);
                                JSONArray options = deal.getJSONArray("options");
                                
                                JSONObject option = options.getJSONObject(0);
                                
                                desc.put("price", option.getString("formatted_price"));
                                
                                double original_price = option.getDouble("original_price");
                                double price = option.getDouble("price");
                                
                                double discount = 100 - (price / original_price * 100);
                                desc.put("discount", Double.toString(discount) + "%");
                        	}     
                        }
                                              
                        jsonObject.put("desc", desc);
                        jsonArray.add(jsonObject);
                    }
                }
            } else {
            	handleError(jsonRoot);
            }
        }
        return total;
    }

    private static int createCustomJsonReviewsList(String yelpJson, Map<String, Map<String, String>> jsonObjects) throws JSONException {
        int total = 0;
        if (StringUtils.startsWith(yelpJson, "{")) {
            JSONObject jsonRoot = new JSONObject(yelpJson);
            if (jsonRoot.has("total")) {
                total = jsonRoot.getInt("total");
                if (total > 0) {
                    //System.out.println("total: " + total);
                    JSONArray businesses = jsonRoot.getJSONArray("businesses");
                    for (int i = 0; i < businesses.length(); i++) {
                        JSONObject business = businesses.getJSONObject(i);

                        String phone = business.optString("phone");
                        if (phone != null) {
                            Map<String, String> desc = new HashMap<String, String>();

                            //add desc

                            phone = phone.replaceAll("[^\\d]", "");

                            //System.out.println("Adding review " + phone);

                            if (business.has("rating")) {
                                desc.put("rating", Double.toString(business.getDouble("rating")));
                            }
                            if (business.has("review_count")) {
                                desc.put("numberOfReviews", Integer.toString(business.getInt("review_count")));
                            }

                            jsonObjects.put(phone, desc);
                        }
                    }
                }
            } else {
            	handleError(jsonRoot);
            }
        }

        return total;
    }

    public static Map<String, Map<String, String>> processReviewsRequest(double latitude, double longitude, String query, int radius, int limit, boolean hasDeals, String language) throws JSONException, IOException, OAuthException {
        
    	Map<String, Map<String, String>> reviewsArray = new HashMap<String, Map<String, String>>();
    	
    	if (!CacheUtil.containsKey(CACHE_KEY)) {
        	
    		Map<Integer, Thread> venueDetailsThreads = new ConcurrentHashMap<Integer, Thread>();
    		int normalizedRadius = NumberUtils.normalizeNumber(radius, 1000, 40000);
    		int offset = 0;

    		while (offset < limit) {
    			Thread venueDetailsRetriever = ThreadUtil.newThread(new ReviewDetailsRetriever(venueDetailsThreads, reviewsArray,
                    latitude, longitude, query, normalizedRadius, offset, hasDeals, language));
    			venueDetailsThreads.put(offset, venueDetailsRetriever);
    			venueDetailsRetriever.start();
    			offset += 20;
    		}

    		ThreadUtil.waitForLayers(venueDetailsThreads);
    	} else {
        	logger.log(Level.WARNING, "Yelp Rate Limit Exceeded");
        }

        return reviewsArray;
    }

    private static class ReviewDetailsRetriever implements Runnable {

        private Map<Integer, Thread> reviewDetailsThreads;
        private Map<String, Map<String, String>> reviewsArray;
        private double latitude, longitude;
        private String query, language;
        private int radius, offset;
        private boolean hasDeals;

        public ReviewDetailsRetriever(Map<Integer, Thread> reviewDetailsThreads, Map<String, Map<String, String>> reviewsArray,
                double latitude, double longitude, String query, int radius, int offset, boolean hasDeals, String language) {
            this.reviewDetailsThreads = reviewDetailsThreads;
            this.reviewsArray = reviewsArray;
            this.latitude = latitude;
            this.longitude = longitude;
            this.query = query;
            this.radius = radius;
            this.offset = offset;
            this.hasDeals = hasDeals;
            this.language = language;
        }

        @Override
        public void run() {
            try {
                String responseBody = processRequest(latitude, longitude, query, radius, hasDeals, offset, language);
                createCustomJsonReviewsList(responseBody, reviewsArray);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "ReviewDetailsRetriever.run exception:", e);
            } finally {
                reviewDetailsThreads.remove(offset);
            }
        }
    }

    private static class VenueDetailsRetriever implements Runnable {

        private Map<Integer, Thread> venueDetailsThreads;
        private List<? extends Object> venueArray;
        private double latitude, longitude;
        private String query, language, format;
        private int radius, offset, stringLimit;
        private boolean hasDeals;
        private Locale locale;

        public VenueDetailsRetriever(Map<Integer, Thread> venueDetailsThreads, List<? extends Object> venueArray,
                double latitude, double longitude, String query, int radius,
                int offset, boolean hasDeals, int stringLimit, String language, String format, Locale locale) {
            this.venueDetailsThreads = venueDetailsThreads;
            this.venueArray = venueArray;
            this.latitude = latitude;
            this.longitude = longitude;
            this.query = query;
            this.radius = radius;
            this.offset = offset;
            this.hasDeals = hasDeals;
            this.stringLimit = stringLimit;
            this.language = language;
            this.format = format;
            this.locale = locale;
        }

        @Override
        public void run() {
            try {
                String responseBody = processRequest(latitude, longitude, query, radius, hasDeals, offset, language);
                if (format.equals("bin")) {
                	createCustomLandmarkYelpList(responseBody, (List<ExtendedLandmark>)venueArray, stringLimit, hasDeals, locale);
                } else {
                    createCustomJsonYelpList(responseBody, (List<Object>)venueArray, stringLimit, hasDeals);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "VenueDetailsRetriever.run exception:", e);
            } finally {
                venueDetailsThreads.remove(offset);
            }
        }
    }

	@Override
	protected List<ExtendedLandmark> processBinaryRequest(double lat, double lng, String query, int radius, int version, int limit, int stringLimit, String hasDeals, String language, Locale locale)
			throws Exception {
		int normalizedRadius = NumberUtils.normalizeNumber(radius, 1000, 40000);
        int normalizedLimit = NumberUtils.normalizeNumber(limit, 20, 100);
        String key = getCacheKey(getClass(), "processBinaryRequest", lat, lng, query, normalizedRadius, version, normalizedLimit, stringLimit, hasDeals, language);

        List<ExtendedLandmark> landmarks = (List<ExtendedLandmark>)CacheUtil.getObject(key);
        if (landmarks == null) {
        	landmarks = Collections.synchronizedList(new ArrayList<ExtendedLandmark>());
            
        	if (!CacheUtil.containsKey(CACHE_KEY)) {
        		Map<Integer, Thread> venueDetailsThreads = new ConcurrentHashMap<Integer, Thread>();
        		boolean isDeal = Boolean.parseBoolean(hasDeals);
        		int offset = 0;

        		while (offset < normalizedLimit) {
        			Thread venueDetailsRetriever = ThreadUtil.newThread(new VenueDetailsRetriever(venueDetailsThreads, landmarks,
                        lat, lng, query, normalizedRadius, offset, isDeal, stringLimit, language, "bin", locale));
        			venueDetailsThreads.put(offset, venueDetailsRetriever);
        			venueDetailsRetriever.start();
        			offset += 20;
        		}

        		ThreadUtil.waitForLayers(venueDetailsThreads);
            
        		if (landmarks.size() > normalizedLimit) {
        			landmarks = new ArrayList<ExtendedLandmark>(landmarks.subList(0, normalizedLimit));
        		}
        	} else {
        		logger.log(Level.WARNING, "Yelp Rate Limit Exceeded");
        	}

            if (!landmarks.isEmpty()) {
                CacheUtil.put(key, landmarks);
                logger.log(Level.INFO, "Adding YP landmark list to cache with key {0}", key);
            }
        } else {
            logger.log(Level.INFO, "Reading YP landmark list from cache with key {0}", key);
        }
        return landmarks;
	}
	
	private static int createCustomLandmarkYelpList(String yelpJson, List<ExtendedLandmark> landmarks, int stringLimit, boolean hasDeals, Locale locale) throws JSONException {
        int total = 0;
        if (StringUtils.startsWith(yelpJson, "{")) {
            JSONObject jsonRoot = new JSONObject(yelpJson);
            if (jsonRoot.has("total")) {
                total = jsonRoot.getInt("total");
                if (total > 0) {
                    //System.out.println("total: " + total);
                    JSONArray businesses = jsonRoot.getJSONArray("businesses");
                    for (int i = 0; i < businesses.length(); i++) {
                        JSONObject business = businesses.getJSONObject(i);

                        JSONObject location = business.getJSONObject("location");
                        JSONObject coordinate = location.getJSONObject("coordinate");

                        String name = business.getString("name");
                        String url = business.getString("mobile_url");

                        QualifiedCoordinates qc = new QualifiedCoordinates(coordinate.getDouble("latitude"), coordinate.getDouble("longitude"), 0f, 0f, 0f);
             		   
                        AddressInfo address = new AddressInfo();
                        if (business.has("display_phone") && !business.isNull("display_phone")) {
                           	address.setField(AddressInfo.PHONE_NUMBER, business.getString("display_phone"));
                        }
                        
                        if (location.has("display_address")) {
                            JSONArray displayAddressArray = location.getJSONArray("display_address");
                            String display_address = "";
                            for (int j = 0; j < displayAddressArray.length(); j++) {
                                if (display_address.length() > 0) {
                                    display_address += ", ";
                                }
                                display_address += displayAddressArray.getString(j);
                            }
                            address.setField(AddressInfo.STREET, display_address);
                        }
                        
                        ExtendedLandmark landmark = LandmarkFactory.getLandmark(name, null, qc, Commons.YELP_LAYER, address, -1, null);
             		    landmark.setUrl(url);
             		   
                        if (business.has("rating")) {
                           landmark.setRating(business.getDouble("rating"));
                        }
                        if (business.has("review_count")) {
                        	landmark.setNumberOfReviews(business.getInt("review_count"));
                        }

                        String icon = business.optString("image_url");
                        if (StringUtils.isNotEmpty(icon)) {
                            landmark.setThumbnail(icon);
                        }
                        
                        Map<String, String> tokens = new HashMap<String, String>();
                        
                        JSONUtils.putOptValue(tokens, "description", business, "snippet_text", false, stringLimit, false);

                        //categories
                        String category = "";
                        JSONArray categories = business.optJSONArray("categories");
                        if (categories != null && categories.length() > 0) {
                        	String[] categoryCodes = new String[categories.length()];
                        	for (int j = 0; j < categories.length() ; j++) {
                        		JSONArray cat = categories.getJSONArray(j);
                        		category += cat.getString(0);
                        		categoryCodes[j] = cat.getString(1);
                        		if (j <  categories.length()-1) {
                        			category += ", ";
                        		}
                        	}
                        	if (StringUtils.isNotEmpty(category)) {
                        		tokens.put("category", category);
                        	}
                        	if (hasDeals) {
                        		String[] categoryCode = YelpCategoryMapping.findMapping(categoryCodes);
                        		landmark.setCategoryId(Integer.valueOf(categoryCode[0]).intValue());
                        		String subcat = categoryCode[1];
                        		if (StringUtils.isNotEmpty(subcat)) {
                        			landmark.setSubCategoryId(Integer.valueOf(subcat).intValue());
                        		}
                        	}
                        }
                        
                        //deals
                        JSONArray deals = business.optJSONArray("deals");
                        if (deals != null && deals.length() > 0) {
                        	for (int d = 0; d < deals.length(); d++) {
                                JSONObject deal = deals.getJSONObject(d);
                                
                                long creationDate = deal.getLong("time_start")*1000;
                                landmark.setCreationDate(creationDate);
                                tokens.put("start_date", Long.toString(creationDate));
                                long endDate = 0;
                                if (deal.has("time_end")) {
                                	endDate = deal.getLong("time_end")*1000;
                                	tokens.put("end_date", Long.toString(endDate));
                                }
                                landmark.setUrl(deal.getString("url"));
                                landmark.setName(deal.getString("title") + " Deal At " + business.getString("name"));
                                //desc.put("icon", deal.getString("image_url"));
                                String description = ""; 
                                if (deal.has("what_you_get")) {
                                	description = "<b>What You Get</b><br/>" + deal.getString("what_you_get") + "<br/>";
                                } if (deal.has("important_restrictions")) {
                                	description += "<b>Important Restrictions</b><br/>" + deal.getString("important_restrictions") + "<br/>";
                                } if (deal.has("additional_restrictions")) {
                                	description += "<b>Additional Restrictions</b><br/>" + deal.getString("additional_restrictions") + "<br/>";
                                }
                                tokens.put("description", description);
                                String currencyCode = deal.getString("currency_code");
                                JSONArray options = deal.getJSONArray("options");
                                
                                JSONObject option = options.getJSONObject(0);
                                
                                double original_price = option.getDouble("original_price") / 100d;
                                double price = option.getDouble("price") / 100d;
                                double save = (original_price - price);
                                
                                double discount = (100d - (price / original_price * 100d)) / 100d;
                                
                                Deal dealObj = new Deal(price, discount, save, null, currencyCode);
                                dealObj.setEndDate(endDate);
                                landmark.setDeal(dealObj);
                        	}     
                        }
                        
                        landmark.setDescription(JSONUtils.buildLandmarkDesc(landmark, tokens, locale));
                                              
                        landmarks.add(landmark);
                    }
                }
            } else {
            	handleError(jsonRoot);
            }
        }
        return total;
    }
	
	private static void handleError(JSONObject root) {
		JSONObject error = root.optJSONObject("error");
		if (error != null && StringUtils.equals(error.optString("id"), "EXCEEDED_REQS")) {
			CacheUtil.put(CACHE_KEY, "1");
		}
		logger.log(Level.SEVERE, "Received Yelp error response {0}", root);
	}
}

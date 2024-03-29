/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.lm.server.layers;

import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkFactory;
import com.jstakun.lm.server.config.Commons;
import com.jstakun.lm.server.config.Commons.Property;
import com.jstakun.lm.server.utils.HttpUtils;
import com.jstakun.lm.server.utils.JSONUtils;
import com.jstakun.lm.server.utils.MathUtils;
import com.jstakun.lm.server.utils.NumberUtils;
import com.jstakun.lm.server.utils.memcache.CacheUtil;
import com.openlapi.AddressInfo;
import com.openlapi.QualifiedCoordinates;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jstakun
 */
public class GeonamesUtils extends LayerHelper {

    private static final int MAXROWS = 30;

    @Override
    protected JSONObject processRequest(double lat, double lng, String query, int radius, int version, int limit, int stringLimit, String lang, String flexString2) throws Exception {
        int r = NumberUtils.normalizeNumber(radius, 1, 20);
        String key = getCacheKey(getClass(), "processRequest", lat, lng, query, r, version, limit, stringLimit, lang, flexString2);
        String output = CacheUtil.getString(key);

        if (output == null) {

            URL geonamesUrl = new URL("http://api.geonames.org/findNearbyWikipediaJSON?lat=" + lat + "&lng=" + lng + "&maxRows=" + MAXROWS + "&radius=" + r + "&username=" + Commons.getProperty(Property.GEONAMES_USERNAME) + "&lang=" + lang);

            String geonamesResponse = HttpUtils.processFileRequest(geonamesUrl);

            JSONObject json =  createCustomJSonGeonamesList(geonamesResponse, version, limit, stringLimit);

            if (json.getJSONArray("ResultSet").length() > 0) {
                CacheUtil.put(key, json.toString());
                logger.log(Level.INFO, "Adding GN landmark list to cache with key {0}", key);
            }

            return json;
        } else {
            logger.log(Level.INFO, "Reading GN landmark list from cache with key {0}", key);
            return new JSONObject(output);
        }
    }

    private static JSONObject createCustomJSonGeonamesList(String jsonGeonames, int version, int limit, int stringLimit) throws JSONException {
        ArrayList<Map<String, Object>> jsonArray = new ArrayList<Map<String, Object>>();

        JSONArray geonames = JSONUtils.getJSonArray(jsonGeonames, "geonames");
        if (geonames != null) {
            for (int i = 0; i < geonames.length(); i++) {
                try {
                    JSONObject geoname = geonames.getJSONObject(i);
                    Map<String, Object> jsonObject = new HashMap<String, Object>();
                    jsonObject.put("name", geoname.getString("title"));
                    jsonObject.put("lat", MathUtils.normalizeE6(geoname.getDouble("lat")));
                    jsonObject.put("lng", MathUtils.normalizeE6(geoname.getDouble("lng")));

                    if (version >= 2) {
                        Map<String, String> desc = new HashMap<String, String>();
                        JSONUtils.putOptValue(desc, "description", geoname, "summary", false, stringLimit, false);
                        if (version >= 3) {
                            String icon = geoname.optString("thumbnailImg");
                            if (StringUtils.isNotEmpty(icon)) {
                                desc.put("icon", icon);
                            }
                        }
                        if (!desc.isEmpty()) {
                            jsonObject.put("desc", desc);
                        }
                        jsonObject.put("url", geoname.getString("wikipediaUrl"));
                    } else {
                        jsonObject.put("desc", geoname.getString("wikipediaUrl"));
                    }

                    jsonArray.add(jsonObject);
                } catch (JSONException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                if (i > limit) {
                    break;
                }
            }
        }

        JSONObject json = new JSONObject().put("ResultSet", jsonArray);
        return json;
    }

	@Override
	protected List<ExtendedLandmark> processBinaryRequest(double lat, double lng, String query, int radius, int version, int limit, int stringLimit, String lang, String flexString2, Locale locale) throws Exception {
		int r = NumberUtils.normalizeNumber(radius, 1, 20);
        String key = getCacheKey(getClass(), "processBinaryRequest", lat, lng, query, r, version, limit, stringLimit, lang, flexString2);
        List<ExtendedLandmark> output = (List<ExtendedLandmark>)CacheUtil.getObject(key);

        if (output == null) {

            URL geonamesUrl = new URL("http://api.geonames.org/findNearbyWikipediaJSON?lat=" + lat + "&lng=" + lng + "&maxRows=" + MAXROWS + "&radius=" + r + "&username=" + Commons.getProperty(Property.GEONAMES_USERNAME) + "&lang=" + lang);

            String geonamesResponse = HttpUtils.processFileRequest(geonamesUrl);
            
            //System.out.println("Response: " + geonamesResponse);

            output =  createLandmarksGeonamesList(geonamesResponse, limit, stringLimit, locale);

            if (!output.isEmpty()) {
                CacheUtil.put(key, output);
                logger.log(Level.INFO, "Adding GN landmark list to cache with key {0}", key);
            }

        } else {
            logger.log(Level.INFO, "Reading GN landmark list from cache with key {0}", key);
        }
        return output;
	}
	
	private static List<ExtendedLandmark> createLandmarksGeonamesList(String jsonGeonames, int limit, int stringLimit, Locale locale) throws JSONException {
		List<ExtendedLandmark> landmarks = new ArrayList<ExtendedLandmark>();

        JSONArray geonames = JSONUtils.getJSonArray(jsonGeonames, "geonames");
        if (geonames != null) {
            for (int i = 0; i < geonames.length(); i++) {
                try {
                    JSONObject geoname = geonames.getJSONObject(i);
                    
                    String name = geoname.getString("title");
                    double lat = geoname.getDouble("lat");
                    double lng = geoname.getDouble("lng");
                    String url = geoname.getString("wikipediaUrl");
                    
                    QualifiedCoordinates qc = new QualifiedCoordinates(lat, lng, 0f, 0f, 0f);
                    ExtendedLandmark landmark = LandmarkFactory.getLandmark(name, null, qc, Commons.WIKIPEDIA_LAYER, new AddressInfo(), -1, null);
                    landmark.setUrl(url); 
                    
                    Map<String, String> tokens = new HashMap<String, String>();
                    JSONUtils.putOptValue(tokens, "description", geoname, "summary", false, stringLimit, false);
                    
                    String icon = geoname.optString("thumbnailImg");
                    if (StringUtils.isNotEmpty(icon)) {
                        landmark.setThumbnail(icon);
                    }
                    
                    String description = JSONUtils.buildLandmarkDesc(landmark, tokens, locale);
                    landmark.setDescription(description);
					
                    landmarks.add(landmark);
                } catch (JSONException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                if (i > limit) {
                    break;
                }
            }
        }

        return landmarks;
    }
}

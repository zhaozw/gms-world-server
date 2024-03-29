/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.lm.server.layers;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.SearchParameters;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkFactory;
import com.jstakun.lm.server.config.Commons;
import com.jstakun.lm.server.config.Commons.Property;
import com.jstakun.lm.server.utils.JSONUtils;
import com.jstakun.lm.server.utils.NumberUtils;
import com.jstakun.lm.server.utils.memcache.CacheUtil;
import com.jstakun.lm.server.utils.xml.XMLUtils;
import com.openlapi.AddressInfo;
import com.openlapi.QualifiedCoordinates;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jstakun
 */
public class FlickrUtils extends LayerHelper {

	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
    @Override
    protected JSONObject processRequest(double latitude, double longitude, String query, int radius, int version, int limit, int stringLimit, String flex, String flexString2) throws ParserConfigurationException, JSONException, UnsupportedEncodingException {
    	int r = NumberUtils.normalizeNumber(radius, 1, 32);
        String key = getCacheKey(getClass(), "processRequest", latitude, longitude, query, r, version, limit, stringLimit, flex, flexString2);
        
        String output = CacheUtil.getString(key);
        JSONObject json = null;
        if (output == null) {
            SearchParameters sp = new SearchParameters();
            sp.setLatitude(Double.toString(latitude));
            sp.setLongitude(Double.toString(longitude));
            sp.setRadius(r);
            PhotoList<Photo> photoList = getPhotoList(sp, query, limit);
            json = createCustomJsonFlickrPhotoList(photoList, version, stringLimit);
            if (!photoList.isEmpty()) {
                CacheUtil.put(key, json.toString());
                logger.log(Level.INFO, "Adding FL landmark list to cache with key {0}", key);
            }
        } else {
            logger.log(Level.INFO, "Reading FL landmark list from cache with key {0}", key);
            json = new JSONObject(output);
        }
        return json;
    }

    protected String processRequest(double latitudeMin, double latitudeMax, double longitudeMin, double longitudeMax, String query, int version, int limit, int stringLimit, String format) throws JSONException, ParserConfigurationException, UnsupportedEncodingException {
        String key = getCacheKey(FlickrUtils.class, "processRequest", (latitudeMin + latitudeMax) / 2, (longitudeMin + longitudeMax) / 2, query, 0, version, limit, stringLimit, format, null);


        String output = CacheUtil.getString(key);
        if (output == null) {
            SearchParameters sp = new SearchParameters();
            sp.setBBox(Double.toString(longitudeMin), Double.toString(latitudeMin), Double.toString(longitudeMax), Double.toString(latitudeMax));
            PhotoList<Photo> photoList = getPhotoList(sp, query, limit);
            if (format.equals("kml")) {
                output = XMLUtils.createKmlPhotoList(photoList);
            } else if (format.equals("json")) {
                output = createCustomJsonFlickrPhotoList(photoList, version, stringLimit).toString();
            } else {
                output = XMLUtils.createCustomXmlPhotoList(photoList);
            }
            if (!photoList.isEmpty()) {
                CacheUtil.put(key, output);
                logger.log(Level.INFO, "Adding FL landmark list to cache with key {0}", key);
            }
        } else {
            logger.log(Level.INFO, "Reading FL landmark list from cache with key {0}", key);
        }

        return output;
    }

    private static PhotoList<Photo> getPhotoList(SearchParameters sp, String query, int limit) throws ParserConfigurationException, JSONException {
    	PhotosInterface photosIntf = new PhotosInterface(Commons.getProperty(Property.FLICKR_APIKEY), Commons.getProperty(Property.FLICKR_sharedSecret), new REST()); // new REST("https://api.flickr.com"));
        
        //sp.setMinTakenDate(new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 365 * 5))); //5 years old max
        sp.setSort(SearchParameters.DATE_TAKEN_DESC);
        sp.setHasGeo(true);
        sp.setAccuracy(Flickr.ACCURACY_STREET);
        if (StringUtils.isNotEmpty(query)) {
            sp.setText(query);
        }

        HashSet<String> hs = new HashSet<String>();
        hs.add("geo");
        hs.add("url_sq");
        hs.add("url_t");
        hs.add("url_s");
        hs.add("url_m");
        hs.add("url_z");
        hs.add("url_l");
        hs.add("url_o");
        hs.add("date_upload");
        hs.add("date_taken");

        sp.setExtras(hs);

        PhotoList<Photo> photoList = new PhotoList<Photo>();

        try {
            photoList = photosIntf.search(sp, limit, 1);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return photoList;
    }

    private static JSONObject createCustomJsonFlickrPhotoList(PhotoList<Photo> photos, int version, int stringLimit) throws JSONException {
        List<Map<String, Object>> jsonArray = new ArrayList<Map<String, Object>>();
        
        for (Photo p : photos) {

            if (p.hasGeoData()) {
                Map<String, Object> jsonObject = new HashMap<String, Object>();

                String name = p.getTitle();
                if (name == null || name.length() == 0) {
                    name = p.getId();
                }

                jsonObject.put("name", name);
                jsonObject.put("lat", p.getGeoData().getLatitude());
                jsonObject.put("lng", p.getGeoData().getLongitude());

                String url = p.getUrl();
                url = url.replace("http://", "http://m.");
                if (version >= 2) {
                    Map<String, String> desc = new HashMap<String, String>();
                    JSONUtils.putOptValue(desc, "description", p.getDescription(), stringLimit, false);
                    if (version >= 3) {
                        //if (p.getDateTaken() != null) {
                        //    JSONUtils.putOptDate(desc, "taken_date", formatter.format(p.getDateTaken()), formatter);
                        //}
                        if (p.getDatePosted() != null) {
                            JSONUtils.putOptDate(desc, "taken_date", formatter.format(p.getDatePosted()), formatter);
                        }
                        //if (p.getDateAdded() != null) {
                        //    JSONUtils.putOptDate(desc, "upload_date", formatter.format(p.getDateAdded()), formatter);
                        //}
                    }

                    if (version >= 4) {
                       if (p.getThumbnailUrl() != null) {
                           desc.put("icon", p.getThumbnailUrl());
                       }
                    }

                    if (!desc.isEmpty()) {
                        jsonObject.put("desc", desc);
                    }
                    jsonObject.put("url", url);
                } else {
                    jsonObject.put("desc", url);
                }

                jsonArray.add(jsonObject);
            }
        }

        JSONObject json = new JSONObject().put("ResultSet", jsonArray);
        return json;
    }

	@Override
	protected List<ExtendedLandmark> processBinaryRequest(double lat, double lng, String query, int radius, int version, int limit, int stringLimit, String flex, String flexString2, Locale locale) throws Exception {
		int r = NumberUtils.normalizeNumber(radius, 1, 32);
        String key = getCacheKey(getClass(), "processBinaryRequest", lat, lng, query, r, version, limit, stringLimit, flex, flexString2);
        List<ExtendedLandmark> output = (List<ExtendedLandmark>)CacheUtil.getObject(key);
        
        if (output == null) {
            SearchParameters sp = new SearchParameters();
            sp.setLatitude(Double.toString(lat));
            sp.setLongitude(Double.toString(lng));
            sp.setRadius(r);
            PhotoList<Photo> photoList = getPhotoList(sp, query, limit);
            output = createLandmarksFlickrPhotoList(photoList,  stringLimit, locale);
            if (!output.isEmpty()) {
                CacheUtil.put(key, output);
                logger.log(Level.INFO, "Adding FL landmark list to cache with key {0}", key);
            }
        } else {
            logger.log(Level.INFO, "Reading FL landmark list from cache with key {0}", key);
        }
        return output;
	}
	
	private static List<ExtendedLandmark> createLandmarksFlickrPhotoList(PhotoList<Photo> photos, int stringLimit, Locale locale) throws JSONException {
		List<ExtendedLandmark> landmarks = new ArrayList<ExtendedLandmark>();
        
        for (Photo p : photos) {

            if (p.hasGeoData()) {
                
                String name = p.getTitle();
                if (name == null || name.length() == 0) {
                    name = p.getId();
                }

                double lat = p.getGeoData().getLatitude();
                double lng = p.getGeoData().getLongitude();

                String url = p.getUrl();
                url = url.replace("http://", "http://m.");
                Map<String, String> tokens = new HashMap<String, String>();
                JSONUtils.putOptValue(tokens, "description", p.getDescription(), stringLimit, false);
                
                //if (p.getDateTaken() != null) {
                //    JSONUtils.putOptDate(tokens, "taken_date", formatter.format(p.getDateTaken()), formatter);
                //}
                
                long creationDate = -1;
                if (p.getDatePosted() != null) {
                    //JSONUtils.putOptDate(tokens, "taken_date", formatter.format(p.getDatePosted()), formatter);
                	creationDate = p.getDatePosted().getTime();
                }
                //if (p.getDateAdded() != null) {
                //    JSONUtils.putOptDate(tokens, "upload_date", formatter.format(p.getDateAdded()), formatter);
                //}
                 
                QualifiedCoordinates qc = new QualifiedCoordinates(lat, lng, 0f, 0f, 0f);
                ExtendedLandmark landmark = LandmarkFactory.getLandmark(name, null, qc, Commons.FLICKR_LAYER, new AddressInfo(), creationDate, null);
                landmark.setUrl(url); 
                
                if (p.getThumbnailUrl() != null) {
                     landmark.setThumbnail(p.getThumbnailUrl());
                }
                
                String description = JSONUtils.buildLandmarkDesc(landmark, tokens, locale);
                landmark.setDescription(description);
				
                landmarks.add(landmark);
            }
        }

        return landmarks;
    }
}

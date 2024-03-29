package com.jstakun.lm.server.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.jstakun.lm.server.persistence.Screenshot;
import com.jstakun.lm.server.utils.memcache.CacheAction;
import com.jstakun.lm.server.utils.persistence.ScreenshotPersistenceUtils;

public class FileUtils {

	private static final Logger logger = Logger.getLogger(FileUtils.class.getName());
	/*public static BlobKey saveFile(String fileName, InputStream is) throws IOException {
		FileService fileService = FileServiceFactory.getFileService();
        AppEngineFile file = fileService.createNewBlobFile("image/jpeg", fileName);
        FileWriteChannel writeChannel = fileService.openWriteChannel(file, true);
        
        int nRead;
        byte[] data = new byte[8192];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            writeChannel.write(ByteBuffer.wrap(data, 0, nRead));
        }

        writeChannel.closeFinally();
        
        return fileService.getBlobKey(file);
	}*/	
	
	/*public static void saveFileV2(String fileName, InputStream is, double lat, double lng) throws IOException {
		String bucketName = AppIdentityServiceFactory.getAppIdentityService().getDefaultGcsBucketName();
		GcsService gcsService = GcsServiceFactory.createGcsService();
        GcsFilename filename = new GcsFilename(bucketName, fileName);
        GcsFileOptions options = new GcsFileOptions.Builder()
            .mimeType("image/jpeg")
            .acl("public-read")
            .addUserMetadata("lat", Double.toString(lat))
            .addUserMetadata("lng", Double.toString(lng))
            .build();
        GcsOutputChannel writeChannel = gcsService.createOrReplace(filename, options);
        
        int nRead;
        byte[] data = new byte[8192];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            writeChannel.write(ByteBuffer.wrap(data, 0, nRead));
        }
        
        writeChannel.close();
	}*/
	
	public static void saveFileV2(String fileName, byte[] screenshot, double lat, double lng) throws IOException {
		String bucketName = AppIdentityServiceFactory.getAppIdentityService().getDefaultGcsBucketName();
		GcsService gcsService = GcsServiceFactory.createGcsService();
        GcsFilename filename = new GcsFilename(bucketName, fileName);
        GcsFileOptions options = new GcsFileOptions.Builder()
            .mimeType("image/jpeg")
            .acl("public-read")
            .addUserMetadata("lat", Double.toString(lat))
            .addUserMetadata("lng", Double.toString(lng))
            .build();
        GcsOutputChannel writeChannel = gcsService.createOrReplace(filename, options);
        
        writeChannel.write(ByteBuffer.wrap(screenshot, 0, screenshot.length));
        
        writeChannel.close();
	}
	
	public static boolean deleteFileV2(String fileName) throws IOException {
		String bucketName = AppIdentityServiceFactory.getAppIdentityService().getDefaultGcsBucketName();
		GcsService gcsService = GcsServiceFactory.createGcsService();
        GcsFilename filename = new GcsFilename(bucketName, fileName);
        return gcsService.delete(filename);
	}
	
	private static String getImageUrl(BlobKey blobKey, boolean thumbnail) {
		//This URL is served by a high-performance dynamic image serving infrastructure that is available globally. 
		//The URL returned by this method is always public, but not guessable; private URLs are not currently supported. 
		//If you wish to stop serving the URL, delete the underlying blob key. This takes up to 24 hours to take effect. 
		//The URL format also allows dynamic resizing and crop with certain restrictions. 
		//To get dynamic resizing and cropping simply append options to the end of the url obtained 
		//via this call. Here is an example: getServingUrl -> "http://lh3.ggpht.com/SomeCharactersGoesHere"
        //To get a 32 pixel sized version (aspect-ratio preserved) simply append "=s32" to the url: "http://lh3.ggpht.com/SomeCharactersGoesHere=s32"
        //To get a 32 pixel cropped version simply append "=s32-c": "http://lh3.ggpht.com/SomeCharactersGoesHere=s32-c"
        //Valid sizes are any integer in the range [0, 1600] (maximum is available as SERVING_SIZES_LIMIT).

        ImagesService imagesService = ImagesServiceFactory.getImagesService();
        ServingUrlOptions sou = ServingUrlOptions.Builder.withBlobKey(blobKey);
        String imageUrl = imagesService.getServingUrl(sou);
        if (thumbnail) {
        	imageUrl += "=s128";
        }
        return imageUrl;
	}
	
	//"http://storage.googleapis.com/" + bucketName + "/" + fileName;
	private static String getImageUrlV2(String fileName, boolean thumbnail) {
		String bucketName = AppIdentityServiceFactory.getAppIdentityService().getDefaultGcsBucketName();
		BlobKey bk = getCloudStorageBlobKey(bucketName, fileName);
		return getImageUrl(bk, thumbnail);
	}
	
	private static BlobKey getCloudStorageBlobKey(String bucket_name, String object_name)
	{       
	    String cloudStorageURL = "/gs/" + bucket_name + "/" + object_name;
	    BlobstoreService bs = BlobstoreServiceFactory.getBlobstoreService();
	    BlobKey bk = bs.createGsBlobKey(cloudStorageURL);
	    return bk;
	} 
	
	public static Screenshot getScreenshot(final String key, boolean thumbnail) {
		Screenshot s = null;
    	if (StringUtils.isNotEmpty(key)) {
            CacheAction screenshotCacheAction = new CacheAction(new CacheAction.CacheActionExecutor() {			
				@Override
				public Object executeAction() {
					return ScreenshotPersistenceUtils.selectScreenshot(key);
				}
			});
        	s = (Screenshot) screenshotCacheAction.getObjectFromCache(key);
        	if (s != null) {
        		try {
                	s.setUrl(FileUtils.getImageUrlV2(s.getFilename(), thumbnail));
                } catch (Exception e) {
                	logger.log(Level.SEVERE, "FileUtils.getScreenshot() exception", e);
                }
        	}	
		} 	
    	return s;
	}
}

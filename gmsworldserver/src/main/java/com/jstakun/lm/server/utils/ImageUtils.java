package com.jstakun.lm.server.utils;

import java.awt.image.BufferedImage;

public class ImageUtils {

	private static final double BLACK_FACTOR = 0.75;
	
	public static boolean isBlackImage(BufferedImage image) {
		boolean isBlack = false;
		int blackPixelsCount = 0;
		int w = image.getWidth();
	    int h = image.getHeight();
	    int totalPixels = w * h;
	    
	    for (int i = 0; i < h; i++) {
	    	for (int j = 0; j < w; j++) {
	    		int pixel = image.getRGB(j, i);
	    		if (((pixel & 0x00FFFFFF) == 0)) {
	    			blackPixelsCount++;
	    			if ((blackPixelsCount / totalPixels) > BLACK_FACTOR) {
	    				isBlack = true;
	    				break;
	    			}
	    		}
	    	}
	    }  
		
	    return isBlack;
	}
	
	public static boolean isBlackImage(byte[] imageData) {
		boolean isBlack = false;
		
		int blackPixelsCount = 0;
		int totalPixels = imageData.length / 3;	    
	    
		for (int i = 0; i < imageData.length / 3; i++) {
		    int pixel = 0xFF000000 | 
		        ((imageData[3 * i + 0] & 0xFF) << 16) |
		        ((imageData[3 * i + 1] & 0xFF) << 8) |
		        ((imageData[3 * i + 2] & 0xFF));
		    
		    if ((pixel & 0x00FFFFFF) == 0) {
    			blackPixelsCount++;
    			if ((blackPixelsCount / totalPixels) > BLACK_FACTOR) {
    				isBlack = true;
    				break;
    			}
    		}
		}
		
		return isBlack;
	}
}

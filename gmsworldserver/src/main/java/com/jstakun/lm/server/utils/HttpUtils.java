/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.lm.server.utils;

import com.google.gdata.util.common.util.Base64;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class HttpUtils {

    private static final Logger logger = Logger.getLogger(HttpUtils.class.getName());

    public static String processFileRequestWithLocale(URL fileUrl, String locale) throws IOException {
        return processFileRequest(fileUrl, false, null, null, "GET", locale, null, null);
    }

    public static String processFileRequestWithAuthn(URL fileUrl, String authn) throws IOException {
        return processFileRequest(fileUrl, true, null, authn, "GET", null, null, null);
    }

    public static String processFileRequest(URL fileUrl) throws IOException {
        return processFileRequest(fileUrl, false, null, null, "GET", null, null, null);
    }

    public static String processFileRequest(URL fileUrl, String method, String accept, String urlParams) throws IOException {
        return processFileRequest(fileUrl, false, null, null, method, null, accept, urlParams);
    }

    private static String processFileRequest(URL fileUrl, boolean authn, String userpassword, String authnOther, String method, String locale, String accept, String urlParams) throws IOException {
        InputStream is = null;
        String file = null;

        try {
            HttpURLConnection conn = (HttpURLConnection) fileUrl.openConnection();
            conn.setRequestMethod(method);

            if (authn && userpassword != null) {
                //username : password
                String encodedAuthorization = Base64.encode(userpassword.getBytes());
                conn.setRequestProperty("Authorization", "Basic " + encodedAuthorization);
            } else if (authn && authnOther != null) {
                conn.setRequestProperty("Authorization", authnOther);
            }

            if (StringUtils.isNotEmpty(locale)) {
                conn.setRequestProperty("Accept-Language", locale);
            }

            conn.setRequestProperty("Accept-Charset", "utf-8");

            if (StringUtils.isNotEmpty(accept)) {
                conn.setRequestProperty("Accept", accept);
            }

            if (urlParams != null) {
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length", Integer.toString(urlParams.getBytes().length));
                conn.setRequestProperty("Content-Language", "en-US");
                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                //Send request
                IOUtils.write(urlParams, conn.getOutputStream());
            } else {
                conn.connect();
            }
            //int length = conn.getContentLength();
            if (conn != null) {
                int responseCode = conn.getResponseCode();

                if (responseCode == HttpServletResponse.SC_OK) {
                    is = conn.getInputStream();
                   
                    //byte[] buf = new byte[1024];
                    //int count = 0;
                    //while ((count = is.read(buf)) >= 0) {
                    //    file.append(new String(buf, 0, count));
                    //}
                } else {
                    is = conn.getErrorStream();
                    
                }
                file = IOUtils.toString(is, "UTF-8");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            if (is != null) {
                is.close();
            }
        }

        return file;
    }

    public static void processImageFileRequest(OutputStream out, HttpServletResponse response, URL fileUrl) throws IOException {

        InputStream is = null;

        try {

            HttpURLConnection conn = (HttpURLConnection) fileUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            //int length = conn.getContentLength();
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpServletResponse.SC_OK) {
                is = conn.getInputStream();

                response.setHeader("Content-Encoding", "gzip");
                response.setHeader("Vary", "Accept-Encoding");
                response.setContentType("image/png");
                //response.setContentLength(length);

                byte[] buf = new byte[1024];
                int count = 0;
                while ((count = is.read(buf)) >= 0) {
                    out.write(buf, 0, count);
                }
            } else {
                response.sendError(responseCode);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public static boolean isEmptyAny(HttpServletRequest request, String... params) {
        for (String p : params) {
            if (StringUtils.isEmpty(request.getParameter(p))) {
                logger.log(Level.INFO, "Missing required parameter {0}", p);
                return true;
            }
        }
        return false;
    }
    
    public static boolean isEmptyAnyDebug(HttpServletRequest request, String... params) {
        boolean isMissing = false;
    	for (String p : params) {
    		String value = request.getParameter(p);
            if (StringUtils.isEmpty(value)) {
                logger.log(Level.INFO, "Missing required parameter {0}", p);
                isMissing = true;
            } else {
            	logger.log(Level.INFO, "Found parameter {0} : {1}", new Object[] {p, value});
            }
        }
        return isMissing;
    }
}
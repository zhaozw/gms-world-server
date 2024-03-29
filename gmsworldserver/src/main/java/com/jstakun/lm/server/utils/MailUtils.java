/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.lm.server.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

import com.jstakun.lm.server.config.ConfigurationManager;

/**
 *
 * @author jstakun
 */
public class MailUtils {

    private static final Logger logger = Logger.getLogger(MailUtils.class.getName());

    private static void sendMail(String fromA, String fromP, String toA, String toP, String subject, String content, String contentType) {
        try {
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(fromA, fromP));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(toA, toP));
            msg.setSubject(subject);
            msg.setContent(content, contentType);
            Transport.send(msg);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    public static void sendEmailingMessage(String toA, String nick, String message) {
        sendMail(ConfigurationManager.SUPPORT_MAIL, ConfigurationManager.ADMIN_NICK, toA, nick, "Message from Landmark Manager", message, "text/html");
    }

    public static void sendLandmarkCreationNotification(String title, String body) {
        sendMail(ConfigurationManager.SUPPORT_MAIL, ConfigurationManager.ADMIN_NICK, ConfigurationManager.ADMIN_MAIL, ConfigurationManager.ADMIN_NICK, title, body, "text/plain");
    }

    public static void sendList(String title, Map<String, Collection<String>> stringMap, Map<String, Integer> recentlyCreated) {
        String message = "";
        for (Map.Entry<String, Collection<String>> entry : stringMap.entrySet()) {
            message += "Report for " + entry.getKey() + "<br/><br/>";
            Collection<String> stringList = entry.getValue();
            for (String s : stringList) {
                message += s + "<br/>";
            }
            if (!stringList.isEmpty()) {
                message += "<br/>";
            }
            message += "Found " + stringList.size() + " record(s).";
            message += "<br/>Number of users last week: " + recentlyCreated.get(entry.getKey()) + ".<br/><br/>";
        }
        //System.out.println(message);
        sendMail(ConfigurationManager.SUPPORT_MAIL, ConfigurationManager.ADMIN_NICK, ConfigurationManager.ADMIN_MAIL, ConfigurationManager.ADMIN_NICK, title, message, "text/html");
    }

    public static void sendVerificationRequest(String toA, String nick, String key, ServletContext context) {
        InputStream is = null;
        try {
            String link = ConfigurationManager.SERVER_URL + "verify.do?k=" + URLEncoder.encode(key, "UTF-8") + "&s=1";
            is = context.getResourceAsStream("/WEB-INF/emails/verification.html");
            String message = String.format(IOUtils.toString(is), nick, link);
            sendMail(ConfigurationManager.SUPPORT_MAIL, ConfigurationManager.ADMIN_NICK, toA, nick, "Welcome to GMS World", message, "text/html");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    public static void sendRegistrationNotification(String toA, String nick, ServletContext context) {
        InputStream is = null;
        try {
            is = context.getResourceAsStream("/WEB-INF/emails/notification.html");
            String message = String.format(IOUtils.toString(is), nick);
            sendMail(ConfigurationManager.SUPPORT_MAIL, ConfigurationManager.ADMIN_NICK, toA, nick, "GMS World Registration", message, "text/html");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public static void sendLoginNotification(String toA, String nick, String layer, ServletContext context) {
        InputStream is = null;
        try {
            is = context.getResourceAsStream("/WEB-INF/emails/login.html");
            String message = String.format(IOUtils.toString(is), nick, layer);
            sendMail(ConfigurationManager.SUPPORT_MAIL, ConfigurationManager.ADMIN_NICK, toA, nick, "GMS World Login", message, "text/html");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    public static void sendLandmarkNotification(String toA, String userUrl, String nick, String landmarkUrl, String key, ServletContext context) {
        InputStream is = null;
        try {
            is = context.getResourceAsStream("/WEB-INF/emails/landmark.html");
            String message = String.format(IOUtils.toString(is), userUrl, nick, landmarkUrl, key);
            sendMail(ConfigurationManager.SUPPORT_MAIL, ConfigurationManager.ADMIN_NICK, toA, nick, "Message from GMS World", message, "text/html");
            //remove after tests
            //sendMail(SUPPORT_MAIL, ADMIN_NICK, ADMIN_MAIL, ADMIN_NICK, "Copy of message to " + toA, message, "text/html");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    public static void sendUserCreationNotification(String body) {
        sendMail(ConfigurationManager.SUPPORT_MAIL, ConfigurationManager.ADMIN_NICK, ConfigurationManager.ADMIN_MAIL, ConfigurationManager.ADMIN_NICK, "New user", body, "text/plain");
    }
    
    public static void sendBlackScreenshotNotification(String body) {
        sendMail(ConfigurationManager.SUPPORT_MAIL, ConfigurationManager.ADMIN_NICK, ConfigurationManager.ADMIN_MAIL, ConfigurationManager.ADMIN_NICK, "Black screenshot", body, "text/plain");
    }

    public static void sendCrashReport(String title, String body) {
        sendMail(ConfigurationManager.SUPPORT_MAIL, ConfigurationManager.ADMIN_NICK, ConfigurationManager.SUPPORT_MAIL, ConfigurationManager.ADMIN_NICK, title, body, "text/plain");
    }

    public static void sendContactMessage(String fromA, String nick, String subject, String body) {
        sendMail(ConfigurationManager.SUPPORT_MAIL, ConfigurationManager.ADMIN_NICK, ConfigurationManager.ADMIN_MAIL, ConfigurationManager.ADMIN_NICK, subject, "Message from: " + nick + " " + fromA + "\n" + body, "text/plain");
    }

    public static boolean isValidEmailAddress(String aEmailAddress) {
        return EmailValidator.getInstance().isValid(aEmailAddress);
    }

    public static void sendSearchQueryNotification(String query, boolean isDeal, int counter, String uri) {
        String url = null;
        if (uri.startsWith("/services")) {
            url = ConfigurationManager.SSL_SERVER_URL + uri.substring(1);
        } else {
            url = ConfigurationManager.SERVER_URL + uri.substring(1);
        }
        String message = "New search query has been executed: " + query + "\nDeals: "
                + isDeal + "\nResponse contains " + counter + " landmarks.\nCheck it out: " + url;
        sendMail(ConfigurationManager.SUPPORT_MAIL, ConfigurationManager.ADMIN_NICK, ConfigurationManager.ADMIN_MAIL, ConfigurationManager.ADMIN_NICK, "New search with " + counter + " results", message, "text/plain");
    }
    
    public static void sendEngagementMessage(String toA, ServletContext context) {
        InputStream is = null;
        try {
            is = context.getResourceAsStream("/WEB-INF/emails/engage.html");
            String message = IOUtils.toString(is);
            
            String excluded = ConfigurationManager.getParam(ConfigurationManager.EXCLUDED, "");
            String[] excludedList = StringUtils.split(excluded, "|");
            if (StringUtils.indexOfAny(toA, excludedList) >= 0) {
               //sendMail(ConfigurationManager.SUPPORT_MAIL, ConfigurationManager.ADMIN_NICK, ConfigurationManager.ADMIN_MAIL, ConfigurationManager.ADMIN_NICK, "Copy of excluded engagement message to " + toA, message, "text/html");
               logger.log(Level.INFO, "Skipped sending engagement message to " + toA);
            } else {
               sendMail(ConfigurationManager.SUPPORT_MAIL, ConfigurationManager.ADMIN_NICK, toA, "Landmark Manager User", "Message from Landmark Manager", message, "text/html");
               //TODO remove after tests
               sendMail(ConfigurationManager.SUPPORT_MAIL, ConfigurationManager.ADMIN_NICK, ConfigurationManager.ADMIN_MAIL, ConfigurationManager.ADMIN_NICK, "Copy of engagement message to " + toA, message, "text/html");
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}

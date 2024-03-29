/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.lm.server.servlet;

import com.jstakun.lm.server.config.ConfigurationManager;
import com.jstakun.lm.server.utils.HttpUtils;
import com.jstakun.lm.server.utils.MailUtils;
import com.jstakun.lm.server.utils.NumberUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

/**
 *
 * @author jstakun
 */
public class NotificationsServlet extends HttpServlet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(NotificationsServlet.class.getName());
    private static final long ONE_DAY = 1000 * 60 * 60 * 24;
	
    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            if (HttpUtils.isEmptyAny(request, "type", "appId")) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                String type = request.getParameter("type");
                String appId = request.getParameter("appId");
                JSONObject reply = new JSONObject();

                if (StringUtils.equals(type, "v")) {
                    //check for version
                    reply.put("type", type);
                    if (StringUtils.equalsIgnoreCase(appId,"0")) {
                        //LM
                        String version = ConfigurationManager.getParam(ConfigurationManager.LM_VERSION, "0");
                        reply.put("value", version);
                    } else if (StringUtils.equalsIgnoreCase(appId,"1")) {
                        //DA
                        String version = ConfigurationManager.getParam(ConfigurationManager.DA_VERSION, "0");
                        reply.put("value", version);
                    }
                } else if (StringUtils.equals(type, "u")) {
                	String email = request.getParameter("e");
                	long lastStartupTime = NumberUtils.getLong(request.getParameter("lst"), -1);
                	String useCount = request.getParameter("uc");
                	Calendar cal = Calendar.getInstance();
                	cal.setTimeInMillis(lastStartupTime);
                	logger.log(Level.INFO, "Received usage notification from " + (email != null ? email : "guest") + 
                			" last startup time: " + DateFormat.getDateTimeInstance().format(cal.getTime()) + 
                			", use count: " + useCount);
                	int interval = NumberUtils.getInt(ConfigurationManager.getParam(ConfigurationManager.NOTIFICATIONS_INTERVAL, "14"), 14);
                	if (System.currentTimeMillis() - lastStartupTime > (interval * ONE_DAY) && email != null) {
                		//send email notification if lastStartupTime > week ago 
                    	//send not more that once a week
                		logger.log(Level.WARNING, email + " should be engaged to run Landmark Manager!");
                		MailUtils.sendEngagementMessage(email, getServletContext());
                		reply = new JSONObject().put("status", "engaged"); 
                	} else {
                		response.setStatus(HttpServletResponse.SC_ACCEPTED);
                		reply = new JSONObject().put("status", "accepted"); 
                	}	
                }
                out.print(reply.toString());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            out.close();
        }
    } 

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    } 

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Notifications servlet";
    }// </editor-fold>

}

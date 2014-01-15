/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.lm.server.servlet;

import com.jstakun.lm.server.layers.FlickrUtils;
import com.jstakun.lm.server.layers.LayerHelperFactory;
import com.jstakun.lm.server.utils.GeocodeUtils;
import com.jstakun.lm.server.utils.HttpUtils;
import com.jstakun.lm.server.utils.NumberUtils;
import com.jstakun.lm.server.utils.StringUtil;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author jstakun
 */
public class FlickrProviderServlet extends HttpServlet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(FlickrProviderServlet.class.getName());
    
    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String format = StringUtil.getStringParam(request.getParameter("format"), "xml");
        if (format.equals("json")) {
            response.setContentType("text/json;charset=UTF-8");
        } else if (format.equals("xml")) {
            response.setContentType("text/xml;charset=UTF-8");
        }
        PrintWriter out = response.getWriter();
        try {
            if (HttpUtils.isEmptyAny(request,"latitudeMin","longitudeMin")) { //,"latitudeMax","longitudeMax")) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                double latitudeMin = GeocodeUtils.getLatitude(request.getParameter("latitudeMin"));
                double longitudeMin = GeocodeUtils.getLongitude(request.getParameter("longitudeMin"));
                double latitudeMax = GeocodeUtils.getLatitude(request.getParameter("latitudeMax"));
                double longitudeMax = GeocodeUtils.getLongitude(request.getParameter("longitudeMax"));
                int version = NumberUtils.getVersion(request.getParameter("version"), 1);
                int limit = NumberUtils.getInt(request.getParameter("limit"), 30);
                int stringLimit = StringUtil.getStringLengthLimit(request.getParameter("display"));
                int radius = NumberUtils.getRadius(request.getParameter("radius"), 3, 6371);

                String resp = null;
                if (version > 4) {
                    resp = LayerHelperFactory.getFlickrUtils().processRequest(latitudeMin, longitudeMin, null, radius, version, limit, stringLimit, null, null).toString();
                } else {
                   resp = FlickrUtils.processRequest(latitudeMin, latitudeMax, longitudeMin, longitudeMax, null, version, limit, stringLimit, format);
                }

                out.print(resp);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            //response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        } finally {
            if (out != null) {
                out.close();
            }
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
        return "Flickr Provider Servlet";
    }// </editor-fold>
}
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.lm.server.oauth;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jstakun.lm.server.config.Commons;

/**
 *
 * @author jstakun
 */
public class LnLoginServlet extends HttpServlet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
        	String userPass = request.getParameter(Commons.OAUTH_USERNAME);  
            if (userPass != null) {
                String[] unPw = CommonUtils.userPass(userPass);
                if (unPw != null) {
                    request.getSession().setAttribute("token", unPw[0]);
                    request.getSession().setAttribute("password", unPw[1]);
                }
            }  
            //request.getSession().setAttribute("lntoken", requestToken.getToken());
            //request.getSession().setAttribute("lnsecret", requestToken.getSecret());
        
            /*OAuthService service = new ServiceBuilder().
            		provider(LnCommons.API).
            		apiKey(Commons.LN_API_KEY).
            		apiSecret(Commons.LN_API_SECRET).
            		callback(LnCommons.CALLBACK_URI).
            		build();
                    
            Token requestToken = service.getRequestToken();

            response.sendRedirect(service.getAuthorizationUrl(requestToken));*/
            response.sendRedirect(LnCommons.getAuthorizationUrl());
        } catch (Exception ex) {
            Logger.getLogger(LnLoginServlet.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
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
        return "Short description";
    }// </editor-fold>
}
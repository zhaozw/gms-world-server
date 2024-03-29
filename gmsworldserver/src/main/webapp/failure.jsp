<%-- 
    Document   : failure
    Created on : 2010-12-18, 15:02:48
    Author     : jstakun
--%>

<%@page contentType="text/html" pageEncoding="utf-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<!-- content-outer -->
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

    <head>
        <title>Landmark Manager Action Failure</title>
        <%@ include file="/WEB-INF/jspf/head.jspf" %>
    </head>

    <body>

        <jsp:include page="/WEB-INF/jspf/header.jsp"/>
       

        <div id="content-wrap" class="clear" >

            <!-- content -->
            <div id="content">

                <!-- main -->
                <div id="main">
<%
    String step = request.getParameter("step");
    if (step == null || step.length() != 1) {
       step = "0";
    }

    if (step.equals("1")) {
 %>
                    <h3>Account Registration Failed</h3>
                    <p><a href="register.jsp">Please try again</a></p>
 <%
    } else if (step.equals("2")) {
 %>
                    <h3>Account Verification Failed</h3>
                    <p>Please try again or contact <a href="mailto:support@gms-world.net?subject=Account verification failed">System Administrator</a></p>
 <%
    } else if (step.equals("3")) {
 %>
                    <h3>Account Unregistration Failed</h3>
                    <p>Please try again or contact <a href="mailto:support@gms-world.net?subject=Account unregistration failed">System Administrator</a></p>
 <%
    } else {
 %>
                    <h3>Account Action Error</h3>
                    <p>Oops! Something went wrong. Please try again.</p>
 <%
    }
 %>
                    <!-- /main -->
                </div>
                <%@ include file="/WEB-INF/jspf/sidebar.jsp" %>
                <!-- content -->
            </div>
            <!-- /content-out -->
        </div>

       <jsp:include page="/WEB-INF/jspf/footer.jsp" />

    </body>
</html>

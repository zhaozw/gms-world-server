<%@page contentType="text/html" pageEncoding="utf-8"%>
<%@page import="com.jstakun.lm.server.persistence.Landmark,
        com.jstakun.lm.server.utils.persistence.LayerPersistenceUtils,
        com.jstakun.lm.server.persistence.Comment,
        com.jstakun.lm.server.utils.UrlUtils,
        com.jstakun.lm.server.utils.DateUtils,
        com.jstakun.lm.server.utils.StringUtil,
        java.util.List,
        java.util.Date,
        com.google.appengine.api.datastore.KeyFactory" %>
<!DOCTYPE html>
<html>

<head>
  <meta charset="utf-8" />  
  <title>GMS World - Landmark Details</title>
  <meta name="viewport" content="width=device-width; initial-scale=1.0; maximum-scale=1.0" /> 
  <link rel="stylesheet" media="all" href="/style.css" type="text/css">
  <%@ include file="/WEB-INF/jspf/head_small.jspf" %>
</head>

<body>
 <div class="wrap">
    <%@ include file="/WEB-INF/jspf/header_mobile.jspf" %>
       
    <div class="content">
    	<article>
    	
    	<% 
    		Landmark landmark = (Landmark) request.getAttribute("landmark");
        	String key = (String) request.getAttribute("key");
    	
    		if (landmark == null && key == null) {
        %>
        	<h3>No landmark selected.</h3>            
        <% } else if (landmark == null) {
        %>
            <h3>This landmark has been archived and is currently unavailable.</h3>
        <% } else {
        %>
                        
            <h3><%= landmark.getName()%></h3>
            <h4><%= landmark.getDescription()%></h4>

            <a href="/showLandmark/<%= key %>/fullScreen">
                <img src="http://maps.google.com/maps/api/staticmap?center=<%= landmark.getLatitude() %>,<%= landmark.getLongitude() %>&zoom=9&size=146x146&sensor=false&markers=icon:http://gms-world.appspot.com/images/flagblue.png|<%= landmark.getLatitude() %>,<%= landmark.getLongitude() %>" alt="Landmark on Google Map" height="146" width="146"/><br/>
            </a>

            <p>
                <a href="/showLandmark/<%= key %>/fullScreen">See full screen map</a><br/>
                <%= request.getAttribute("address")!=null ? "Geocode address: "+request.getAttribute("address") : "" %><br/>
                Latitude: <%= StringUtil.formatCoordE6(landmark.getLatitude()) %>, Longitude: <%= StringUtil.formatCoordE6(landmark.getLongitude()) %><br/>
                Created in layer <a href="/showLayer/<%= landmark.getLayer() %>"><%= LayerPersistenceUtils.getLayerFormattedName(landmark.getLayer()) %></a>
                 <div class="date"><span>Posted on <%= DateUtils.getFormattedDateTime(request.getLocale(), landmark.getCreationDate()) %> | by <a href="/showUser/<%= landmark.getUsername() %>"><%= UrlUtils.createUsernameMask(landmark.getUsername()) %></a></span></div>
            </p>

            <h4>Check-ins</h4>
            <p>
                Number of check-ins: <b><%= request.getAttribute("checkinsCount")==null ? "0" : request.getAttribute("checkinsCount") %></b> <br/>
                <%
                  if (request.getAttribute("checkinsCount") != null)
                  {
                     String lastCheckinUsername = (String)request.getAttribute("lastCheckinUsername");
                %>
                     <div class="date"><span>Last check-in <%= DateUtils.getFormattedDateTime(request.getLocale(), (Date)request.getAttribute("lastCheckinDate")) %> | by <a href="<%= response.encodeURL("/showUser/" + lastCheckinUsername) %>"><%= UrlUtils.createUsernameMask(lastCheckinUsername) %></a></span></div>
                <%
                  }
                  List<Comment> commentList = (List<Comment>) request.getAttribute("comments");
                               
                  if (commentList != null)
                  {              
                %>
            </p>
            <h4><%= commentList.size() %> comments</h4>
              <div>
                <ul class="vertical comments">
				<%
                   		for (Comment comment : commentList)
                   		{
				%>
                   <li>
                     <p><%= comment.getMessage() %></p>                       
                     <div class="date"><span><%= DateUtils.getFormattedDateTime(request.getLocale(), comment.getCreationDate()) %> | by <a href="<%= response.encodeURL("/showUser/" + comment.getUsername()) %>"><%= UrlUtils.createUsernameMask(comment.getUsername()) %></a></span></div>
                   </li>
                <%
                  		}
                %>
                </ul>
              </div>
                <%
                  }
             }
                %>
 
    	</article>
    	
    	<%@ include file="/WEB-INF/jspf/ad_small_baner.jspf" %>
    </div>
    <%@ include file="/WEB-INF/jspf/footer_mobile.jspf" %>
  </div>
 
  <script src="/js/jquery.min.js"></script>
  <script type="text/javascript">
    window.addEventListener("load",function() {
	  // Set a timeout...
	  setTimeout(function(){
	    // Hide the address bar!
	    window.scrollTo(0, 1);
	  }, 0);
	});
    $('.search-box,.menu' ).hide();   
    $('.options li:first-child').click(function(){	
   		$(this).toggleClass('active'); 	
   		$('.search-box').toggle();        			
   		$('.menu').hide();  		
   		$('.options li:last-child').removeClass('active'); 
    });
    $('.options li:last-child').click(function(){
   		$(this).toggleClass('active');      			
   		$('.menu').toggle();  		
   		$('.search-box').hide(); 
   		$('.options li:first-child').removeClass('active'); 		
    });
    $('.content').click(function(){
   		$('.search-box,.menu' ).hide();   
   		$('.options li:last-child, .options li:first-child').removeClass('active');
    });
  </script>
</body>

</html>

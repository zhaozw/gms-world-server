<%-- any content can be specified here e.g.: --%>
<%@ page pageEncoding="utf-8" %>
<%@ page import="com.jstakun.lm.server.utils.persistence.LandmarkPersistenceUtils,
                 com.jstakun.lm.server.persistence.Landmark,
                 com.jstakun.lm.server.utils.DateUtils,
                 com.jstakun.lm.server.utils.UrlUtils,
                 java.util.List"%>
<div id="footer-outer" class="clear"><div id="footer-wrap">

        <div class="col-a">

            <h3>Contact Info</h3>

            <p><strong>Address: </strong>Warsaw, Poland</p>
            <p><strong>E-mail: </strong><a href="mailto:support@gms-world.net">support@gms-world.net</a></p>
            <p>Want more info - go to our <a href="/contact.jsp">contact page</a></p>

            <h3>Updates</h3>

            <ul class="subscribe-stuff">
                <li><a title="Blog" href="http://blog.gms-world.net" rel="nofollow">
                        <img alt="Blog" title="Blog" src="/images/blogger.png" /></a>
                </li>
                <li><a title="Facebook" href="http://www.facebook.com/pages/GMS-World/165436696841663" rel="nofollow">
                        <img alt="Facebook" title="Facebook" src="/images/social_facebook.png" /></a>
                </li>
                <li><a title="Twitter" href="http://twitter.com/geolocationms" rel="nofollow">
                        <img alt="Twitter" title="Twitter" src="/images/social_twitter.png" /></a>
                </li>
                <li><a title="Google+" href="https://plus.google.com/117623384724994541747" rel="nofollow">
                        <img alt="Google+" title="Google+" src="/images/google_plus.png" /></a>
                </li>
            </ul>

            <p>Stay up to date. Subscribe via
                <a href="http://blog.gms-world.net">Blog</a>,
                <a href="http://www.facebook.com/pages/GeoLocation-Mobile-Solutions/165436696841663">Facebook</a>,
                <a href="http://twitter.com/geolocationms">Twitter</a> or 
                <a href="https://plus.google.com/117623384724994541747" rel="publisher">Google+</a>
            </p>

        </div>

        <div class="col-a">

            <h3>Site Links</h3>

            <div class="footer-list">
                <ul>
                    <li><a href="/index.jsp">Home</a></li>
                    <li><a href="/download.jsp">Download</a></li>
                    <li><a href="/demo/run.jsp">Online Demo</a></li>
                    <li><a href="http://blog.gms-world.net">Blog</a></li>
                    <li><a href="/archive.do">Archives</a></li>
                    <li><a href="/about.jsp">About</a></li>
                    <li><a href="/register.jsp">Register</a></li>
                    <li><a href="/privacy.jsp">Privacy policy</a></li>
                </ul>
            </div>


        </div>

        <div class="col-a">

            <h3>Newest Landmarks</h3>

            <div class="recent-comments">
                <ul>
<%
   List<Landmark> landmarkList1 = (List<Landmark>)request.getAttribute("newestLandmarkList");

   if (landmarkList1 == null) {
	   landmarkList1 = LandmarkPersistenceUtils.selectNewestLandmarks();
   }
   
   if (landmarkList1 != null) {
   
   		for (Landmark landmark : landmarkList1) {
 %>
 <li><a href="<%= response.encodeURL("/showLandmark/" + landmark.getId()) %>" title="<%= landmark.getName() %>"><%= landmark.getName() %></a><br/> &#45; <cite><a href="<%= response.encodeURL("/showUser/" + landmark.getUsername()) %>"><%= UrlUtils.createUsernameMask(landmark.getUsername()) %></a></cite></li>
 <%
   		}
   
   }
 %>
                </ul>
            </div>

        </div>

        <div class="col-b">

            <h3>Archives</h3>

            <div class="footer-list">
                <ul>
<%
   for (int i=0;i<12;i++)
   {
%>
                    <li><a href="/archive.do?month=<%= DateUtils.getShortMonthYearString(i) %>"><%= DateUtils.getLongMonthYearString(i) %></a></li>
<%
   }
%>        
                </ul>
            </div>

        </div>

        <!-- /footer-outer -->
    </div></div>

<!-- footer-bottom -->
<div id="footer-bottom">

    <p class="bottom-left">
        &copy; 2010-14 <strong>GMS World</strong>&nbsp; &nbsp; &nbsp;
        <a href="http://www.bluewebtemplates.com/" title="Website Templates">website templates</a> by <a href="http://www.styleshout.com/">styleshout</a>
    </p>

    <p class="bottom-right">
        <a href="http://jigsaw.w3.org/css-validator/check/referer">CSS</a> |
        <a href="http://validator.w3.org/check/referer">XHTML</a>	|
        <a href="/index.jsp">Home</a> |
        <strong><a href="#top">Back to Top</a></strong>
    </p>
    <a name="bottom"></a>
    <!-- /footer-bottom-->
</div>




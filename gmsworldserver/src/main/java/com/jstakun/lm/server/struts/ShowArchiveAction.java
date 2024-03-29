/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.lm.server.struts;

import com.jstakun.lm.server.persistence.Landmark;
import com.jstakun.lm.server.utils.DateUtils;
import com.jstakun.lm.server.utils.NumberUtils;
import com.jstakun.lm.server.utils.persistence.LandmarkPersistenceUtils;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 *
 * @author jstakun
 */
public class ShowArchiveAction extends org.apache.struts.action.Action {

    private static final int INTERVAL = 10;
    private static final DateFormat df = new SimpleDateFormat("MM-yyyy", Locale.getDefault());
    
    /**
     * This is the action called from the Struts framework.
     * @param mapping The ActionMapping used to select this instance.
     * @param form The optional ActionForm bean for this request.
     * @param request The HTTP Request we are processing.
     * @param response The HTTP Response we are processing.
     * @throws java.lang.Exception
     * @return
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        String month = request.getParameter("month");
        int first = NumberUtils.getInt(request.getParameter("first"), 0);
        int next = -1;
        int prev = -1;

        String m = null;
        try {
            m = DateUtils.getLongMonthYearString(df.parse(month));
        } catch (Exception ex) {
            Date now = new Date();
            month = df.format(now);
            m = DateUtils.getLongMonthYearString(now);
        }

        int count = LandmarkPersistenceUtils.countLandmarksByMonth(month);

        if (count - first - INTERVAL > 0) {
            next = first + INTERVAL;
        }
        if (first - INTERVAL >= 0) {
            prev = first - INTERVAL;
        }

        request.setAttribute("next", new Integer(next));
        request.setAttribute("prev", new Integer(prev));
 
        request.setAttribute("month", m);
        List<Landmark> landmarkList = LandmarkPersistenceUtils.selectLandmarksByMonth(first, first+INTERVAL, month);
        request.setAttribute("landmarkList", landmarkList);

        return mapping.findForward("success");
    }
}

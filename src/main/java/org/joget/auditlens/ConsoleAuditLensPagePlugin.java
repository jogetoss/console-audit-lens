package org.joget.auditlens;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joget.apps.app.dao.AuditTrailDao;
import org.joget.apps.app.model.AuditTrail;
import org.joget.apps.app.model.ConsolePagePluginAbstract;
import org.joget.apps.app.service.AppUtil;
import org.joget.plugin.base.ConsolePagePlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.service.WorkflowUserManager;

public class ConsoleAuditLensPagePlugin extends ConsolePagePluginAbstract {

    private static final int DEFAULT_ROWS = 200;

    @Override
    public String getName() {
        return "ConsoleAuditLensPage";
    }

    @Override
    public String getVersion() {
        return "9.0.0";
    }

    @Override
    public String getDescription() {
        return "Unified who-changed-what monitor page for setup, plugin settings and governance actions.";
    }

    @Override
    public String getLabel() {
        return "Console Audit Lens";
    }

    @Override
    public String getI18nLabel() {
        return "Console Audit Lens";
    }

    @Override
    public String getPluginIcon() {
        return "<i class=\"fas fa-clipboard-check\"></i>";
    }

    @Override
    public int getOrder() {
        return 785;
    }

    @Override
    public ConsolePagePlugin.Location getLocation() {
        return ConsolePagePlugin.Location.MONITOR;
    }

    @Override
    public boolean isAuthorized() {
        WorkflowUserManager wum = (WorkflowUserManager) AppUtil.getApplicationContext().getBean("workflowUserManager");
        return wum.isCurrentUserInRole(WorkflowUserManager.ROLE_ADMIN);
    }

    @Override
    public String render(HttpServletRequest request, HttpServletResponse response) {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");

        String search = trim(request.getParameter("search"));
        String username = trim(request.getParameter("username"));
        String area = trim(request.getParameter("area"));
        String dateFrom = trim(request.getParameter("dateFrom"));
        String dateTo = trim(request.getParameter("dateTo"));

        int rows = parseInt(request.getParameter("rows"), DEFAULT_ROWS);
        int start = parseInt(request.getParameter("start"), 0);

        List<AuditTrail> audits = queryAudits(search, username, dateFrom, dateTo, start, rows);
        List<Map<String, Object>> records = filterAndMap(audits, area);

        long totalCount = countAudits(search, username, dateFrom, dateTo);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("request", request);
        data.put("records", records);
        data.put("search", search);
        data.put("username", username);
        data.put("area", area);
        data.put("dateFrom", dateFrom);
        data.put("dateTo", dateTo);
        data.put("rows", rows);
        data.put("start", start);
        data.put("totalCount", totalCount);
        data.put("nextStart", start + rows);
        data.put("prevStart", Math.max(0, start - rows));
        data.put("exportUrl", buildExportUrl(request, search, username, area, dateFrom, dateTo));

        return pluginManager.getPluginFreeMarkerTemplate(data, getClassName(), "/templates/ConsoleAuditLensPage.ftl", null);
    }

    @ConsolePagePlugin.Path("/export")
    public void export(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isAuthorized()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String search = trim(request.getParameter("search"));
        String username = trim(request.getParameter("username"));
        String area = trim(request.getParameter("area"));
        String dateFrom = trim(request.getParameter("dateFrom"));
        String dateTo = trim(request.getParameter("dateTo"));

        List<AuditTrail> audits = queryAudits(search, username, dateFrom, dateTo, 0, 5000);
        List<Map<String, Object>> records = filterAndMap(audits, area);

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=console-audit-lens.csv");

        StringBuilder sb = new StringBuilder();
        sb.append("timestamp,username,area,class,method,message,appId");
        sb.append('\n');
        for (Map<String, Object> row : records) {
            csv(sb, row.get("timestamp"));
            sb.append(',');
            csv(sb, row.get("username"));
            sb.append(',');
            csv(sb, row.get("area"));
            sb.append(',');
            csv(sb, row.get("clazz"));
            sb.append(',');
            csv(sb, row.get("method"));
            sb.append(',');
            csv(sb, row.get("message"));
            sb.append(',');
            csv(sb, row.get("appId"));
            sb.append('\n');
        }

        response.getWriter().write(sb.toString());
    }

    protected List<Map<String, Object>> filterAndMap(List<AuditTrail> audits, String areaFilter) {
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (AuditTrail a : audits) {
            String area = detectArea(a);
            if (!areaFilter.isEmpty() && !areaFilter.equalsIgnoreCase(area)) {
                continue;
            }

            Map<String, Object> row = new HashMap<String, Object>();
            row.put("timestamp", formatTimestamp(a.getTimestamp()));
            row.put("username", nvl(a.getUsername()));
            row.put("area", area);
            row.put("clazz", nvl(a.getClazz()));
            row.put("method", nvl(a.getMethod()));
            row.put("message", nvl(a.getMessage()));
            row.put("appId", nvl(a.getAppId()));
            records.add(row);
        }
        return records;
    }

    protected List<AuditTrail> queryAudits(String search, String username, String dateFrom, String dateTo, int start, int rows) {
        AuditTrailDao auditTrailDao = (AuditTrailDao) AppUtil.getApplicationContext().getBean("AuditTrailDao");

        StringBuilder condition = new StringBuilder();
        List<Object> args = new ArrayList<Object>();

        if (!search.isEmpty()) {
            appendAnd(condition, "(e.username like ? or e.clazz like ? or e.method like ? or e.message like ?)");
            String like = "%" + search + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }

        if (!username.isEmpty()) {
            appendAnd(condition, "e.username = ?");
            args.add(username);
        }

        Date from = parseDate(dateFrom, true);
        Date to = parseDate(dateTo, false);
        if (from != null && to != null) {
            appendAnd(condition, "e.timestamp >= ? and e.timestamp <= ?");
            args.add(from);
            args.add(to);
        }

        String where = condition.length() > 0 ? "where " + condition.toString() : "";
        return auditTrailDao.getAuditTrails(where, args.toArray(), "timestamp", true, start, rows);
    }

    protected long countAudits(String search, String username, String dateFrom, String dateTo) {
        AuditTrailDao auditTrailDao = (AuditTrailDao) AppUtil.getApplicationContext().getBean("AuditTrailDao");

        StringBuilder condition = new StringBuilder();
        List<Object> args = new ArrayList<Object>();

        if (!search.isEmpty()) {
            appendAnd(condition, "(e.username like ? or e.clazz like ? or e.method like ? or e.message like ?)");
            String like = "%" + search + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }

        if (!username.isEmpty()) {
            appendAnd(condition, "e.username = ?");
            args.add(username);
        }

        Date from = parseDate(dateFrom, true);
        Date to = parseDate(dateTo, false);
        if (from != null && to != null) {
            appendAnd(condition, "e.timestamp >= ? and e.timestamp <= ?");
            args.add(from);
            args.add(to);
        }

        String where = condition.length() > 0 ? "where " + condition.toString() : "";
        Long c = auditTrailDao.count(where, args.toArray());
        return c != null ? c.longValue() : 0L;
    }

    protected String detectArea(AuditTrail a) {
        String clazz = nvl(a.getClazz()).toLowerCase();
        String method = nvl(a.getMethod()).toLowerCase();
        String message = nvl(a.getMessage()).toLowerCase();

        if (clazz.contains("setupmanager") || message.startsWith("governance_") || message.contains("systemtheme") || message.contains("smtp") || message.contains("directorymanagerimpl")) {
            if (message.startsWith("governance_") || clazz.contains("governance")) {
                return "governance";
            }
            return "setup";
        }

        if (clazz.contains("pluginmanager") || method.contains("plugin") || message.contains("plugin_config_") || message.contains("installplugin") || message.contains("uninstallplugin")) {
            return "plugin";
        }

        if (clazz.contains("governance") || method.contains("governance") || message.contains("governance")) {
            return "governance";
        }

        return "other";
    }

    protected String buildExportUrl(HttpServletRequest request, String search, String username, String area, String dateFrom, String dateTo) {
        StringBuilder url = new StringBuilder();
        url.append(request.getContextPath()).append("/web/console/plugin/").append(getName()).append("/export?");
        url.append("search=").append(urlEncode(search));
        url.append("&username=").append(urlEncode(username));
        url.append("&area=").append(urlEncode(area));
        url.append("&dateFrom=").append(urlEncode(dateFrom));
        url.append("&dateTo=").append(urlEncode(dateTo));
        return url.toString();
    }

    protected String urlEncode(String s) {
        try {
            return URLEncoder.encode(nvl(s), "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    protected void appendAnd(StringBuilder condition, String clause) {
        if (condition.length() > 0) {
            condition.append(" and ");
        }
        condition.append(clause);
    }

    protected Date parseDate(String value, boolean startOfDay) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            String[] parts = value.split("-");
            if (parts.length != 3) {
                return null;
            }
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, Integer.parseInt(parts[0]));
            c.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
            c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
            c.set(Calendar.HOUR_OF_DAY, startOfDay ? 0 : 23);
            c.set(Calendar.MINUTE, startOfDay ? 0 : 59);
            c.set(Calendar.SECOND, startOfDay ? 0 : 59);
            c.set(Calendar.MILLISECOND, startOfDay ? 0 : 999);
            return c.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    protected String formatTimestamp(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    protected int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(trim(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    protected String trim(String s) {
        return s == null ? "" : s.trim();
    }

    protected String nvl(String s) {
        return s == null ? "" : s;
    }

    protected void csv(StringBuilder sb, Object val) {
        String s = String.valueOf(val == null ? "" : val);
        sb.append('"').append(s.replace("\"", "\"\"")).append('"');
    }
}

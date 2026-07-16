package com.xinke.erp.controller;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.xinke.common.annotation.Anonymous;
import com.xinke.common.annotation.Log;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.core.page.TableDataInfo;
import com.xinke.common.enums.BusinessType;
import com.xinke.common.utils.poi.ExcelUtil;
import com.xinke.erp.domain.DyCaptureReport;
import com.xinke.erp.domain.DyViewerLeadExport;
import com.xinke.erp.mapper.DyViewerLeadMapper;
import com.xinke.erp.service.IDyViewerLeadService;
import com.xinke.system.domain.SysConfig;
import com.xinke.system.service.ISysConfigService;

@RestController
public class DyViewerLeadController extends BaseController
{
    private static final int PLUGIN_DISABLED_CODE = 46001;
    private static final int PLUGIN_VERSION_BLOCKED_CODE = 46002;
    private static final int PLUGIN_CLIENT_DISABLED_CODE = 46003;
    private static final String CONFIG_ENABLED = "live.plugin.enabled";
    private static final String CONFIG_MIN_VERSION = "live.plugin.minVersion";
    private static final String CONFIG_LATEST_VERSION = "live.plugin.latestVersion";

    @Autowired
    private IDyViewerLeadService dyViewerLeadService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private DyViewerLeadMapper dyViewerLeadMapper;

    @Anonymous
    @PostMapping({ "/live/viewer/report", "/live/userRoom/report" })
    public AjaxResult report(@RequestBody DyCaptureReport report)
    {
        AjaxResult blocked = checkPluginControl(report);
        if (blocked != null)
        {
            return blocked;
        }
        Map<String, Object> data = dyViewerLeadService.report(report);
        markPluginClientSuccess(report);
        return success(data);
    }

    @PreAuthorize("@ss.hasPermi('live:plugin:control')")
    @GetMapping("/live/plugin/control")
    public AjaxResult pluginControl()
    {
        ensurePluginConfig();
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", pluginEnabled());
        data.put("minVersion", configValue(CONFIG_MIN_VERSION, "4.0.8"));
        data.put("latestVersion", configValue(CONFIG_LATEST_VERSION, "4.0.8"));
        data.put("disabledCode", PLUGIN_DISABLED_CODE);
        data.put("versionBlockedCode", PLUGIN_VERSION_BLOCKED_CODE);
        data.put("clientDisabledCode", PLUGIN_CLIENT_DISABLED_CODE);
        return success(data);
    }

    @PreAuthorize("@ss.hasPermi('live:plugin:control')")
    @PutMapping("/live/plugin/control")
    public AjaxResult updatePluginControl(@RequestBody Map<String, Object> form)
    {
        ensurePluginConfig();
        if (form != null && form.containsKey("enabled"))
        {
            upsertConfig(CONFIG_ENABLED, "Live plugin enabled", truthy(String.valueOf(form.get("enabled"))) ? "true" : "false",
                    "When disabled, reports receive a stop response.");
        }
        if (form != null && form.containsKey("minVersion"))
        {
            upsertConfig(CONFIG_MIN_VERSION, "Live plugin minimum version", str(form.get("minVersion"), "4.0.8"),
                    "Reports from lower plugin versions are blocked.");
        }
        if (form != null && form.containsKey("latestVersion"))
        {
            upsertConfig(CONFIG_LATEST_VERSION, "Live plugin latest version", str(form.get("latestVersion"), "4.0.8"),
                    "Recommended plugin version.");
        }
        configService.resetConfigCache();
        return pluginControl();
    }

    @PreAuthorize("@ss.hasPermi('live:plugin:control')")
    @GetMapping("/live/plugin/client/list")
    public TableDataInfo pluginClientList(@RequestParam Map<String, Object> query)
    {
        ensurePluginClientTable();
        startPage();
        return getDataTable(dyViewerLeadMapper.selectPluginClientList(query));
    }

    @PreAuthorize("@ss.hasPermi('live:plugin:control')")
    @PutMapping("/live/plugin/client/{clientId}")
    public AjaxResult updatePluginClient(@PathVariable String clientId, @RequestBody Map<String, Object> form)
    {
        ensurePluginClientTable();
        Map<String, Object> data = new HashMap<>();
        data.put("clientId", clientId);
        if (form != null && form.containsKey("clientName"))
        {
            data.put("clientName", str(form.get("clientName"), ""));
        }
        if (form != null && form.containsKey("status"))
        {
            data.put("status", truthy(String.valueOf(form.get("status"))) ? "1" : "0");
        }
        if (form != null && form.containsKey("remark"))
        {
            data.put("remark", str(form.get("remark"), ""));
        }
        return toAjax(dyViewerLeadMapper.updatePluginClient(data));
    }

    @PreAuthorize("@ss.hasPermi('live:room:list')")
    @GetMapping("/live/room/list")
    public TableDataInfo roomList(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = dyViewerLeadService.listRooms(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('live:room:query')")
    @GetMapping("/live/room/{roomKey}")
    public AjaxResult getRoom(@PathVariable String roomKey)
    {
        return success(dyViewerLeadService.getRoom(roomKey));
    }

    @PreAuthorize("@ss.hasPermi('live:room:add')")
    @Log(title = "Douyin live room", businessType = BusinessType.INSERT)
    @PostMapping("/live/room")
    public AjaxResult addRoom(@RequestBody Map<String, Object> form)
    {
        return toAjax(dyViewerLeadService.addRoom(form));
    }

    @PreAuthorize("@ss.hasPermi('live:room:edit')")
    @Log(title = "Douyin live room", businessType = BusinessType.UPDATE)
    @PutMapping("/live/room/{roomKey}")
    public AjaxResult editRoom(@PathVariable String roomKey, @RequestBody Map<String, Object> form)
    {
        return toAjax(dyViewerLeadService.updateRoom(roomKey, form));
    }

    @PreAuthorize("@ss.hasPermi('live:room:remove')")
    @Log(title = "Douyin live room", businessType = BusinessType.DELETE)
    @DeleteMapping("/live/room/{roomKey}")
    public AjaxResult removeRoom(@PathVariable String roomKey)
    {
        return toAjax(dyViewerLeadService.deleteRoom(roomKey));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @GetMapping("/live/viewer/lead/list")
    public TableDataInfo list(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = dyViewerLeadService.listLeads(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @Log(title = "Douyin viewer lead", businessType = BusinessType.EXPORT)
    @PostMapping("/live/viewer/lead/export")
    public void export(HttpServletResponse response, @RequestParam Map<String, Object> query)
    {
        List<DyViewerLeadExport> list = dyViewerLeadService.exportLeads(query);
        ExcelUtil<DyViewerLeadExport> util = new ExcelUtil<>(DyViewerLeadExport.class);
        util.exportExcel(response, list, "live_viewer_leads");
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @GetMapping("/live/viewer/room/suggestions")
    public AjaxResult roomSuggestions(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(dyViewerLeadService.listRoomSuggestions(keyword));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @GetMapping("/live/viewer/owner/suggestions")
    public AjaxResult ownerSuggestions(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(dyViewerLeadService.listOwnerSuggestions(keyword));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:query')")
    @GetMapping("/live/viewer/lead/{leadId}")
    public AjaxResult getInfo(@PathVariable Long leadId)
    {
        return success(dyViewerLeadService.getLeadDetail(leadId));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:edit')")
    @Log(title = "Douyin viewer lead", businessType = BusinessType.UPDATE)
    @PutMapping("/live/viewer/lead/{leadId}")
    public AjaxResult edit(@PathVariable Long leadId, @RequestBody Map<String, Object> form)
    {
        return toAjax(dyViewerLeadService.updateLead(leadId, form));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:follow')")
    @Log(title = "Douyin viewer follow", businessType = BusinessType.INSERT)
    @PostMapping("/live/viewer/lead/{leadId}/follow")
    public AjaxResult addFollow(@PathVariable Long leadId, @RequestBody Map<String, Object> form)
    {
        return toAjax(dyViewerLeadService.addFollowRecord(leadId, form, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @GetMapping("/live/viewer/summary")
    public AjaxResult summary(@RequestParam Map<String, Object> query)
    {
        return success(dyViewerLeadService.summary(query));
    }

    @PreAuthorize("@ss.hasPermi('live:viewer:list')")
    @GetMapping("/live/viewer/bi")
    public AjaxResult bi(@RequestParam Map<String, Object> query)
    {
        return success(dyViewerLeadService.biDashboard(query));
    }

    private AjaxResult checkPluginControl(DyCaptureReport report)
    {
        ensurePluginConfig();
        Map<String, Object> client = upsertAndGetPluginClient(report);
        String minVersion = configValue(CONFIG_MIN_VERSION, "4.0.8");
        String latestVersion = configValue(CONFIG_LATEST_VERSION, "4.0.8");
        String clientVersion = report == null ? "" : str(report.getPluginVersion(), "");
        String clientId = report == null ? "" : str(report.getClientId(), "");
        Map<String, Object> data = new HashMap<>();
        data.put("pluginAction", "stop");
        data.put("minVersion", minVersion);
        data.put("latestVersion", latestVersion);
        data.put("clientVersion", clientVersion);
        data.put("clientId", clientId);

        if (client != null && "1".equals(str(client.get("status"), "0")))
        {
            data.put("reason", "client_disabled");
            markPluginClientError(report, "client_disabled");
            return pluginStop(PLUGIN_CLIENT_DISABLED_CODE, "This plugin client is disabled", data);
        }

        if (!pluginEnabled())
        {
            data.put("reason", "disabled");
            markPluginClientError(report, "global_disabled");
            return pluginStop(PLUGIN_DISABLED_CODE, "Live capture plugin is disabled", data);
        }
        if (compareVersion(clientVersion, minVersion) < 0)
        {
            data.put("reason", "version_blocked");
            markPluginClientError(report, "version_blocked");
            return pluginStop(PLUGIN_VERSION_BLOCKED_CODE,
                    "Plugin version is too old. Please update to " + minVersion + " or later", data);
        }
        return null;
    }

    private AjaxResult pluginStop(int code, String message, Map<String, Object> data)
    {
        return AjaxResult.error(code, message)
                .put("pluginAction", "stop")
                .put("reason", data.get("reason"))
                .put("minVersion", data.get("minVersion"))
                .put("latestVersion", data.get("latestVersion"))
                .put("clientVersion", data.get("clientVersion"))
                .put("clientId", data.get("clientId"))
                .put("data", data);
    }

    private Map<String, Object> upsertAndGetPluginClient(DyCaptureReport report)
    {
        String clientId = report == null ? "" : str(report.getClientId(), "");
        if (clientId.isEmpty())
        {
            return null;
        }
        ensurePluginClientTable();
        Map<String, Object> data = new HashMap<>();
        data.put("clientId", clientId);
        data.put("clientName", str(report.getClientName(), ""));
        data.put("pluginVersion", str(report.getPluginVersion(), ""));
        data.put("source", str(report.getSource(), ""));
        data.put("roomKey", str(report.getRoomKey(), ""));
        data.put("liveRoomName", str(report.getLiveRoomName(), ""));
        data.put("pageUrl", str(report.getPageUrl(), ""));
        data.put("requestUrl", str(report.getRequestUrl(), ""));
        dyViewerLeadMapper.upsertPluginClient(data);
        return dyViewerLeadMapper.selectPluginClientById(clientId);
    }

    private void markPluginClientSuccess(DyCaptureReport report)
    {
        String clientId = report == null ? "" : str(report.getClientId(), "");
        if (clientId.isEmpty())
        {
            return;
        }
        ensurePluginClientTable();
        Map<String, Object> data = new HashMap<>();
        data.put("clientId", clientId);
        dyViewerLeadMapper.markPluginClientSuccess(data);
    }

    private void markPluginClientError(DyCaptureReport report, String errorMsg)
    {
        String clientId = report == null ? "" : str(report.getClientId(), "");
        if (clientId.isEmpty())
        {
            return;
        }
        ensurePluginClientTable();
        Map<String, Object> data = new HashMap<>();
        data.put("clientId", clientId);
        data.put("errorMsg", errorMsg);
        dyViewerLeadMapper.markPluginClientError(data);
    }

    private void ensurePluginClientTable()
    {
        dyViewerLeadMapper.createPluginClientTable();
    }

    private boolean pluginEnabled()
    {
        return truthy(configValue(CONFIG_ENABLED, "true"));
    }

    private void ensurePluginConfig()
    {
        ensureConfig(CONFIG_ENABLED, "Live plugin enabled", "true", "When disabled, reports receive a stop response.");
        ensureConfig(CONFIG_MIN_VERSION, "Live plugin minimum version", "4.0.8", "Reports from lower plugin versions are blocked.");
        ensureConfig(CONFIG_LATEST_VERSION, "Live plugin latest version", "4.0.8", "Recommended plugin version.");
    }

    private void ensureConfig(String key, String name, String value, String remark)
    {
        String currentValue = configService.selectConfigByKey(key);
        if (currentValue == null || currentValue.trim().isEmpty())
        {
            upsertConfig(key, name, value, remark);
        }
    }

    private void upsertConfig(String key, String name, String value, String remark)
    {
        SysConfig query = new SysConfig();
        query.setConfigKey(key);
        List<SysConfig> configs = configService.selectConfigList(query);
        SysConfig config = configs == null || configs.isEmpty() ? null : configs.get(0);
        if (config == null)
        {
            config = new SysConfig();
            config.setConfigName(name);
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigType("Y");
            config.setRemark(remark);
            config.setCreateBy("admin");
            configService.insertConfig(config);
        }
        else
        {
            config.setConfigName(name);
            config.setConfigValue(value);
            config.setConfigType("Y");
            config.setRemark(remark);
            config.setUpdateBy("admin");
            configService.updateConfig(config);
        }
    }

    private String configValue(String key, String defaultValue)
    {
        String value = configService.selectConfigByKey(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private boolean truthy(String value)
    {
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "Y".equalsIgnoreCase(value);
    }

    private String str(Object value, String defaultValue)
    {
        return value == null ? defaultValue : String.valueOf(value).trim();
    }

    private int compareVersion(String current, String required)
    {
        if (current == null || current.trim().isEmpty())
        {
            return -1;
        }
        String[] a = current.trim().split("\\.");
        String[] b = required == null ? new String[] { "0" } : required.trim().split("\\.");
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++)
        {
            int ai = parseVersionPart(i < a.length ? a[i] : "0");
            int bi = parseVersionPart(i < b.length ? b[i] : "0");
            if (ai != bi)
            {
                return ai - bi;
            }
        }
        return 0;
    }

    private int parseVersionPart(String value)
    {
        try
        {
            return Integer.parseInt(value.replaceAll("[^0-9].*$", ""));
        }
        catch (Exception e)
        {
            return 0;
        }
    }
}

package com.xinke.datahub.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.constant.DataHubConstants;
import com.xinke.datahub.domain.DataHubAcl;
import com.xinke.datahub.domain.DataHubColumn;
import com.xinke.datahub.domain.DataHubDataset;
import com.xinke.datahub.domain.DataHubDataVersion;
import com.xinke.datahub.domain.DataHubImportJob;
import com.xinke.datahub.domain.dto.DataHubAccess;
import com.xinke.datahub.domain.dto.DataHubAclEntryRequest;
import com.xinke.datahub.domain.dto.DataHubAclRequest;
import com.xinke.datahub.domain.dto.DataHubDataPage;
import com.xinke.datahub.domain.dto.DataHubDataQuery;
import com.xinke.datahub.domain.dto.DataHubDatasetDetail;
import com.xinke.datahub.domain.dto.DataHubJobView;
import com.xinke.datahub.mapper.DataHubMapper;

@Service
public class DataHubDatasetService
{
    private static final Set<String> FOLDER_SCOPES = Set.of("ALL", "OWNED", "FOLDER", "UNCLASSIFIED", "SHARED");

    private final DataHubMapper mapper;
    private final DataHubDynamicQueryService queryService;

    public DataHubDatasetService(DataHubMapper mapper, DataHubDynamicQueryService queryService)
    {
        this.mapper = mapper;
        this.queryService = queryService;
    }

    public List<DataHubDataset> list(DataHubDataset query, Long userId, boolean admin)
    {
        if (query == null) query = new DataHubDataset();
        String scope = query.getFolderScope();
        scope = scope == null || scope.isBlank() ? "ALL" : scope.strip().toUpperCase(Locale.ROOT);
        if (!FOLDER_SCOPES.contains(scope)) throw new ServiceException("文件夹查询范围不合法");
        if ("FOLDER".equals(scope) && (query.getFolderId() == null || query.getFolderId() <= 0))
            throw new ServiceException("请选择有效文件夹");
        if (!"FOLDER".equals(scope)) query.setFolderId(null);
        query.setFolderScope(scope);
        return mapper.selectDatasetList(query, userId, admin);
    }

    public DataHubDatasetDetail detail(Long datasetId, Long userId, boolean admin)
    {
        DataHubDataset dataset = requireDataset(datasetId);
        int accessMask = requireAccess(datasetId, userId, admin, DataHubConstants.ACCESS_READ);
        dataset.setAccessMask(accessMask);
        DataHubDatasetDetail detail = new DataHubDatasetDetail();
        detail.setDataset(dataset);
        detail.setColumns(mapper.selectColumnsBySchemaId(datasetId, dataset.getCurrentSchemaId()));
        detail.setAccess(new DataHubAccess(accessMask));
        return detail;
    }

    public DataHubDataPage queryData(Long datasetId, DataHubDataQuery query, Long userId, boolean admin)
    {
        requireAccess(datasetId, userId, admin, DataHubConstants.ACCESS_READ);
        DataHubDataVersion version = mapper.selectCurrentVersion(datasetId);
        if (version == null) throw new ServiceException("数据表当前版本不存在");
        List<DataHubColumn> columns = mapper.selectColumnsBySchemaId(datasetId, version.getSchemaId());
        return queryService.query(version, columns, query);
    }

    public List<DataHubJobView> jobs(Long datasetId, Long userId, boolean admin)
    {
        requireAccess(datasetId, userId, admin, DataHubConstants.ACCESS_READ);
        List<DataHubJobView> views = new ArrayList<>();
        for (DataHubImportJob job : mapper.selectJobsByDatasetId(datasetId)) views.add(DataHubJobView.from(job));
        return views;
    }

    public List<DataHubAcl> acl(Long datasetId, Long userId, boolean admin)
    {
        requireAccess(datasetId, userId, admin, DataHubConstants.ACCESS_MANAGE);
        return mapper.selectAclList(datasetId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceAcl(Long datasetId, DataHubAclRequest request, Long userId, String username, boolean admin)
    {
        DataHubDataset dataset = requireDataset(datasetId);
        requireAccess(datasetId, userId, admin, DataHubConstants.ACCESS_MANAGE);
        List<DataHubAclEntryRequest> requested = request == null || request.getEntries() == null
                ? List.of() : request.getEntries();
        if (requested.size() > 100) throw new ServiceException("单张数据表最多配置100条授权");

        Set<String> unique = new HashSet<>();
        List<DataHubAcl> entries = new ArrayList<>();
        for (DataHubAclEntryRequest item : requested)
        {
            if (item == null || item.getSubjectId() == null) throw new ServiceException("授权对象不能为空");
            String type = item.getSubjectType() == null ? "" : item.getSubjectType().toUpperCase(Locale.ROOT);
            if (!DataHubConstants.SUBJECT_USER.equals(type) && !DataHubConstants.SUBJECT_ROLE.equals(type))
                throw new ServiceException("授权对象类型不合法");
            if (!unique.add(type + ":" + item.getSubjectId())) throw new ServiceException("同一用户或角色不能重复授权");
            int mask = item.getPermissionMask() == null ? 0 : item.getPermissionMask();
            if (mask < 1 || mask > DataHubConstants.ACCESS_ALL || (mask & DataHubConstants.ACCESS_READ) == 0)
                throw new ServiceException("授权至少需要包含查看权限");
            if (DataHubConstants.SUBJECT_USER.equals(type))
            {
                if (item.getSubjectId().equals(dataset.getOwnerUserId())) continue;
                if (mapper.countActiveUser(item.getSubjectId()) == 0) throw new ServiceException("授权用户不存在或已停用");
            }
            else if (mapper.countActiveRole(item.getSubjectId()) == 0)
            {
                throw new ServiceException("授权角色不存在或已停用");
            }
            DataHubAcl acl = new DataHubAcl();
            acl.setDatasetId(datasetId);
            acl.setSubjectType(type);
            acl.setSubjectId(item.getSubjectId());
            acl.setPermissionMask(mask);
            acl.setCreateBy(username);
            entries.add(acl);
        }
        mapper.deleteAclByDatasetId(datasetId);
        if (!entries.isEmpty()) mapper.insertAclList(entries);
    }

    public List<Map<String, Object>> userOptions(String keyword)
    {
        return mapper.selectUserOptions(normalizeKeyword(keyword));
    }

    public List<Map<String, Object>> roleOptions(String keyword)
    {
        return mapper.selectRoleOptions(normalizeKeyword(keyword));
    }

    private int requireAccess(Long datasetId, Long userId, boolean admin, int required)
    {
        requireDataset(datasetId);
        if (admin) return DataHubConstants.ACCESS_ALL;
        Integer mask = mapper.selectAccessMask(datasetId, userId);
        int accessMask = mask == null ? 0 : mask;
        if ((accessMask & required) == 0) throw new ServiceException("无权访问该数据表");
        return accessMask;
    }

    private DataHubDataset requireDataset(Long datasetId)
    {
        DataHubDataset dataset = datasetId == null ? null : mapper.selectDatasetById(datasetId);
        if (dataset == null) throw new ServiceException("数据表不存在");
        return dataset;
    }

    private String normalizeKeyword(String keyword)
    {
        if (keyword == null || keyword.isBlank()) return null;
        String value = keyword.strip();
        return value.length() > 50 ? value.substring(0, 50) : value;
    }
}

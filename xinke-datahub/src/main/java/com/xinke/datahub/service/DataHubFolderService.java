package com.xinke.datahub.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.domain.DataHubFolder;
import com.xinke.datahub.domain.DataHubFolderItem;
import com.xinke.datahub.domain.dto.DataHubFolderItemMoveRequest;
import com.xinke.datahub.domain.dto.DataHubFolderItemView;
import com.xinke.datahub.domain.dto.DataHubFolderRequest;
import com.xinke.datahub.domain.dto.DataHubFolderTreeNode;
import com.xinke.datahub.mapper.DataHubFolderMapper;
import com.xinke.datahub.naming.EnglishNameGenerator;

@Service
public class DataHubFolderService
{
    static final long ROOT_FOLDER_ID = 0L;
    static final int MAX_FOLDER_DEPTH = 8;

    private final DataHubFolderMapper mapper;
    private final EnglishNameGenerator nameGenerator;

    public DataHubFolderService(DataHubFolderMapper mapper, EnglishNameGenerator nameGenerator)
    {
        this.mapper = mapper;
        this.nameGenerator = nameGenerator;
    }

    public List<DataHubFolderTreeNode> tree(Long userId, boolean admin)
    {
        List<DataHubFolder> folders = mapper.selectOwnedFolderList(requireUserId(userId), admin);
        Map<Long, DataHubFolder> folderMap = folderMap(folders);
        for (DataHubFolder folder : folders) depth(folder.getParentFolderId(), folderMap);

        Map<Long, DataHubFolderTreeNode> nodes = new LinkedHashMap<>();
        for (DataHubFolder folder : folders) nodes.put(folder.getFolderId(), node(folder));
        List<DataHubFolderTreeNode> roots = new ArrayList<>();
        for (DataHubFolder folder : folders)
        {
            DataHubFolderTreeNode current = nodes.get(folder.getFolderId());
            if (isRoot(folder.getParentFolderId())) roots.add(current);
            else nodes.get(folder.getParentFolderId()).getChildren().add(current);
        }
        sortTree(roots);
        return roots;
    }

    @Transactional(rollbackFor = Exception.class)
    public DataHubFolder create(DataHubFolderRequest request, Long userId, String username)
    {
        Long ownerId = requireUserId(userId);
        String name = validateFolderName(request == null ? null : request.getFolderName());
        long parentId = normalizeFolderId(request == null ? null : request.getParentFolderId());
        List<DataHubFolder> folders = mapper.selectOwnedFolderListForUpdate(ownerId);
        Map<Long, DataHubFolder> folderMap = folderMap(folders);
        if (!isRoot(parentId) && !folderMap.containsKey(parentId)) throw new ServiceException("上级文件夹不存在");
        if (depth(parentId, folderMap) >= MAX_FOLDER_DEPTH)
            throw new ServiceException("文件夹最多支持" + MAX_FOLDER_DEPTH + "层");

        DataHubFolder folder = new DataHubFolder();
        folder.setParentFolderId(parentId);
        folder.setFolderName(name);
        folder.setNormalizedName(nameGenerator.normalizeNameKey(name));
        folder.setOwnerUserId(ownerId);
        folder.setOwnerUserName(username);
        folder.setSortOrder(nextSortOrder(parentId, folders));
        folder.setLockVersion(0);
        folder.setCreateBy(username);
        folder.setUpdateBy(username);
        try { mapper.insertFolder(folder); }
        catch (DuplicateKeyException e) { throw new ServiceException("同级文件夹名称已经存在"); }
        return folder;
    }

    @Transactional(rollbackFor = Exception.class)
    public DataHubFolder update(Long folderId, DataHubFolderRequest request, Long userId, String username)
    {
        Long ownerId = requireUserId(userId);
        requirePositiveId(folderId, "文件夹不存在");
        if (request == null || request.getLockVersion() == null) throw staleFolder();
        String name = validateFolderName(request.getFolderName());
        long parentId = normalizeFolderId(request.getParentFolderId());
        List<DataHubFolder> folders = mapper.selectOwnedFolderListForUpdate(ownerId);
        Map<Long, DataHubFolder> folderMap = folderMap(folders);
        DataHubFolder folder = folderMap.get(folderId);
        if (folder == null) throw new ServiceException("文件夹不存在");
        if (!request.getLockVersion().equals(folder.getLockVersion())) throw staleFolder();
        if (folderId.equals(parentId)) throw new ServiceException("上级文件夹不能是自己");
        if (!isRoot(parentId) && !folderMap.containsKey(parentId)) throw new ServiceException("上级文件夹不存在");
        if (isDescendant(parentId, folderId, folderMap)) throw new ServiceException("不能移动到自己的下级文件夹");

        int targetParentDepth = depth(parentId, folderMap);
        int subtreeHeight = subtreeHeight(folderId, folderMap, new HashSet<>());
        if (targetParentDepth + subtreeHeight > MAX_FOLDER_DEPTH)
            throw new ServiceException("移动后文件夹层级不能超过" + MAX_FOLDER_DEPTH + "层");

        folder.setParentFolderId(parentId);
        folder.setFolderName(name);
        folder.setNormalizedName(nameGenerator.normalizeNameKey(name));
        folder.setOwnerUserId(ownerId);
        folder.setUpdateBy(username);
        try
        {
            if (mapper.updateFolder(folder) != 1) throw staleFolder();
        }
        catch (DuplicateKeyException e)
        {
            throw new ServiceException("同级文件夹名称已经存在");
        }
        folder.setLockVersion(folder.getLockVersion() + 1);
        return folder;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long folderId, Integer lockVersion, Long userId, String username, boolean admin)
    {
        Long ownerId = requireUserId(userId);
        requirePositiveId(folderId, "文件夹不存在");
        if (lockVersion == null) throw staleFolder();
        List<DataHubFolder> folders = mapper.selectOwnedFolderListForUpdate(ownerId);
        DataHubFolder folder = folderMap(folders).get(folderId);
        if (folder == null) throw new ServiceException("文件夹不存在");
        if (!lockVersion.equals(folder.getLockVersion())) throw staleFolder();
        if (mapper.countActiveChildren(folderId, ownerId) > 0)
            throw new ServiceException("文件夹包含下级文件夹，不能删除");

        mapper.deleteInvisibleFolderItems(folderId, ownerId, admin);
        if (mapper.countFolderItems(folderId, ownerId) > 0)
            throw new ServiceException("文件夹中还有数据表，不能删除");
        if (mapper.softDeleteFolder(folderId, ownerId, lockVersion, username) != 1) throw staleFolder();
    }

    @Transactional(rollbackFor = Exception.class)
    public DataHubFolderItemView moveItem(Long datasetId, DataHubFolderItemMoveRequest request,
            Long userId, String username, boolean admin)
    {
        Long ownerId = requireUserId(userId);
        requirePositiveId(datasetId, "数据表不存在或无权访问");
        if (mapper.countVisibleDataset(datasetId, ownerId, admin) != 1)
            throw new ServiceException("数据表不存在或无权访问");

        long targetFolderId = normalizeFolderId(request == null ? null : request.getFolderId());
        if (!isRoot(targetFolderId)
                && mapper.selectOwnedFolderForUpdate(targetFolderId, ownerId) == null)
            throw new ServiceException("目标文件夹不存在");

        DataHubFolderItem current = mapper.selectFolderItemForUpdate(datasetId, ownerId);
        Integer expectedVersion = request == null ? null : request.getItemVersion();
        if (current == null)
        {
            if (expectedVersion != null) throw staleItem();
            if (isRoot(targetFolderId)) return new DataHubFolderItemView(datasetId, null, null);
            DataHubFolderItem item = new DataHubFolderItem();
            item.setOwnerUserId(ownerId);
            item.setFolderId(targetFolderId);
            item.setDatasetId(datasetId);
            item.setLockVersion(0);
            item.setCreateBy(username);
            item.setUpdateBy(username);
            try { mapper.insertFolderItem(item); }
            catch (DuplicateKeyException e) { throw staleItem(); }
            return new DataHubFolderItemView(datasetId, targetFolderId, 0);
        }

        if (expectedVersion == null || !expectedVersion.equals(current.getLockVersion())) throw staleItem();
        if (current.getFolderId().equals(targetFolderId))
            return new DataHubFolderItemView(datasetId, targetFolderId, current.getLockVersion());
        if (isRoot(targetFolderId))
        {
            if (mapper.deleteFolderItem(datasetId, ownerId, expectedVersion) != 1) throw staleItem();
            return new DataHubFolderItemView(datasetId, null, null);
        }
        if (mapper.updateFolderItem(datasetId, ownerId, targetFolderId, expectedVersion, username) != 1)
            throw staleItem();
        return new DataHubFolderItemView(datasetId, targetFolderId, expectedVersion + 1);
    }

    private Map<Long, DataHubFolder> folderMap(List<DataHubFolder> folders)
    {
        Map<Long, DataHubFolder> values = new HashMap<>();
        for (DataHubFolder folder : folders)
        {
            if (folder.getFolderId() == null || values.put(folder.getFolderId(), folder) != null)
                throw new ServiceException("文件夹结构异常");
        }
        return values;
    }

    private int depth(Long folderId, Map<Long, DataHubFolder> folders)
    {
        int depth = 0;
        long current = normalizeFolderId(folderId);
        Set<Long> visited = new HashSet<>();
        while (!isRoot(current))
        {
            if (!visited.add(current)) throw new ServiceException("文件夹结构存在循环");
            DataHubFolder folder = folders.get(current);
            if (folder == null) throw new ServiceException("文件夹上级结构不完整");
            depth++;
            if (depth > MAX_FOLDER_DEPTH) throw new ServiceException("文件夹层级超过限制");
            current = normalizeFolderId(folder.getParentFolderId());
        }
        return depth;
    }

    private boolean isDescendant(long candidateParent, Long sourceId, Map<Long, DataHubFolder> folders)
    {
        long current = candidateParent;
        Set<Long> visited = new HashSet<>();
        while (!isRoot(current))
        {
            if (sourceId.equals(current)) return true;
            if (!visited.add(current)) throw new ServiceException("文件夹结构存在循环");
            DataHubFolder folder = folders.get(current);
            if (folder == null) throw new ServiceException("文件夹上级结构不完整");
            current = normalizeFolderId(folder.getParentFolderId());
        }
        return false;
    }

    private int subtreeHeight(Long sourceId, Map<Long, DataHubFolder> folders, Set<Long> visiting)
    {
        if (!visiting.add(sourceId)) throw new ServiceException("文件夹结构存在循环");
        int height = 1;
        for (DataHubFolder folder : folders.values())
        {
            if (sourceId.equals(folder.getParentFolderId()))
                height = Math.max(height, 1 + subtreeHeight(folder.getFolderId(), folders, visiting));
        }
        visiting.remove(sourceId);
        return height;
    }

    private DataHubFolderTreeNode node(DataHubFolder folder)
    {
        DataHubFolderTreeNode node = new DataHubFolderTreeNode();
        node.setFolderId(folder.getFolderId());
        node.setParentFolderId(folder.getParentFolderId());
        node.setFolderName(folder.getFolderName());
        node.setSortOrder(folder.getSortOrder());
        node.setLockVersion(folder.getLockVersion());
        node.setItemCount(folder.getItemCount() == null ? 0L : folder.getItemCount());
        return node;
    }

    private void sortTree(List<DataHubFolderTreeNode> nodes)
    {
        nodes.sort(Comparator.comparing((DataHubFolderTreeNode node) ->
                node.getSortOrder() == null ? 0 : node.getSortOrder()).thenComparing(DataHubFolderTreeNode::getFolderId));
        for (DataHubFolderTreeNode node : nodes) sortTree(node.getChildren());
    }

    private int nextSortOrder(long parentId, List<DataHubFolder> folders)
    {
        int maximum = 0;
        for (DataHubFolder folder : folders)
        {
            if (normalizeFolderId(folder.getParentFolderId()) == parentId && folder.getSortOrder() != null)
                maximum = Math.max(maximum, folder.getSortOrder());
        }
        return maximum > Integer.MAX_VALUE - 10 ? maximum : maximum + 10;
    }

    private String validateFolderName(String value)
    {
        if (value == null || value.isBlank()) throw new ServiceException("请输入文件夹名称");
        String name = value.strip();
        if (name.length() > 128) throw new ServiceException("文件夹名称不能超过128个字符");
        if (".".equals(name) || "..".equals(name) || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0
                || name.chars().anyMatch(Character::isISOControl))
            throw new ServiceException("文件夹名称不能包含斜杠或控制字符");
        return name;
    }

    private Long requireUserId(Long userId)
    {
        if (userId == null || userId <= 0) throw new ServiceException("当前用户身份无效");
        return userId;
    }

    private void requirePositiveId(Long value, String message)
    {
        if (value == null || value <= 0) throw new ServiceException(message);
    }

    private long normalizeFolderId(Long folderId)
    {
        if (folderId == null) return ROOT_FOLDER_ID;
        if (folderId < 0) throw new ServiceException("文件夹编号不合法");
        return folderId;
    }

    private boolean isRoot(Long folderId)
    {
        return folderId == null || folderId == ROOT_FOLDER_ID;
    }

    private ServiceException staleFolder()
    {
        return new ServiceException("文件夹已被其他操作修改，请刷新后重试", HttpStatus.CONFLICT.value());
    }

    private ServiceException staleItem()
    {
        return new ServiceException("数据表所在文件夹已发生变化，请刷新后重试", HttpStatus.CONFLICT.value());
    }
}

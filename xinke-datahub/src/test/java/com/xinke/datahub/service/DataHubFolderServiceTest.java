package com.xinke.datahub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.domain.DataHubFolder;
import com.xinke.datahub.domain.DataHubFolderItem;
import com.xinke.datahub.domain.dto.DataHubFolderItemMoveRequest;
import com.xinke.datahub.domain.dto.DataHubFolderRequest;
import com.xinke.datahub.mapper.DataHubFolderMapper;
import com.xinke.datahub.naming.EnglishNameGenerator;

@ExtendWith(MockitoExtension.class)
class DataHubFolderServiceTest
{
    @Mock
    private DataHubFolderMapper mapper;

    private DataHubFolderService service;

    @BeforeEach
    void setUp()
    {
        service = new DataHubFolderService(mapper, new EnglishNameGenerator());
    }

    @Test
    void rejectsFoldersBelowTheEighthLevel()
    {
        List<DataHubFolder> folders = new ArrayList<>();
        for (long id = 1; id <= 8; id++) folders.add(folder(id, id - 1, "level-" + id));
        when(mapper.selectOwnedFolderListForUpdate(7L)).thenReturn(folders);
        DataHubFolderRequest request = request(8L, "too-deep", null);

        assertThrows(ServiceException.class, () -> service.create(request, 7L, "tester"));
        verify(mapper, never()).insertFolder(any());
    }

    @Test
    void rejectsMovingFolderBelowItsOwnChild()
    {
        DataHubFolder root = folder(1L, 0L, "root");
        DataHubFolder child = folder(2L, 1L, "child");
        when(mapper.selectOwnedFolderListForUpdate(7L)).thenReturn(List.of(root, child));

        assertThrows(ServiceException.class,
                () -> service.update(1L, request(2L, "root", 0), 7L, "tester"));
        verify(mapper, never()).updateFolder(any());
    }

    @Test
    void deletesOnlyEmptyFolders()
    {
        DataHubFolder folder = folder(1L, 0L, "root");
        when(mapper.selectOwnedFolderListForUpdate(7L)).thenReturn(List.of(folder));
        when(mapper.countActiveChildren(1L, 7L)).thenReturn(0);
        when(mapper.countFolderItems(1L, 7L)).thenReturn(1);

        assertThrows(ServiceException.class, () -> service.delete(1L, 0, 7L, "tester", false));
        verify(mapper, never()).softDeleteFolder(any(), any(), any(), any());
    }

    @Test
    void refusesToClassifyDatasetWithoutCurrentReadAccess()
    {
        when(mapper.countVisibleDataset(9L, 7L, false)).thenReturn(0);

        assertThrows(ServiceException.class,
                () -> service.moveItem(9L, move(1L, null), 7L, "tester", false));
        verify(mapper, never()).insertFolderItem(any());
    }

    @Test
    void detectsAStaleFolderItemMove()
    {
        when(mapper.countVisibleDataset(9L, 7L, false)).thenReturn(1);
        DataHubFolderItem current = new DataHubFolderItem();
        current.setFolderId(3L);
        current.setLockVersion(2);
        when(mapper.selectFolderItemForUpdate(9L, 7L)).thenReturn(current);

        assertThrows(ServiceException.class,
                () -> service.moveItem(9L, move(0L, 1), 7L, "tester", false));
        verify(mapper, never()).deleteFolderItem(any(), any(), any());
    }

    @Test
    void buildsOnlyTheCurrentUsersNestedTree()
    {
        DataHubFolder root = folder(1L, 0L, "root");
        root.setItemCount(2L);
        DataHubFolder child = folder(2L, 1L, "child");
        when(mapper.selectOwnedFolderList(7L, false)).thenReturn(List.of(root, child));

        var tree = service.tree(7L, false);

        assertEquals(1, tree.size());
        assertEquals("root", tree.get(0).getFolderName());
        assertEquals(2L, tree.get(0).getItemCount());
        assertEquals("child", tree.get(0).getChildren().get(0).getFolderName());
    }

    private DataHubFolder folder(Long id, Long parentId, String name)
    {
        DataHubFolder folder = new DataHubFolder();
        folder.setFolderId(id);
        folder.setParentFolderId(parentId);
        folder.setFolderName(name);
        folder.setOwnerUserId(7L);
        folder.setSortOrder(id.intValue() * 10);
        folder.setLockVersion(0);
        return folder;
    }

    private DataHubFolderRequest request(Long parentId, String name, Integer lockVersion)
    {
        DataHubFolderRequest request = new DataHubFolderRequest();
        request.setParentFolderId(parentId);
        request.setFolderName(name);
        request.setLockVersion(lockVersion);
        return request;
    }

    private DataHubFolderItemMoveRequest move(Long folderId, Integer itemVersion)
    {
        DataHubFolderItemMoveRequest request = new DataHubFolderItemMoveRequest();
        request.setFolderId(folderId);
        request.setItemVersion(itemVersion);
        return request;
    }
}

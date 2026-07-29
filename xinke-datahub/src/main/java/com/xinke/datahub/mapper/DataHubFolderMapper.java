package com.xinke.datahub.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xinke.datahub.domain.DataHubFolder;
import com.xinke.datahub.domain.DataHubFolderItem;

@Mapper
public interface DataHubFolderMapper
{
    List<DataHubFolder> selectOwnedFolderList(@Param("userId") Long userId, @Param("admin") boolean admin);

    List<DataHubFolder> selectOwnedFolderListForUpdate(Long userId);

    DataHubFolder selectOwnedFolderForUpdate(@Param("folderId") Long folderId, @Param("userId") Long userId);

    int insertFolder(DataHubFolder folder);

    int updateFolder(DataHubFolder folder);

    int softDeleteFolder(@Param("folderId") Long folderId, @Param("userId") Long userId,
            @Param("lockVersion") Integer lockVersion, @Param("username") String username);

    int countActiveChildren(@Param("folderId") Long folderId, @Param("userId") Long userId);

    int deleteInvisibleFolderItems(@Param("folderId") Long folderId, @Param("userId") Long userId,
            @Param("admin") boolean admin);

    int countFolderItems(@Param("folderId") Long folderId, @Param("userId") Long userId);

    int countVisibleDataset(@Param("datasetId") Long datasetId, @Param("userId") Long userId,
            @Param("admin") boolean admin);

    DataHubFolderItem selectFolderItemForUpdate(@Param("datasetId") Long datasetId,
            @Param("userId") Long userId);

    int insertFolderItem(DataHubFolderItem item);

    int insertFolderItemIfOwnedActive(@Param("datasetId") Long datasetId, @Param("userId") Long userId,
            @Param("folderId") Long folderId, @Param("username") String username);

    int updateFolderItem(@Param("datasetId") Long datasetId, @Param("userId") Long userId,
            @Param("folderId") Long folderId, @Param("lockVersion") Integer lockVersion,
            @Param("username") String username);

    int deleteFolderItem(@Param("datasetId") Long datasetId, @Param("userId") Long userId,
            @Param("lockVersion") Integer lockVersion);
}

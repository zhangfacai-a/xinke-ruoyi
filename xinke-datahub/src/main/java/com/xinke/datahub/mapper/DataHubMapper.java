package com.xinke.datahub.mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xinke.datahub.domain.DataHubAcl;
import com.xinke.datahub.domain.DataHubColumn;
import com.xinke.datahub.domain.DataHubDataset;
import com.xinke.datahub.domain.DataHubDataVersion;
import com.xinke.datahub.domain.DataHubImportError;
import com.xinke.datahub.domain.DataHubImportJob;
import com.xinke.datahub.domain.DataHubSchema;

@Mapper
public interface DataHubMapper
{
    int insertImportJob(DataHubImportJob job);

    int updateImportJob(DataHubImportJob job);

    int updatePreviewSheet(DataHubImportJob job);

    int queueImportJob(@Param("previewId") String previewId, @Param("userId") Long userId,
            @Param("displayName") String displayName, @Param("physicalName") String physicalName,
            @Param("schemaJson") String schemaJson, @Param("expectedSheetName") String expectedSheetName);

    int claimQueuedImportJob(String previewId);

    int requeueClaimedImportJob(@Param("previewId") String previewId, @Param("lockVersion") Integer lockVersion);

    List<String> selectQueuedPreviewIds(@Param("limit") int limit);

    int requeueStaleImportJobs(Date staleBefore);

    int failStaleParsingJobs(Date staleBefore);

    int touchImportJob(@Param("jobId") Long jobId, @Param("lockVersion") Integer lockVersion);

    int countClaimedImportJob(@Param("jobId") Long jobId, @Param("lockVersion") Integer lockVersion,
            @Param("status") String status);

    int markImportJobFailed(DataHubImportJob job);

    List<DataHubImportJob> selectExpiredStoredFiles(@Param("limit") int limit);

    int clearStoredFilePath(Long jobId);

    DataHubImportJob selectJobByPreviewId(String previewId);

    List<DataHubImportJob> selectJobsByDatasetId(Long datasetId);

    int deleteImportErrors(Long jobId);

    int insertImportErrors(@Param("errors") List<DataHubImportError> errors);

    List<DataHubImportError> selectImportErrors(Long jobId);

    int countDatasetByNameKey(String normalizedName);

    int countDatasetByCode(String datasetCode);

    int countVersionByPhysicalTable(String physicalTableName);

    int insertDataset(DataHubDataset dataset);

    int insertSchema(DataHubSchema schema);

    int insertColumns(@Param("columns") List<DataHubColumn> columns);

    int insertVersion(DataHubDataVersion version);

    int publishDataset(DataHubDataset dataset);

    List<DataHubDataset> selectDatasetList(@Param("query") DataHubDataset query, @Param("userId") Long userId,
            @Param("admin") boolean admin);

    DataHubDataset selectDatasetById(Long datasetId);

    DataHubDataVersion selectCurrentVersion(Long datasetId);

    List<DataHubColumn> selectCurrentColumns(Long datasetId);

    List<DataHubColumn> selectColumnsBySchemaId(@Param("datasetId") Long datasetId, @Param("schemaId") Long schemaId);

    Integer selectAccessMask(@Param("datasetId") Long datasetId, @Param("userId") Long userId);

    List<DataHubAcl> selectAclList(Long datasetId);

    int deleteAclByDatasetId(Long datasetId);

    int insertAclList(@Param("entries") List<DataHubAcl> entries);

    int countActiveUser(Long userId);

    int countActiveRole(Long roleId);

    List<Map<String, Object>> selectUserOptions(@Param("keyword") String keyword);

    List<Map<String, Object>> selectRoleOptions(@Param("keyword") String keyword);

    List<Map<String, String>> selectNameDictionary();
}

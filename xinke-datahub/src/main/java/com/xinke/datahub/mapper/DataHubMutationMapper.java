package com.xinke.datahub.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xinke.datahub.domain.DataHubColumn;
import com.xinke.datahub.domain.DataHubDataChange;
import com.xinke.datahub.domain.DataHubDataset;
import com.xinke.datahub.domain.DataHubDataVersion;
import com.xinke.datahub.domain.DataHubImportJob;

@Mapper
public interface DataHubMutationMapper
{
    int insertMutationJob(DataHubImportJob job);

    DataHubImportJob selectMutationJob(String previewId);

    DataHubDataset selectMutationDataset(Long datasetId);

    DataHubDataVersion selectDatasetVersion(@Param("datasetId") Long datasetId, @Param("versionId") Long versionId);

    DataHubDataVersion selectVersionByJobId(Long jobId);

    List<DataHubDataVersion> selectDatasetVersions(Long datasetId);

    List<DataHubColumn> selectSchemaColumns(@Param("datasetId") Long datasetId, @Param("schemaId") Long schemaId);

    int updateMutationPreview(DataHubImportJob job);

    int markMutationPreviewFailed(@Param("jobId") Long jobId, @Param("message") String message,
            @Param("finishTime") Date finishTime);

    int acquireDatasetJob(@Param("datasetId") Long datasetId, @Param("jobId") Long jobId,
            @Param("sourceVersionId") Long sourceVersionId, @Param("sourceLockVersion") Integer sourceLockVersion);

    int queueMutationJob(@Param("jobId") Long jobId, @Param("userId") Long userId,
            @Param("sourceVersionId") Long sourceVersionId, @Param("sourceLockVersion") Integer sourceLockVersion,
            @Param("sheetName") String sheetName, @Param("schemaJson") String schemaJson);

    int releaseDatasetJob(@Param("datasetId") Long datasetId, @Param("jobId") Long jobId);

    int selectNextVersionNo(Long datasetId);

    int insertMutationVersion(DataHubDataVersion version);

    int archiveVersion(@Param("versionId") Long versionId, @Param("retentionUntil") Date retentionUntil);

    int activateVersion(Long versionId);

    int publishNewVersion(@Param("datasetId") Long datasetId, @Param("jobId") Long jobId,
            @Param("sourceVersionId") Long sourceVersionId, @Param("expectedLockVersion") Integer expectedLockVersion,
            @Param("targetVersionId") Long targetVersionId, @Param("schemaId") Long schemaId,
            @Param("rowCount") Long rowCount, @Param("username") String username);

    int publishRollback(@Param("datasetId") Long datasetId, @Param("jobId") Long jobId,
            @Param("sourceVersionId") Long sourceVersionId, @Param("expectedLockVersion") Integer expectedLockVersion,
            @Param("targetVersionId") Long targetVersionId, @Param("schemaId") Long schemaId,
            @Param("rowCount") Long rowCount, @Param("username") String username);

    int completeMutationJob(@Param("jobId") Long jobId, @Param("lockVersion") Integer lockVersion,
            @Param("targetVersionId") Long targetVersionId, @Param("processedRows") Long processedRows,
            @Param("finishTime") Date finishTime);

    int insertDataChanges(@Param("changes") List<DataHubDataChange> changes);

    int reconcileCompletedJob(@Param("jobId") Long jobId, @Param("targetVersionId") Long targetVersionId,
            @Param("processedRows") Long processedRows, @Param("finishTime") Date finishTime);

    List<DataHubDataVersion> selectPurgeCandidates(@Param("limit") int limit);

    List<DataHubDataVersion> selectStalePurgingVersions(@Param("staleBefore") Date staleBefore,
            @Param("limit") int limit);

    int claimVersionForPurge(@Param("versionId") Long versionId);

    int reclaimStaleVersionForPurge(@Param("versionId") Long versionId,
            @Param("staleBefore") Date staleBefore);

    int markVersionPurged(Long versionId);

}

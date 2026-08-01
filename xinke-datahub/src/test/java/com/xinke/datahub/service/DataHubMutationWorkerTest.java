package com.xinke.datahub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinke.datahub.config.DataHubProperties;
import com.xinke.datahub.domain.DataHubDataset;
import com.xinke.datahub.domain.DataHubDataVersion;
import com.xinke.datahub.domain.DataHubImportJob;
import com.xinke.datahub.domain.dto.DataHubColumnDefinition;
import com.xinke.datahub.domain.dto.DataHubEditCellValue;
import com.xinke.datahub.mapper.DataHubMapper;
import com.xinke.datahub.mapper.DataHubMutationMapper;
import com.xinke.datahub.naming.DataHubIdentifierService;
import com.xinke.datahub.parser.SpreadsheetParser;
import com.xinke.datahub.storage.DataHubStorageService;

@ExtendWith(MockitoExtension.class)
class DataHubMutationWorkerTest
{
    @Mock private DataHubMapper mapper;
    @Mock private DataHubMutationMapper mutationMapper;
    @Mock private DataHubImportWorker importWorker;
    @Mock private DataHubDynamicTableService tableService;
    @Mock private SpreadsheetParser parser;
    @Mock private DataHubStorageService storageService;
    @Mock private DataHubDefinitionValidator validator;
    @Mock private PlatformTransactionManager transactionManager;

    private DataHubMutationWorker worker;
    private DataHubImportJob job;
    private DataHubDataset dataset;
    private DataHubDataVersion source;

    @BeforeEach
    void setUp()
    {
        DataHubProperties properties = new DataHubProperties();
        worker = new DataHubMutationWorker(mapper, mutationMapper, importWorker, tableService,
                new DataHubIdentifierService(properties), parser, storageService, validator,
                properties, new ObjectMapper(), transactionManager);

        job = new DataHubImportJob();
        job.setJobId(11L);
        job.setPreviewId("preview");
        job.setDatasetId(7L);
        job.setOperationType("CLEAR");
        job.setSourceVersionId(31L);
        job.setSourceLockVersion(4);
        job.setStatus("VALIDATING");
        job.setLockVersion(2);
        job.setUploadUserName("tester");
        job.setTotalRows(0L);

        dataset = new DataHubDataset();
        dataset.setDatasetId(7L);
        dataset.setDatasetCode("orders");
        dataset.setCurrentVersionId(31L);
        dataset.setCurrentSchemaId(21L);
        dataset.setActiveJobId(11L);
        dataset.setLockVersion(5);

        source = new DataHubDataVersion();
        source.setVersionId(31L);
        source.setDatasetId(7L);
        source.setSchemaId(21L);
        source.setPhysicalTableName("dh_data_orders_source");
        source.setRowCount(10L);
        source.setStatus("ACTIVE");

    }

    @Test
    void publishesClearAsANewImmutableVersion()
    {
        stubExecution();
        when(mutationMapper.publishNewVersion(7L, 11L, 31L, 5, 41L, 21L, 0L, "tester"))
                .thenReturn(1);
        when(mutationMapper.completeMutationJob(anyLong(), anyInt(), anyLong(), anyLong(), any())).thenReturn(1);

        worker.execute("preview", 2);

        ArgumentCaptor<DataHubDataVersion> version = ArgumentCaptor.forClass(DataHubDataVersion.class);
        verify(mutationMapper).insertMutationVersion(version.capture());
        assertEquals("CLEAR", version.getValue().getVersionType());
        assertEquals(31L, version.getValue().getParentVersionId());
        assertEquals(0L, version.getValue().getRowCount());
        verify(mutationMapper).completeMutationJob(eq(11L), eq(2), eq(41L), eq(0L), any());
        verify(mutationMapper, never()).releaseDatasetJob(anyLong(), anyLong());
    }

    @Test
    void releasesDatasetAndDropsUnownedTargetWhenPublishCasFails()
    {
        stubExecution();
        when(mutationMapper.publishNewVersion(7L, 11L, 31L, 5, 41L, 21L, 0L, "tester"))
                .thenReturn(0);
        when(mapper.markImportJobFailed(any())).thenReturn(1);
        when(mutationMapper.releaseDatasetJob(7L, 11L)).thenReturn(1);
        when(mapper.countVersionByPhysicalTable(anyString())).thenReturn(0);

        worker.execute("preview", 2);

        verify(mutationMapper).releaseDatasetJob(7L, 11L);
        verify(mutationMapper, never()).completeMutationJob(anyLong(), anyInt(), anyLong(), anyLong(), any());
        verify(tableService, atLeastOnce()).dropIfExists(anyString());
    }

    @Test
    void editKeepsTextBlanksUnlessNullIsExplicit()
    {
        DataHubColumnDefinition text = new DataHubColumnDefinition();
        text.setDataType("TEXT");
        text.setNullable(true);

        assertEquals("", worker.convertEditValue(text, editValue("", false)));
        assertEquals("  ", worker.convertEditValue(text, editValue("  ", false)));

        when(importWorker.convert(text, null)).thenReturn(null);
        assertEquals(null, worker.convertEditValue(text, editValue("ignored", true)));
    }

    @Test
    void invalidOperationFailsTheJobAndReleasesItsDatasetLock()
    {
        job.setOperationType("BROKEN");
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(mapper.selectJobByPreviewId("preview")).thenReturn(job);
        when(mapper.markImportJobFailed(any())).thenReturn(1);
        when(mutationMapper.releaseDatasetJob(7L, 11L)).thenReturn(1);

        worker.execute("preview", 2);

        verify(mapper).markImportJobFailed(any());
        verify(mutationMapper).releaseDatasetJob(7L, 11L);
        verify(tableService, never()).createTableLike(anyString(), anyString());
    }

    private DataHubEditCellValue editValue(String value, boolean isNull)
    {
        DataHubEditCellValue cell = new DataHubEditCellValue();
        cell.setValue(value);
        cell.setIsNull(isNull);
        return cell;
    }

    private void stubExecution()
    {
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(mapper.selectJobByPreviewId("preview")).thenReturn(job);
        when(mutationMapper.selectMutationDataset(7L)).thenReturn(dataset);
        when(mutationMapper.selectDatasetVersion(7L, 31L)).thenReturn(source);
        when(mapper.updateImportJob(any())).thenReturn(1);
        when(mutationMapper.selectNextVersionNo(7L)).thenReturn(2);
        when(mapper.countClaimedImportJob(11L, 2, "COMMITTING")).thenReturn(1);
        when(mutationMapper.archiveVersion(anyLong(), any())).thenReturn(1);
        when(mutationMapper.insertMutationVersion(any())).thenAnswer(invocation -> {
            DataHubDataVersion version = invocation.getArgument(0);
            version.setVersionId(41L);
            return 1;
        });
    }
}

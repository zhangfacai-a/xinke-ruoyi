package com.xinke.erp.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.lenient;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.domain.audience.AudienceCommentRankItem;
import com.xinke.erp.domain.audience.AudienceRankBatch;
import com.xinke.erp.domain.audience.AudienceRankImportRequest;
import com.xinke.erp.domain.audience.AudienceRankImportResult;
import com.xinke.erp.domain.audience.AudienceRankSnapshot;
import com.xinke.erp.domain.audience.AudienceRankRoomMatch;
import com.xinke.erp.domain.audience.AudienceWatchRankItem;
import com.xinke.erp.domain.audience.AudienceFollowup;
import com.xinke.erp.domain.audience.AudienceFollowupQuery;
import com.xinke.erp.domain.audience.AudienceAssignmentRule;
import com.xinke.erp.domain.audience.AudienceOpportunity;
import com.xinke.erp.domain.audience.AudienceCustomerOrder;
import com.xinke.erp.mapper.AudienceRankMapper;

@ExtendWith(MockitoExtension.class)
class AudienceRankServiceImplTest
{
    @Mock
    private AudienceRankMapper mapper;

    private AudienceRankServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new AudienceRankServiceImpl(mapper);
        lenient().when(mapper.selectMatchingRooms(any())).thenReturn(List.of());
        lenient().doAnswer(invocation -> {
            AudienceRankRoomMatch room = invocation.getArgument(0);
            room.setRoomId(30L);
            return 1;
        }).when(mapper).insertAutoRoom(any(AudienceRankRoomMatch.class));
        lenient().doAnswer(invocation -> {
            AudienceRankBatch batch = invocation.getArgument(0);
            batch.setBatchId(100L);
            return 1;
        }).when(mapper).insertBatch(any(AudienceRankBatch.class));
        lenient().doAnswer(invocation -> ((List<?>) invocation.getArgument(0)).size())
                .when(mapper).insertSnapshots(anyList());
        lenient().doAnswer(invocation -> ((List<?>) invocation.getArgument(0)).size())
                .when(mapper).upsertProfiles(anyList());
    }

    @Test
    void reactivatesEndedCustomerAsNewOpportunityWithoutDeletingHistory()
    {
        AudienceFollowup before = followup(10L, "ORDERED", 2L, null);
        before.setOrderNo("ORDER-OLD");
        before.setReactivationPending(true);
        before.setVersion(3);
        AudienceFollowup after = followup(10L, "UNASSIGNED", null, null);
        after.setVersion(4);
        AudienceOpportunity old = new AudienceOpportunity();
        old.setOpportunityId(9L);
        old.setSequenceNo(1);
        when(mapper.selectFollowupById(10L)).thenReturn(before, after);
        when(mapper.selectOpportunities(10L)).thenReturn(List.of(old), List.of(old));
        when(mapper.selectCustomerOrders(10L)).thenReturn(List.of(), List.of());
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        AudienceFollowup result = service.reactivateFollowup(10L, 2L, "领取人甲");

        assertEquals("UNASSIGNED", result.getStatus());
        verify(mapper).closeCurrentOpportunities(10L);
        ArgumentCaptor<AudienceOpportunity> opportunity = ArgumentCaptor.forClass(AudienceOpportunity.class);
        verify(mapper).insertOpportunity(opportunity.capture());
        assertEquals(2, opportunity.getValue().getSequenceNo());
        assertEquals(true, opportunity.getValue().getCurrent());
        verify(mapper).insertFollowupLog(any());
    }

    @Test
    void addsSecondOrderAndKeepsExistingCustomerIdentity()
    {
        AudienceFollowup before = followup(10L, "CONTACTED", 2L, null);
        before.setVersion(1);
        AudienceFollowup after = followup(10L, "ORDERED", 2L, null);
        after.setOrderNo("ORDER-NEW");
        after.setVersion(2);
        AudienceOpportunity current = new AudienceOpportunity();
        current.setOpportunityId(8L);
        current.setCurrent(true);
        when(mapper.selectFollowupById(10L)).thenReturn(before, after, after);
        when(mapper.selectOpportunities(10L)).thenReturn(List.of(current), List.of(current), List.of(current));
        when(mapper.selectCustomerOrders(10L)).thenReturn(List.of(), List.of(), List.of());
        when(mapper.selectCurrentOpportunity(10L)).thenReturn(current);
        when(mapper.selectCustomerOrderByNo("ORDER-NEW")).thenReturn(null, order(10L, "ORDER-NEW"));
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        AudienceCustomerOrder input = new AudienceCustomerOrder();
        input.setOrderNo("ORDER-NEW");
        AudienceCustomerOrder result = service.saveCustomerOrder(10L, input, 1L, "管理员");

        assertEquals("ORDER-NEW", result.getOrderNo());
        assertEquals(10L, result.getFollowupId());
        verify(mapper).insertCustomerOrder(any(AudienceCustomerOrder.class));
        verify(mapper).insertFollowupLog(any());
    }

    @Test
    void mergesBothRankingsBySecUidAndKeepsNullForMissingRanking()
    {
        AudienceRankImportRequest request = request();
        AudienceCommentRankItem commentOnly = comment(1, "sec-comment", "评论用户", 7L);
        AudienceCommentRankItem bothComment = comment(2, "sec-both", "共同用户", 2L);
        AudienceWatchRankItem bothWatch = watch(1, "sec-both", "共同用户", 900L);
        AudienceWatchRankItem watchOnly = watch(2, "sec-watch", "观看用户", 120L);
        request.setCommentRanks(List.of(commentOnly, bothComment));
        request.setWatchRanks(List.of(bothWatch, watchOnly));

        service.importRanks(request, "127.0.0.1");

        ArgumentCaptor<List<AudienceRankSnapshot>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).insertSnapshots(captor.capture());
        List<AudienceRankSnapshot> rows = captor.getValue();
        assertEquals(3, rows.size());
        AudienceRankSnapshot commentRow = rows.stream().filter(row -> "sec-comment".equals(row.getSecUid())).findFirst().orElseThrow();
        AudienceRankSnapshot bothRow = rows.stream().filter(row -> "sec-both".equals(row.getSecUid())).findFirst().orElseThrow();
        assertEquals(7L, commentRow.getCommentCount());
        assertEquals(null, commentRow.getWatchSeconds());
        assertEquals(2L, bothRow.getCommentCount());
        assertEquals(900L, bothRow.getWatchSeconds());
    }

    @Test
    void rejectsDuplicateSecUidInsideOneRankingBeforeWriting()
    {
        AudienceRankImportRequest request = request();
        request.setCommentRanks(List.of(comment(1, "same", "甲", 1L), comment(2, "same", "乙", 2L)));

        assertThrows(ServiceException.class, () -> service.importRanks(request, null));
        verify(mapper, never()).insertBatch(any(AudienceRankBatch.class));
        verify(mapper, never()).insertSnapshots(anyList());
    }

    @Test
    void rejectsDuplicateRankInsideOneRanking()
    {
        AudienceRankImportRequest request = request();
        request.setWatchRanks(List.of(watch(1, "a", "甲", 1L), watch(1, "b", "乙", 2L)));

        assertThrows(ServiceException.class, () -> service.importRanks(request, null));
        verify(mapper, never()).insertBatch(any(AudienceRankBatch.class));
    }

    @Test
    void repeatedPayloadIsReturnedAsDuplicateWithoutWritingRows()
    {
        AudienceRankImportRequest request = request();
        AudienceRankBatch existing = new AudienceRankBatch();
        existing.setBatchId(9L);
        existing.setPayloadHash("already");
        existing.setRoomName("测试直播间");
        existing.setCommentRowCount(1);
        existing.setWatchRowCount(1);
        existing.setUniqueUserCount(1);
        existing.setRoomMatchStatus("UNMATCHED");
        when(mapper.selectBatchByHash(any())).thenReturn(null, existing);

        AudienceRankImportResult first = service.importRanks(request, null);
        AudienceRankImportResult second = service.importRanks(request, null);

        assertEquals(false, first.isDuplicate());
        assertEquals(true, second.isDuplicate());
        assertEquals(9L, second.getBatchId());
        verify(mapper).insertBatch(any(AudienceRankBatch.class));
        verify(mapper).insertSnapshots(anyList());
    }

    @Test
    void inputOrderDoesNotChangePayloadFingerprint()
    {
        AudienceRankImportRequest first = request();
        first.setCommentRanks(List.of(comment(2, "b", "乙", 2L), comment(1, "a", "甲", 1L)));
        first.setWatchRanks(List.of(watch(2, "b", "乙", 20L), watch(1, "a", "甲", 10L)));
        AudienceRankImportRequest second = request();
        second.setCommentRanks(List.of(comment(1, "a", "甲", 1L), comment(2, "b", "乙", 2L)));
        second.setWatchRanks(List.of(watch(1, "a", "甲", 10L), watch(2, "b", "乙", 20L)));
        AudienceRankBatch existing = existingBatch();
        String[] firstHash = new String[1];
        when(mapper.selectBatchByHash(any())).thenAnswer(invocation -> {
            String hash = invocation.getArgument(0);
            if (firstHash[0] == null)
            {
                firstHash[0] = hash;
                return null;
            }
            existing.setPayloadHash(hash);
            return existing;
        });

        AudienceRankImportResult firstResult = service.importRanks(first, null);
        AudienceRankImportResult secondResult = service.importRanks(second, null);

        assertNotNull(firstResult.getPayloadHash());
        assertEquals(firstResult.getPayloadHash(), secondResult.getPayloadHash());
        assertEquals(true, secondResult.isDuplicate());
    }

    @Test
    void allowsEmptyOneRankingButRequiresAtLeastOneRow()
    {
        AudienceRankImportRequest request = request();
        request.setCommentRanks(List.of());
        request.setWatchRanks(List.of(watch(1, "watch", "观看用户", 20L)));

        AudienceRankImportResult result = service.importRanks(request, null);

        assertEquals(0, result.getCommentRowCount());
        assertEquals(1, result.getWatchRowCount());
        AudienceRankImportRequest empty = request();
        empty.setCommentRanks(List.of());
        empty.setWatchRanks(List.of());
        assertThrows(ServiceException.class, () -> service.importRanks(empty, null));
    }

    @Test
    void stopsBeforeProfileUpdateWhenSnapshotWriteIsIncomplete()
    {
        AudienceRankImportRequest request = request();
        org.mockito.Mockito.reset(mapper);
        when(mapper.selectMatchingRooms(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            ((AudienceRankRoomMatch) invocation.getArgument(0)).setRoomId(30L);
            return 1;
        }).when(mapper).insertAutoRoom(any(AudienceRankRoomMatch.class));
        doAnswer(invocation -> {
            ((AudienceRankBatch) invocation.getArgument(0)).setBatchId(100L);
            return 1;
        }).when(mapper).insertBatch(any(AudienceRankBatch.class));
        when(mapper.insertSnapshots(anyList())).thenReturn(1);

        assertThrows(ServiceException.class, () -> service.importRanks(request, null));

        verify(mapper, never()).upsertProfiles(anyList());
    }

    @Test
    void createsAndAssignsRoomWhenUploadedNameDoesNotExist()
    {
        AudienceRankImportResult result = service.importRanks(request(), null);

        ArgumentCaptor<AudienceRankRoomMatch> roomCaptor = ArgumentCaptor.forClass(AudienceRankRoomMatch.class);
        verify(mapper).insertAutoRoom(roomCaptor.capture());
        assertEquals("测试直播间", roomCaptor.getValue().getRoomName());
        assertTrue(roomCaptor.getValue().getRoomCode().startsWith("DY-"));
        assertEquals("MATCHED", result.getRoomMatchStatus());
        assertEquals(30L, result.getRoomId());
    }

    @Test
    void claimsUnassignedFollowupAsIndependentOwner()
    {
        AudienceFollowup before = followup(10L, "UNASSIGNED", null, null);
        AudienceFollowup after = followup(10L, "PENDING", null, 2L);
        after.setOwnerUserId(2L);
        after.setOwnerNameSnapshot("领取人甲");
        after.setVersion(1);
        when(mapper.selectFollowupById(10L)).thenReturn(before, after);
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        service.claimFollowup(10L, 2L, "领取人甲");

        ArgumentCaptor<AudienceFollowup> captor = ArgumentCaptor.forClass(AudienceFollowup.class);
        verify(mapper).updateFollowup(captor.capture());
        assertEquals("PENDING", captor.getValue().getStatus());
        assertEquals(2L, captor.getValue().getOwnerUserId());
        assertEquals("领取人甲", captor.getValue().getOwnerNameSnapshot());
        assertNull(captor.getValue().getControllerUserId());
        assertNull(captor.getValue().getAnchorUserId());
        assertNotNull(captor.getValue().getNextFollowAt());
        verify(mapper).insertFollowupLog(any());
    }

    @Test
    void nonQualifyingUploadKeepsAudienceInObservationPool()
    {
        AudienceRankImportRequest request = request();
        AudienceCommentRankItem comment = comment(80, "quiet-user", "普通观众", 1L);
        AudienceWatchRankItem watch = watch(90, "quiet-user", "普通观众", 20L);
        request.setCommentRanks(List.of(comment));
        request.setWatchRanks(List.of(watch));

        service.importRanks(request, null);

        ArgumentCaptor<List<AudienceFollowup>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).insertFollowups(captor.capture());
        AudienceFollowup created = captor.getValue().get(0);
        assertEquals("OBSERVING", created.getStatus());
        assertNull(created.getQualificationReason());
        assertNull(created.getQualifiedAt());
    }

    @Test
    void qualifyingUploadCreatesClaimableCustomer()
    {
        AudienceRankImportRequest request = request();
        request.setCommentRanks(List.of(comment(8, "qualified-user", "重点观众", 12L)));
        request.setWatchRanks(List.of(watch(66, "qualified-user", "重点观众", 20L)));

        service.importRanks(request, null);

        ArgumentCaptor<List<AudienceFollowup>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).insertFollowups(captor.capture());
        AudienceFollowup created = captor.getValue().get(0);
        assertEquals("UNASSIGNED", created.getStatus());
        assertTrue(created.getQualificationReason().contains("评论榜前30名"));
        assertNotNull(created.getQualifiedAt());
    }

    @Test
    void repeatedVisitPromotesObservedAudienceMember()
    {
        AudienceRankImportRequest request = request();
        request.setCommentRanks(List.of(comment(80, "repeat-user", "重复到访", 1L)));
        request.setWatchRanks(List.of(watch(90, "repeat-user", "重复到访", 20L)));
        AudienceFollowup observing = followup(10L, "OBSERVING", null, null);
        observing.setRoomId(30L);
        observing.setSecUid("repeat-user");
        AudienceFollowup promoted = followup(10L, "UNASSIGNED", null, null);
        promoted.setRoomId(30L);
        promoted.setSecUid("repeat-user");
        AudienceFollowup stats = new AudienceFollowup();
        stats.setSecUid("repeat-user");
        stats.setAppearanceDays(2);
        when(mapper.selectFollowupByUid("repeat-user")).thenReturn(observing, observing, promoted);
        when(mapper.selectFollowupVisitStats(anyList())).thenReturn(List.of(stats));
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        service.importRanks(request, null);

        ArgumentCaptor<AudienceFollowup> updates = ArgumentCaptor.forClass(AudienceFollowup.class);
        verify(mapper, times(2)).updateFollowup(updates.capture());
        AudienceFollowup qualificationUpdate = updates.getAllValues().stream()
                .filter(value -> value.getQualificationReason() != null).findFirst().orElseThrow();
        assertEquals("UNASSIGNED", qualificationUpdate.getStatus());
        assertTrue(qualificationUpdate.getQualificationReason().contains("累计到访2天"));
        verify(mapper).insertFollowupLog(any());
    }

    @Test
    void claimingObservedAudiencePromotesAndQualifiesIt()
    {
        AudienceFollowup before = followup(10L, "OBSERVING", null, null);
        AudienceFollowup after = followup(10L, "PENDING", null, null);
        after.setOwnerUserId(2L);
        when(mapper.selectFollowupById(10L)).thenReturn(before, after);
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        service.claimFollowup(10L, 2L, "领取人甲");

        ArgumentCaptor<AudienceFollowup> captor = ArgumentCaptor.forClass(AudienceFollowup.class);
        verify(mapper).updateFollowup(captor.capture());
        assertEquals("PENDING", captor.getValue().getStatus());
        assertEquals("人工加入跟进", captor.getValue().getQualificationReason());
        assertNotNull(captor.getValue().getQualifiedAt());
    }

    @Test
    void concurrentClaimConflictDoesNotOverwriteExistingOwner()
    {
        AudienceFollowup before = followup(10L, "UNASSIGNED", null, null);
        when(mapper.selectFollowupById(10L)).thenReturn(before);
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(0);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.claimFollowup(10L, 2L, "领取人甲"));

        assertTrue(error.getMessage().contains("领取失败"));
        verify(mapper, never()).insertFollowupLog(any());
    }

    @Test
    void savingQualificationRuleRechecksObservedCustomers()
    {
        AudienceAssignmentRule input = new AudienceAssignmentRule();
        input.setRoomId(20L);
        input.setEnabled(false);
        input.setMemberUserIds(List.of());
        input.setQualificationEnabled(true);
        input.setCommentRankThreshold(30);
        input.setWatchRankThreshold(30);
        input.setMinPayLevel(10);
        input.setMinVisitDays(2);
        AudienceAssignmentRule saved = new AudienceAssignmentRule();
        saved.setRuleId(7L);
        saved.setRoomId(20L);
        saved.setEnabled(false);
        saved.setQualificationEnabled(true);
        saved.setCommentRankThreshold(30);
        saved.setWatchRankThreshold(30);
        saved.setMinPayLevel(10);
        saved.setMinVisitDays(2);
        AudienceFollowup observing = followup(10L, "OBSERVING", null, null);
        observing.setCommentRank(80);
        observing.setWatchRank(90);
        AudienceFollowup stats = new AudienceFollowup();
        stats.setSecUid(observing.getSecUid());
        stats.setAppearanceDays(2);
        when(mapper.selectFollowupRoomOptions()).thenReturn(List.of(Map.of("roomId", 20L)));
        when(mapper.selectAllActiveUsers()).thenReturn(List.of());
        when(mapper.selectAssignmentRuleByRoomId(20L)).thenReturn(null, saved);
        when(mapper.selectFollowupList(any(AudienceFollowupQuery.class))).thenReturn(List.of(observing));
        when(mapper.selectFollowupVisitStats(anyList())).thenReturn(List.of(stats));
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);
        when(mapper.selectAssignmentRules()).thenReturn(List.of(saved));
        when(mapper.selectAssignmentRuleMembers(7L)).thenReturn(List.of());

        service.saveAssignmentRule(input, 1L, "管理员");

        ArgumentCaptor<AudienceFollowup> captor = ArgumentCaptor.forClass(AudienceFollowup.class);
        verify(mapper).updateFollowup(captor.capture());
        assertEquals("UNASSIGNED", captor.getValue().getStatus());
        assertTrue(captor.getValue().getQualificationReason().contains("累计到访2天"));
    }

    @Test
    void enrichesCustomerWithGlobalVisitStatistics()
    {
        AudienceFollowup row = followup(10L, "PENDING", 2L, null);
        AudienceFollowup stats = new AudienceFollowup();
        stats.setSecUid(row.getSecUid());
        stats.setAppearanceDays(4);
        stats.setBestCommentRank(2);
        stats.setBestWatchRank(6);
        stats.setVisitDatesCsv("2026-08-24,2026-08-23,2026-08-21,2026-08-18");
        when(mapper.selectFollowupList(any(AudienceFollowupQuery.class))).thenReturn(List.of(row));
        when(mapper.selectFollowupVisitStats(anyList())).thenReturn(List.of(stats));

        List<AudienceFollowup> result = service.selectFollowupList(new AudienceFollowupQuery(), 1L);

        assertEquals(4, result.get(0).getAppearanceDays());
        assertEquals(2, result.get(0).getConsecutiveDays());
        assertEquals(2, result.get(0).getBestCommentRank());
        assertEquals(6, result.get(0).getBestWatchRank());
    }

    @Test
    void allowsAnyAuthorizedUserToClaimWithoutRoomRole()
    {
        AudienceFollowup before = followup(10L, "UNASSIGNED", null, null);
        AudienceFollowup after = followup(10L, "PENDING", null, null);
        after.setOwnerUserId(2L);
        when(mapper.selectFollowupById(10L)).thenReturn(before, after);
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        service.claimFollowup(10L, 2L, "普通客服");

        ArgumentCaptor<AudienceFollowup> captor = ArgumentCaptor.forClass(AudienceFollowup.class);
        verify(mapper).updateFollowup(captor.capture());
        assertEquals(2L, captor.getValue().getOwnerUserId());
    }

    @Test
    void batchClaimActuallyClaimsEachUnassignedRecord()
    {
        AudienceFollowup before = followup(10L, "UNASSIGNED", null, null);
        before.setRoomId(null);
        AudienceFollowup after = followup(10L, "PENDING", 1L, null);
        after.setRoomId(null);
        after.setVersion(1);
        when(mapper.selectFollowupById(10L)).thenReturn(before, before, after, after);
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        service.batchUpdateFollowups(List.of(10L), Map.of("claim", true), 1L, "管理员");

        ArgumentCaptor<AudienceFollowup> captor = ArgumentCaptor.forClass(AudienceFollowup.class);
        verify(mapper, times(1)).updateFollowup(captor.capture());
        assertEquals(1L, captor.getValue().getOwnerUserId());
        assertEquals("PENDING", captor.getValue().getStatus());
        verify(mapper).insertFollowupLog(any());
    }

    @Test
    void rejectsOrderedStatusWithoutOrderNumber()
    {
        AudienceFollowup before = followup(10L, "PENDING", 2L, null);
        when(mapper.selectFollowupById(10L)).thenReturn(before);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.updateFollowupStatus(10L, "ORDERED", "客户已下单", null, 2L, "主播甲"));

        assertTrue(exception.getMessage().contains("订单号"));
        verify(mapper, never()).updateFollowup(any(AudienceFollowup.class));
    }

    @Test
    void rejectsInvalidStatusWithoutCloseReason()
    {
        AudienceFollowup before = followup(10L, "PENDING", 2L, null);
        when(mapper.selectFollowupById(10L)).thenReturn(before);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.updateFollowupStatus(10L, "INVALID", " ", null, 2L, "主播甲"));

        assertTrue(exception.getMessage().contains("关闭原因"));
        verify(mapper, never()).updateFollowup(any(AudienceFollowup.class));
    }

    @Test
    void ordinaryUserCannotReadAnotherOwnersFollowup()
    {
        AudienceFollowup value = followup(10L, "PENDING", 3L, null);
        when(mapper.selectFollowupById(10L)).thenReturn(value);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.selectFollowup(10L, 2L));

        assertTrue(exception.getMessage().contains("无权查看"));
    }

    @Test
    void todayDueFlagIsPassedToMapper()
    {
        AudienceFollowupQuery query = new AudienceFollowupQuery();
        query.setTodayDue(true);
        when(mapper.selectFollowupList(any(AudienceFollowupQuery.class))).thenReturn(List.of());

        service.selectFollowupList(query, 1L);

        ArgumentCaptor<AudienceFollowupQuery> captor = ArgumentCaptor.forClass(AudienceFollowupQuery.class);
        verify(mapper).selectFollowupList(captor.capture());
        assertEquals(Boolean.TRUE, captor.getValue().getTodayDue());
    }

    @Test
    void quickStatusWithoutReminderKeepsExistingNextFollowTime()
    {
        Date reminder = new Date(1_800_000_000_000L);
        AudienceFollowup before = followup(10L, "PENDING", 2L, null);
        before.setNextFollowAt(reminder);
        AudienceFollowup after = followup(10L, "CONTACTED", 2L, null);
        after.setNextFollowAt(reminder);
        after.setVersion(1);
        when(mapper.selectFollowupById(10L)).thenReturn(before, after);
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        service.updateFollowupStatus(10L, "CONTACTED", "已电话联系", null, 2L, "主播甲");

        ArgumentCaptor<AudienceFollowup> captor = ArgumentCaptor.forClass(AudienceFollowup.class);
        verify(mapper).updateFollowup(captor.capture());
        assertEquals(reminder, captor.getValue().getNextFollowAt());
    }

    @Test
    void closingFollowupClearsExistingReminder()
    {
        Date reminder = new Date(1_800_000_000_000L);
        AudienceFollowup before = followup(10L, "ORDERED", 2L, null);
        before.setOrderNo("ORDER-1001");
        before.setNextFollowAt(reminder);
        AudienceFollowup after = followup(10L, "CLOSED", 2L, null);
        after.setOrderNo("ORDER-1001");
        after.setVersion(1);
        when(mapper.selectFollowupById(10L)).thenReturn(before, after);
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        service.updateFollowupStatus(10L, "CLOSED", "订单已完成", reminder, 2L, "主播甲");

        ArgumentCaptor<AudienceFollowup> captor = ArgumentCaptor.forClass(AudienceFollowup.class);
        verify(mapper).updateFollowup(captor.capture());
        assertNull(captor.getValue().getNextFollowAt());
    }

    @Test
    void editPermissionCannotReassignFollowupWithoutAssignPermission()
    {
        AudienceFollowup before = followup(10L, "PENDING", 2L, null);
        AudienceFollowup update = followup(10L, "PENDING", 3L, null);
        when(mapper.selectFollowupById(10L)).thenReturn(before);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.updateFollowup(update, 2L, "普通跟单人"));

        assertTrue(exception.getMessage().contains("分配权限"));
        verify(mapper, never()).updateFollowup(any(AudienceFollowup.class));
    }

    @Test
    void batchAssigneeFieldsRequireAssignPermission()
    {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.batchUpdateFollowups(List.of(10L), Map.of("controllerUserId", 3L),
                        2L, "普通跟单人"));

        assertTrue(exception.getMessage().contains("分配权限"));
        verify(mapper, never()).selectFollowupById(any());
        verify(mapper, never()).updateFollowup(any(AudienceFollowup.class));
    }

    @Test
    void batchBusinessFieldsRequireEditPermission()
    {
        AudienceRankServiceImpl restrictedService = spy(new AudienceRankServiceImpl(mapper));
        doReturn(true).when(restrictedService).hasAssignPermission(2L);
        doReturn(false).when(restrictedService).hasEditPermission(2L);
        assertTrue(restrictedService.hasAssignPermission(2L));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> restrictedService.batchUpdateFollowups(List.of(10L), Map.of("status", "CONTACTED"),
                        2L, "分配管理员"));

        assertTrue(exception.getMessage().contains("编辑权限"));
        verify(mapper, never()).selectFollowupById(any());
    }

    @Test
    void assigningAnchorDoesNotClaimFollowup()
    {
        AudienceFollowup before = followup(10L, "UNASSIGNED", null, null);
        AudienceFollowup after = followup(10L, "PENDING", 3L, null);
        when(mapper.selectFollowupById(10L)).thenReturn(before, after);
        when(mapper.selectFollowupAssignees(20L, "anchor")).thenReturn(List.of(
                Map.of("userId", 3L, "userName", "主播乙", "roleCode", "anchor")));
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        service.batchUpdateFollowups(List.of(10L), Map.of("anchorUserId", 3L), 1L, "管理员");

        ArgumentCaptor<AudienceFollowup> captor = ArgumentCaptor.forClass(AudienceFollowup.class);
        verify(mapper).updateFollowup(captor.capture());
        assertEquals("UNASSIGNED", captor.getValue().getStatus());
    }

    @Test
    void clearingAnchorDoesNotChangeOwnerStatus()
    {
        AudienceFollowup before = followup(10L, "PENDING", 3L, null);
        AudienceFollowup after = followup(10L, "UNASSIGNED", null, null);
        when(mapper.selectFollowupById(10L)).thenReturn(before, after);
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        service.batchUpdateFollowups(List.of(10L), Collections.singletonMap("anchorUserId", null),
                1L, "管理员");

        ArgumentCaptor<AudienceFollowup> captor = ArgumentCaptor.forClass(AudienceFollowup.class);
        verify(mapper).updateFollowup(captor.capture());
        assertEquals("PENDING", captor.getValue().getStatus());
    }

    @Test
    void updateKeepsRoomAndUsesCanonicalAssigneeName()
    {
        AudienceFollowup before = followup(10L, "PENDING", 2L, null);
        before.setAnchorNameSnapshot("原主播");
        AudienceFollowup update = followup(10L, "PENDING", 3L, null);
        update.setOwnerUserId(2L);
        update.setRoomId(999L);
        update.setAnchorNameSnapshot("伪造名称");
        when(mapper.selectFollowupById(10L)).thenReturn(before, before);
        when(mapper.selectFollowupAssignees(20L, "anchor")).thenReturn(List.of(
                Map.of("userId", 3L, "userName", "主播乙", "roleCode", "anchor")));
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        service.updateFollowup(update, 1L, "管理员");

        ArgumentCaptor<AudienceFollowup> captor = ArgumentCaptor.forClass(AudienceFollowup.class);
        verify(mapper).updateFollowup(captor.capture());
        assertEquals(20L, captor.getValue().getRoomId());
        assertEquals("主播乙", captor.getValue().getAnchorNameSnapshot());
    }

    @Test
    void updateMatchesLegacyFollowupRoomByNameBeforeAssigningStaff()
    {
        AudienceFollowup before = followup(10L, "UNASSIGNED", null, null);
        before.setRoomId(null);
        AudienceFollowup update = followup(10L, "UNASSIGNED", 3L, null);
        update.setRoomId(null);
        update.setOwnerUserId(null);
        AudienceFollowup after = followup(10L, "PENDING", 3L, null);
        AudienceRankRoomMatch room = new AudienceRankRoomMatch();
        room.setRoomId(20L);
        room.setRoomName("测试直播间");
        when(mapper.selectFollowupById(10L)).thenReturn(before, after);
        when(mapper.selectMatchingRooms("测试直播间")).thenReturn(List.of(room));
        when(mapper.selectFollowupAssignees(20L, "anchor")).thenReturn(List.of(
                Map.of("userId", 3L, "userName", "主播乙", "roleCode", "anchor")));
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        service.updateFollowup(update, 1L, "管理员");

        ArgumentCaptor<AudienceFollowup> captor = ArgumentCaptor.forClass(AudienceFollowup.class);
        verify(mapper).updateFollowup(captor.capture());
        assertEquals(20L, captor.getValue().getRoomId());
        assertEquals("主播乙", captor.getValue().getAnchorNameSnapshot());
        assertEquals("UNASSIGNED", captor.getValue().getStatus());
    }

    @Test
    void followResultControlsStatusAndCreatesDefaultReminder()
    {
        AudienceFollowup before = followup(10L, "PENDING", 2L, null);
        AudienceFollowup update = followup(10L, "PENDING", 2L, null);
        update.setFollowResultCode("CONSIDERING");
        update.setIntentLevel("HIGH");
        update.setLastFollowResult("客户需要三天考虑");
        AudienceFollowup after = followup(10L, "QUALIFIED", 2L, null);
        after.setFollowResultCode("CONSIDERING");
        after.setIntentLevel("HIGH");
        after.setVersion(1);
        when(mapper.selectFollowupById(10L)).thenReturn(before, after);
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        service.updateFollowup(update, 2L, "领取人2");

        ArgumentCaptor<AudienceFollowup> captor = ArgumentCaptor.forClass(AudienceFollowup.class);
        verify(mapper).updateFollowup(captor.capture());
        assertEquals("QUALIFIED", captor.getValue().getStatus());
        assertEquals("HIGH", captor.getValue().getIntentLevel());
        assertNotNull(captor.getValue().getLastContactAt());
        assertNotNull(captor.getValue().getNextFollowAt());
    }

    @Test
    void orderedResultClearsReminder()
    {
        AudienceFollowup before = followup(10L, "PENDING", 2L, null);
        before.setNextFollowAt(new Date(System.currentTimeMillis() + 86_400_000L));
        AudienceFollowup update = followup(10L, "PENDING", 2L, null);
        update.setFollowResultCode("ORDERED");
        update.setOrderNo("ORDER-1001");
        update.setNextFollowAt(before.getNextFollowAt());
        AudienceFollowup after = followup(10L, "ORDERED", 2L, null);
        after.setOrderNo("ORDER-1001");
        after.setVersion(1);
        when(mapper.selectFollowupById(10L)).thenReturn(before, after);
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        service.updateFollowup(update, 2L, "领取人2");

        ArgumentCaptor<AudienceFollowup> captor = ArgumentCaptor.forClass(AudienceFollowup.class);
        verify(mapper).updateFollowup(captor.capture());
        assertEquals("ORDERED", captor.getValue().getStatus());
        assertNull(captor.getValue().getNextFollowAt());
    }

    @Test
    void duplicateOrderNumberIsRejectedBeforeSaving()
    {
        AudienceFollowup before = followup(10L, "PENDING", 2L, null);
        AudienceFollowup update = followup(10L, "PENDING", 2L, null);
        update.setFollowResultCode("ORDERED");
        update.setOrderNo("ORDER-1001");
        AudienceFollowup conflict = followup(11L, "ORDERED", 3L, null);
        conflict.setNicknameSnapshot("另一位客户");
        conflict.setOrderNo("ORDER-1001");
        when(mapper.selectFollowupById(10L)).thenReturn(before);
        when(mapper.selectFollowupByOrderNo("ORDER-1001", 10L)).thenReturn(conflict);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.updateFollowup(update, 2L, "领取人2"));

        assertTrue(error.getMessage().contains("另一位客户"));
        verify(mapper, never()).updateFollowup(any(AudienceFollowup.class));
    }

    @Test
    void unchangedLegacyOrderNumberDoesNotBlockSaving()
    {
        AudienceFollowup before = followup(10L, "ORDERED", 2L, null);
        before.setOrderNo("ORDER-1001");
        AudienceFollowup update = followup(10L, "ORDERED", 2L, null);
        update.setFollowResultCode("ORDERED");
        update.setOrderNo("ORDER-1001");
        AudienceFollowup after = followup(10L, "ORDERED", 2L, null);
        after.setOrderNo("ORDER-1001");
        when(mapper.selectFollowupById(10L)).thenReturn(before, after);
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        service.updateFollowup(update, 2L, "领取人2");

        verify(mapper, never()).selectFollowupByOrderNo(any(), any());
        verify(mapper).updateFollowup(any(AudienceFollowup.class));
    }

    @Test
    void teamDashboardUsesDefaultThirtyDayCohort()
    {
        when(mapper.selectTeamOverview(any())).thenReturn(Map.of("totalCustomers", 12L));
        when(mapper.selectTeamFunnel(any())).thenReturn(List.of(Map.of("stageCode", "NEW", "totalCount", 12L)));
        when(mapper.selectOwnerPerformance(any())).thenReturn(List.of());
        when(mapper.selectRoomPerformance(any())).thenReturn(List.of());
        when(mapper.selectDailyTrend(any())).thenReturn(List.of());
        AudienceFollowupQuery query = new AudienceFollowupQuery();

        Map<String, Object> result = service.selectTeamDashboard(query);

        assertNotNull(query.getBeginDate());
        assertNotNull(query.getEndDate());
        assertEquals(29L, java.time.temporal.ChronoUnit.DAYS.between(query.getBeginDate(), query.getEndDate()));
        assertEquals(12L, ((Map<?, ?>) result.get("overview")).get("totalCustomers"));
    }

    @Test
    void autoAssignUsesRoundRobinAndRespectsCurrentLoad()
    {
        AudienceAssignmentRule rule = new AudienceAssignmentRule();
        rule.setRuleId(7L);
        rule.setRoomId(20L);
        rule.setEnabled(true);
        rule.setMaxActivePerOwner(2);
        rule.setReclaimHours(24);
        rule.setNextMemberIndex(0);
        AudienceFollowup first = followup(10L, "UNASSIGNED", null, null);
        AudienceFollowup second = followup(11L, "UNASSIGNED", null, null);
        when(mapper.selectAssignmentRuleByRoomId(20L)).thenReturn(rule);
        when(mapper.selectAssignmentRuleMembers(7L)).thenReturn(List.of(
                Map.of("userId", 2L, "userName", "客服甲"),
                Map.of("userId", 3L, "userName", "客服乙")));
        when(mapper.selectReclaimableFollowups(20L, 24, 500)).thenReturn(List.of());
        when(mapper.countActiveFollowupsByOwner(2L)).thenReturn(1);
        when(mapper.countActiveFollowupsByOwner(3L)).thenReturn(0);
        when(mapper.selectUnassignedForAutoAssign(20L, 500)).thenReturn(List.of(first, second));
        when(mapper.selectUnassignedForAutoAssign(20L, 501)).thenReturn(List.of());
        when(mapper.updateFollowup(any(AudienceFollowup.class))).thenReturn(1);

        Map<String, Object> result = service.autoAssignFollowups(20L, 1L, "管理员");

        assertEquals(2, result.get("assignedCount"));
        ArgumentCaptor<AudienceFollowup> updates = ArgumentCaptor.forClass(AudienceFollowup.class);
        verify(mapper, times(2)).updateFollowup(updates.capture());
        assertEquals(2L, updates.getAllValues().get(0).getOwnerUserId());
        assertEquals(3L, updates.getAllValues().get(1).getOwnerUserId());
        assertEquals("PENDING", updates.getAllValues().get(0).getStatus());
        verify(mapper).updateAssignmentRuleCursor(7L, 0);
    }

    @Test
    void enabledAssignmentRuleRequiresMembers()
    {
        AudienceAssignmentRule rule = new AudienceAssignmentRule();
        rule.setRoomId(20L);
        rule.setEnabled(true);
        rule.setMemberUserIds(List.of());
        when(mapper.selectFollowupRoomOptions()).thenReturn(List.of(Map.of("roomId", 20L)));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.saveAssignmentRule(rule, 1L, "管理员"));

        assertTrue(error.getMessage().contains("领取人"));
        verify(mapper, never()).upsertAssignmentRule(any());
    }

    private AudienceRankImportRequest request()
    {
        AudienceRankImportRequest request = new AudienceRankImportRequest();
        request.setRoomName("测试直播间");
        request.setCommentDataDate("2026.08.21");
        request.setWatchDataDate("2026-08-22");
        request.setCommentRanks(List.of(comment(1, "comment", "评论用户", 3L)));
        request.setWatchRanks(List.of(watch(1, "watch", "观看用户", 60L)));
        return request;
    }

    private AudienceFollowup followup(Long id, String status, Long anchorUserId, Long controllerUserId)
    {
        AudienceFollowup value = new AudienceFollowup();
        value.setFollowupId(id);
        value.setRoomId(20L);
        value.setRoomNameSnapshot("测试直播间");
        value.setRoomScopeKey("scope");
        value.setSecUid("sec-" + id);
        value.setNicknameSnapshot("观众" + id);
        value.setStatus(status);
        value.setAnchorUserId(anchorUserId);
        value.setControllerUserId(controllerUserId);
        value.setOwnerUserId(anchorUserId);
        value.setOwnerNameSnapshot(anchorUserId == null ? null : "领取人" + anchorUserId);
        value.setPriority(false);
        value.setVersion(0);
        return value;
    }

    private AudienceRankBatch existingBatch()
    {
        AudienceRankBatch batch = new AudienceRankBatch();
        batch.setBatchId(20L);
        batch.setRoomName("测试直播间");
        batch.setCommentRowCount(2);
        batch.setWatchRowCount(2);
        batch.setUniqueUserCount(2);
        batch.setRoomMatchStatus("UNMATCHED");
        return batch;
    }

    private AudienceCustomerOrder order(Long followupId, String orderNo)
    {
        AudienceCustomerOrder order = new AudienceCustomerOrder();
        order.setCustomerOrderId(99L);
        order.setFollowupId(followupId);
        order.setOrderNo(orderNo);
        order.setOrderStatus("ORDERED");
        return order;
    }

    private AudienceCommentRankItem comment(int rank, String secUid, String nickname, long count)
    {
        AudienceCommentRankItem item = new AudienceCommentRankItem();
        item.setRank(rank);
        item.setSecUid(secUid);
        item.setNickname(nickname);
        item.setCommentCount(count);
        item.setIsFollower(Boolean.TRUE);
        item.setPayLevel(2);
        return item;
    }

    private AudienceWatchRankItem watch(int rank, String secUid, String nickname, long seconds)
    {
        AudienceWatchRankItem item = new AudienceWatchRankItem();
        item.setRank(rank);
        item.setSecUid(secUid);
        item.setNickname(nickname);
        item.setWatchSeconds(seconds);
        item.setIsFollowing(Boolean.FALSE);
        item.setPayLevel(1);
        return item;
    }
}

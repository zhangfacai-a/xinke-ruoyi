package com.xinke.erp.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.xinke.common.core.domain.entity.SysUser;
import com.xinke.common.core.domain.model.LoginUser;
import com.xinke.common.core.redis.RedisCache;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.domain.LiveHandoverRequest;
import com.xinke.erp.mapper.LiveMetricsMapper;

class LiveDesktopServiceTest {
    LiveMetricsMapper mapper=mock(LiveMetricsMapper.class);
    LiveHandoverService reports=mock(LiveHandoverService.class);
    RedisCache cache=mock(RedisCache.class);
    LiveDesktopService service=new LiveDesktopService(reports,mapper,cache);
    @BeforeEach void login(){SysUser user=new SysUser();user.setUserId(42L);user.setUserName("operator");LoginUser login=new LoginUser(42L,1L,user,Set.of("live:metrics:report"));SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));}
    @AfterEach void logout(){SecurityContextHolder.clearContext();}
    @Test void roomsAreFilteredOnServer(){when(reports.rooms()).thenReturn(List.of(Map.of("roomId",1L),Map.of("roomId",2L)));when(mapper.countRoomAnchor(2L,42L)).thenReturn(1);assertEquals(List.of(Map.of("roomId",2L)),service.rooms());assertThrows(ServiceException.class,()->service.requireRoom(1L));}
    @Test void nonManagerCannotAssign(){assertThrows(ServiceException.class,()->service.assign(1L,Set.of(),Set.of(42L)));verify(mapper,never()).insertRoomAnchor(any(),any(),any());}
    @Test void staleOrAnotherUsersPreviewCannotSubmit(){when(cache.getCacheObject("anchor:preview:11111111-1111-1111-1111-111111111111")).thenReturn("{\"userId\":99}");assertThrows(ServiceException.class,()->service.commit("11111111-1111-1111-1111-111111111111"));assertThrows(ServiceException.class,()->service.commit("bad"));verify(reports,never()).transitionApp(any(),any(),any(),any());}
    @Test void staleAssignmentUsesOptimisticCheck(){SecurityContextHolder.getContext().getAuthentication();SysUser user=new SysUser();user.setUserId(42L);user.setUserName("operator");LoginUser login=new LoginUser(42L,1L,user,Set.of("live:metrics:manage"));SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));when(mapper.lockRoom(1L)).thenReturn(Map.of("roomId",1L));when(mapper.listRoomAnchors(1L)).thenReturn(List.of(Map.of("userId",7L)));assertThrows(ServiceException.class,()->service.assign(1L,Set.of(),Set.of(42L)));verify(mapper,never()).insertRoomAnchor(any(),any(),any());}
    @Test void staleCaptureAndOutOfRangeRatiosAreRejected(){LiveHandoverRequest r=new LiveHandoverRequest();r.setCapturedAt(System.currentTimeMillis()-600000);assertThrows(ServiceException.class,()->LiveDesktopService.validateFresh(r));r.setCapturedAt(System.currentTimeMillis());r.setInteractionTotal(1L);r.setWatchTotal(1L);r.setExposureWatchRate(BigDecimal.valueOf(101));r.setWatchFollowRate(BigDecimal.ZERO);r.setWatchInteractionRate(BigDecimal.ZERO);r.setWatchDealRate(BigDecimal.ZERO);assertThrows(ServiceException.class,()->LiveDesktopService.validateFresh(r));}
}

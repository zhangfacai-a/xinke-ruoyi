package com.xinke.erp.livecrm.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LiveCrmCenterMapper {
    List<Map<String, Object>> listCustomers(@Param("beginAt") String beginAt,
                                            @Param("endAt") String endAt,
                                            @Param("roomNameKey") String roomNameKey,
                                            @Param("queue") String queue,
                                            @Param("keyword") String keyword,
                                            @Param("stage") String stage,
                                            @Param("commentedOnly") boolean commentedOnly,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    List<Map<String, Object>> listCommentContents(@Param("ids") List<Long> ids);

    Map<String, Object> dashboard(@Param("beginAt") String beginAt,
                                  @Param("endAt") String endAt,
                                  @Param("roomNameKey") String roomNameKey);

    List<Map<String, Object>> ownerRanking(@Param("beginAt") String beginAt,
                                           @Param("endAt") String endAt,
                                           @Param("roomNameKey") String roomNameKey);

    List<Map<String, Object>> roomStats(@Param("beginAt") String beginAt,
                                        @Param("endAt") String endAt,
                                        @Param("roomNameKey") String roomNameKey);

    List<Map<String, Object>> exportCustomers(@Param("beginAt") String beginAt,
                                              @Param("endAt") String endAt,
                                              @Param("roomNameKey") String roomNameKey,
                                              @Param("queue") String queue,
                                              @Param("keyword") String keyword,
                                              @Param("stage") String stage,
                                              @Param("commentedOnly") boolean commentedOnly,
                                              @Param("limit") int limit);
}

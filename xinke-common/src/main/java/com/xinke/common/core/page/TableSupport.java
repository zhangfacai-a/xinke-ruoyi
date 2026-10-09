package com.xinke.common.core.page;

import com.xinke.common.core.text.Convert;
import com.xinke.common.utils.ServletUtils;

/**
 * 表格数据处理
 * 
 * @author xinke
 */
public class TableSupport
{
    /** Prevent one malformed or abusive list request from exhausting memory. */
    public static final int MAX_PAGE_SIZE = 500;
    /**
     * 当前记录起始索引
     */
    public static final String PAGE_NUM = "pageNum";

    /**
     * 每页显示记录数
     */
    public static final String PAGE_SIZE = "pageSize";

    /**
     * 排序列
     */
    public static final String ORDER_BY_COLUMN = "orderByColumn";

    /**
     * 排序的方向 "desc" 或者 "asc".
     */
    public static final String IS_ASC = "isAsc";

    /**
     * 分页参数合理化
     */
    public static final String REASONABLE = "reasonable";

    /**
     * 封装分页对象
     */
    public static PageDomain getPageDomain()
    {
        PageDomain pageDomain = new PageDomain();
        pageDomain.setPageNum(Convert.toInt(ServletUtils.getParameter(PAGE_NUM), 1));
        int requestedSize = Convert.toInt(ServletUtils.getParameter(PAGE_SIZE), 10);
        pageDomain.setPageSize(Math.min(Math.max(requestedSize, 1), MAX_PAGE_SIZE));
        pageDomain.setOrderByColumn(ServletUtils.getParameter(ORDER_BY_COLUMN));
        pageDomain.setIsAsc(ServletUtils.getParameter(IS_ASC));
        pageDomain.setReasonable(ServletUtils.getParameterToBool(REASONABLE));
        return pageDomain;
    }

    public static PageDomain buildPageRequest()
    {
        return getPageDomain();
    }
}

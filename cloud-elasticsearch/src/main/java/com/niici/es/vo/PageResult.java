package com.niici.es.vo;

import lombok.Data;

import java.util.List;

/**
 * 通用分页结果封装
 *
 * @param <T> 数据列表的元素类型
 */
@Data
public class PageResult<T> {

    /** 总命中条数（用于前端计算总页数）*/
    private long total;

    /** 当前页数据列表 */
    private List<T> list;

    public static <T> PageResult<T> of(long total, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setList(list);
        return result;
    }
}

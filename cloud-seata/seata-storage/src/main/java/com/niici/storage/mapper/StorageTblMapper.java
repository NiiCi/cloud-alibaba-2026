package com.niici.storage.mapper;

import com.niici.storage.bean.StorageTbl;
import org.apache.ibatis.annotations.Param;

/**
* @author lfy
* @description 针对表【storage_tbl】的数据库操作Mapper
* @createDate 2025-01-08 18:35:07
* @Entity com.atguigu.storage.bean.StorageTbl
*/
public interface StorageTblMapper {

    int deleteByPrimaryKey(Long id);

    int insert(StorageTbl record);

    int insertSelective(StorageTbl record);

    StorageTbl selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(StorageTbl record);

    int updateByPrimaryKey(StorageTbl record);

    void deduct(@Param("commodityCode") String commodityCode, @Param("count") int count);

    /**
     * TCC Try阶段：扣减可用库存，增加冻结库存
     */
    void tryDeduct(@Param("commodityCode") String commodityCode, @Param("count") int count);

    /**
     * TCC Confirm阶段：扣减冻结库存（正式提交）
     */
    void confirmDeduct(@Param("commodityCode") String commodityCode, @Param("count") int count);

    /**
     * TCC Cancel阶段：恢复可用库存，释放冻结库存
     */
    void cancelDeduct(@Param("commodityCode") String commodityCode, @Param("count") int count);
}

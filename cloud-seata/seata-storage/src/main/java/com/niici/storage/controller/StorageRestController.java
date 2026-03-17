package com.niici.storage.controller;

import com.niici.storage.service.StorageTccAction;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StorageRestController {

    @Autowired
    StorageTccAction storageTccAction;

    /**
     * 扣减库存（TCC Try 阶段入口）
     */
    @GetMapping("/deduct")
    public String deduct(@RequestParam("commodityCode") String commodityCode,
                         @RequestParam("count") Integer count) {
        storageTccAction.tryDeduct(new BusinessActionContext(), commodityCode, count);
        return "storage deduct success";
    }
}

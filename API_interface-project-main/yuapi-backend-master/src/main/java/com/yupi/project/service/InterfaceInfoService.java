package com.yupi.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yuapicommon.model.entity.InterfaceInfo;

/**
 * 接口信息服务
 */
public interface InterfaceInfoService extends IService<InterfaceInfo> {

    /**
     * 用户添加新接口前，先校验该接口信息是否符合要求
     * @param interfaceInfo
     * @param add
     * @return
     */
    void validInterfaceInfo(InterfaceInfo interfaceInfo, boolean add);
}

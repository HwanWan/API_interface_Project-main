package com.yupi.yuapicommon.service;

import com.yupi.yuapicommon.model.entity.UserInterfaceInfo;

/**
 * 内部用户接口信息服务
 *
 */
public interface InnerUserInterfaceInfoService {

    /**
     * 调用接口统计
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    boolean invokeCount(long interfaceInfoId, long userId);

    /**
     * 用户调用接口前，校验该用户是否还有次数剩余次数可用，如果没有直接报错
     * @param userInterfaceInfo
     * @param add
     * @return
     */
    void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add);
}

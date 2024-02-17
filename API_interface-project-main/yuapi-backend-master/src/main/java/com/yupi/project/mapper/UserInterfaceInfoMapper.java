package com.yupi.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.yuapicommon.model.entity.UserInterfaceInfo;

import java.util.List;

/**
 * 用户接口信息 Mapper
 *
 */
public interface UserInterfaceInfoMapper extends BaseMapper<UserInterfaceInfo> {


    /**
     * 根据接口总调用次数降序排序
     * @param limit
     * @return 返回调用次数最多的前limit条接口的接口id、总调用次数
     */
    List<UserInterfaceInfo> listTopInvokeInterfaceInfo(int limit);
}





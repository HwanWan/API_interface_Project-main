package com.yupi.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.mapper.UserInterfaceInfoMapper;
import com.yupi.project.service.UserInterfaceInfoService;
import com.yupi.yuapicommon.model.entity.UserInterfaceInfo;
import jdk.nashorn.internal.ir.annotations.Reference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 用户接口信息服务实现类
 */
@Service
public class UserInterfaceInfoServiceImpl extends ServiceImpl<UserInterfaceInfoMapper, UserInterfaceInfo>
    implements UserInterfaceInfoService{

    @Resource
    UserInterfaceInfoMapper userInterfaceInfoMapper;

    /**
     * 用户调用接口前，校验该用户是否还有次数剩余次数可用，如果没有直接报错
     * @param userInterfaceInfo
     * @param add
     * @return
     */
    @Override
    public void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add) {
        if (userInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 创建时，所有参数必须非空
        if (add) {
            if (userInterfaceInfo.getInterfaceInfoId() > 0 || userInterfaceInfo.getUserId() > 0) {
                System.out.println("add:"+add);
                QueryWrapper<UserInterfaceInfo> queryWrapper=new QueryWrapper<>();
                queryWrapper.eq("interfaceInfoId",userInterfaceInfo.getInterfaceInfoId());
                queryWrapper.eq("userId",userInterfaceInfo.getUserId());
                userInterfaceInfo=userInterfaceInfoMapper.selectOne(queryWrapper);
                System.out.println("！！！！！ ===》 userInterfaceInfo="+userInterfaceInfo);
                System.out.println("！！！！！ ===》 interfaceInfoId="+userInterfaceInfo.getInterfaceInfoId()+"userId="+userInterfaceInfo.getUserId());
                if(userInterfaceInfo==null)
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口或用户不存在");
                System.out.println("====>用户:"+userInterfaceInfo.getUserId()+"剩余次数为{}"+userInterfaceInfo.getLeftNum());
            }
        }
        if (userInterfaceInfo.getLeftNum()==null || userInterfaceInfo.getLeftNum() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "剩余次数不能小于 0");
        }
    }

    /**
     * 调用接口统计
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    @Override
    @Transactional
    public boolean invokeCount(long interfaceInfoId, long userId) {

        if(interfaceInfoId<0 || userId <0) throw new BusinessException(ErrorCode.PARAMS_ERROR);

        // 更新数据库:
        UpdateWrapper<UserInterfaceInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("interfaceInfoId",interfaceInfoId);
        updateWrapper.eq("userId",userId);
        updateWrapper.setSql("totalNum = totalNum+1 , leftNum = leftNum-1");
        return this.update(updateWrapper);
    }

}





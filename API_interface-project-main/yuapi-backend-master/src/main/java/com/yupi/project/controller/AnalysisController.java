package com.yupi.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.project.annotation.AuthCheck;
import com.yupi.project.common.BaseResponse;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.common.ResultUtils;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.mapper.UserInterfaceInfoMapper;
import com.yupi.project.model.vo.InterfaceInfoVO;
import com.yupi.project.service.InterfaceInfoService;
import com.yupi.yuapicommon.model.entity.InterfaceInfo;
import com.yupi.yuapicommon.model.entity.UserInterfaceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 分析控制器
 *
 */
@RestController
@RequestMapping("/analysis")
@Slf4j
public class AnalysisController {

    @Resource
    private UserInterfaceInfoMapper userInterfaceInfoMapper;

    @Resource
    private InterfaceInfoService interfaceInfoService;

    /**
     * 接口统计
     * 1. 保证每个用户只能有一定的调用接口次数，避免刷接口进行恶意攻击
     * 2. 可以计费：调用接口达到一定次数进行收费
     * */
    @GetMapping("/top/interface/invoke")
    @AuthCheck(mustRole = "admin") // AOP拦截校验当前用户是否为admin管理员
    public BaseResponse<List<InterfaceInfoVO>> listTopInvokeInterfaceInfo() {

        // 1.获取每个接口所有调用次数，取调用次数最高的3个
        // 这个每个UserInterfaceInfo对象里只含 interfaceInfoId、各用户对该接口的总调用次数totalNum
        List<UserInterfaceInfo> userInterfaceInfoList = userInterfaceInfoMapper.listTopInvokeInterfaceInfo(3);

        // 2.将List<UserInterfaceInfo> 按interfaceInfoId 分组转为 Map<InterfaceInfoId, List<UserInterfaceInfo>>
        Map<Long, List<UserInterfaceInfo>> map = userInterfaceInfoList.stream()
                .collect(Collectors.groupingBy(UserInterfaceInfo::getInterfaceInfoId));

        // 3.根据InterfaceInfoId获取完整接口InterfaceInfo对象
        LambdaQueryWrapper<InterfaceInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(InterfaceInfo::getId,map.keySet());
        List<InterfaceInfo> interfaceInfoList = interfaceInfoService.list(queryWrapper);

        // 4.将这些接口InterfaceInfo对象的List 转为InterfaceInfoVO的List
        /**
         * Java8 List 转 List方法：
         * List list2 = userList.stream().map(对象转换函数).collect(Collectors.toList())
         * */
        // interfaceInfo->{}中interfaceInfo作为传入的形参名
        List<InterfaceInfoVO> interfaceInfoVOList = interfaceInfoList.stream().map(interfaceInfo -> {
            // 将传入的interfaceInfo对象 InterfaceInfoVO对象【复制其totalNum】
            InterfaceInfoVO interfaceInfoVO = new InterfaceInfoVO();
            BeanUtils.copyProperties(interfaceInfo, interfaceInfoVO);

            // InterfaceInfoVO对象的totalNum是指各用户对该接口的总调用次数，而interfaceInfo的totalNum是某个用户对该接口的调用次数
            // Map<InterfaceInfoId, List<UserInterfaceInfo>> , 而List<UserInterfaceInfo>只有一个UserInterfaceInfo
            int totalNum = map.get(interfaceInfo.getId()).get(0).getTotalNum();
            interfaceInfoVO.setTotalNum(totalNum);
            return interfaceInfoVO;
        }).collect(Collectors.toList());

        return ResultUtils.success(interfaceInfoVOList);
    }
}

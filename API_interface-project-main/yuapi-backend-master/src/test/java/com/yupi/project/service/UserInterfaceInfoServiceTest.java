package com.yupi.project.service;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yuapicommon.model.entity.UserInterfaceInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 用户接口信息服务测试
 */
@SpringBootTest
public class UserInterfaceInfoServiceTest {

    @Resource
    private UserInterfaceInfoService userInterfaceInfoService;

    @Test
    public void test1(){
        String userRequestParams="{ \"userName\": \"userName\", \"type\": \"string\" } ";
        Map map = JSON.parseObject(userRequestParams, Map.class);
        System.out.println(map);


    }

    @Test
    public void invokeCount() {
        UserInterfaceInfo userInterfaceInfo=new UserInterfaceInfo();
        userInterfaceInfo.setUserId(1758021687552540673L);
        userInterfaceInfo.setInterfaceInfoId(1L);
        userInterfaceInfoService.validUserInterfaceInfo(userInterfaceInfo,true);
    }
}
package univsersity.jinan.api_client_sdk.client;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import univsersity.jinan.api_client_sdk.model.User;
import univsersity.jinan.api_client_sdk.utils.SignUtils;

import java.util.HashMap;

@Data
public class ApiClient {


    private String accessKey;
    private String secretKey;

    public ApiClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public String getNameByGet(String url, String userName){
        //GET请求
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("userName",userName);
        String content = HttpUtil.get(url,paramMap);
        return content;
    }


    public String getNameByPost(String url,User user){
        //POST请求
        String data = JSONUtil.toJsonStr(user);
        HashMap<String, Object> hashMap =new HashMap<>();
        String jsonUserName = JSONUtil.toJsonStr(user.getUserName());
        getHeaderMap(hashMap,data);
        hashMap.put("userName",jsonUserName);
        String result = HttpUtil.post(url, hashMap);
        return result;
    }

    /**
     * 设置请求参数：
     *     在发送请求给后端服务器前，先设置签名【secretKey+body数据的加密】、nonce随机数【防重放问题】放在请求头
     * */
    public void getHeaderMap(HashMap hashMap,String data){
        hashMap.put("accessKey",accessKey);
        Gson gson = new Gson();
        User user = gson.fromJson(data, User.class);
        //签名=secretKey+body数据的加密
        SignUtils signUtils = new SignUtils();
        hashMap.put("sign",signUtils.getSign(secretKey+"."+data));

        hashMap.put("id",String.valueOf(user.getId()));
        Long delayTime = 100000L;
        // 防重放: nonce随机数只能被使用一次，确保每次发送的请求只能第一次有效
        hashMap.put("nonce", String.valueOf(RandomUtil.randomInt(4)));
        // 避免数据库存储过多nonce随机数，所以nonce只在一定时间内有效，之后会被清空
        hashMap.put("timeStamp", String.valueOf(System.currentTimeMillis()/1000+delayTime));

        hashMap.put("body",data);

    }

    public HttpResponse getNameByJSONPost(String url, User user){
        //POST请求
        String json = JSONUtil.toJsonStr(user);
        HashMap<String, String> hashMap = new HashMap<>();
        getHeaderMap(hashMap,json);
        HttpResponse httpResponse = HttpRequest.post(url)
                .addHeaders(hashMap)
                .body(json)
                .execute();
        System.out.println("===??>>> 结果："+httpResponse);
        return httpResponse;
    }

}

package university.jinan.filter;



import com.yupi.yuapicommon.model.entity.InterfaceInfo;
import com.yupi.yuapicommon.model.entity.User;
import com.yupi.yuapicommon.service.InnerInterfaceInfoService;
import com.yupi.yuapicommon.service.InnerUserInterfaceInfoService;
import com.yupi.yuapicommon.service.InnerUserService;
import lombok.extern.slf4j.Slf4j;

import org.apache.dubbo.config.annotation.DubboReference;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import univsersity.jinan.api_client_sdk.utils.SignUtils;


import java.util.Arrays;
import java.util.List;

/**
 * 全局过滤器
 * */
@Slf4j
@Component //要让springboot扫描到才生效
public class ApiGlobalFilter implements GlobalFilter, Ordered {



    @DubboReference
    private InnerInterfaceInfoService interfaceInfoService;
    @DubboReference
    private InnerUserInterfaceInfoService userInterfaceInfoService;
    @DubboReference
    private InnerUserService userService;

    private User invokeUser;

    // 白名单 ==> 在名单内的主机才能进行
    private List<String> IP_WHITE_LIST= Arrays.asList("127.0.0.1");
    private final String PORT="http://localhost:8091";


    /**
     * springcloud网关的全局过滤器
     * 鉴权通过才放行
     * 官方文档：https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#global-filters
     * */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // 1.用户发送请求到API网关 ==》 application 配置中实现
        // 2.请求日志：记录这次请求的请求信息，如本次请求的请求方式、请求参数等
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        LogRequestInfo(request);

        // 3.白名单 ==> 在名单内的主机【如127.0.0.1】才能往下进行 ==》 request.getId()请求的唯一标识
//        if(!IP_WHITE_LIST.contains(request.getLocalAddress().getHostString())){
//            // 设置状态码并设置请求结束【不在白名单内的用户不允许继续执行请求】
//            return refuseRequest(response, HttpStatus.FORBIDDEN);
//        }

        InterfaceInfo interfaceInfo;
        // 5.请求的模拟接口是否存在，以及查看剩余可调用次数，如果大于0
        try {
            interfaceInfo = interfaceInfoService.getInterfaceInfo(PORT + request.getPath(), String.valueOf(request.getMethod()));
            if(interfaceInfo==null)
                return refuseRequest(response, HttpStatus.FORBIDDEN);
            //System.out.println("接口信息==》》》"+interfaceInfo);
        }catch (Exception e){
            throw new RuntimeException("测试请求的模拟接口是否存在失败");
        }

        // 4.权鉴定(判断accessKey、secretKey否合法)
        if(CheckAuth(exchange)!=null) {
            return refuseRequest(response, HttpStatus.FORBIDDEN);
        }



/*
        // 6.是否还有调用次数，如果没有直接报错 todo:user表的增添一个字段统计用户可以调用次数
//        UserInterfaceInfo userInterfaceInfo=new UserInterfaceInfo();
//        userInterfaceInfo.setUserId(invokeUser.getId());
//        userInterfaceInfo.setInterfaceInfoId(interfaceInfo.getId());
//        userInterfaceInfoService.validUserInterfaceInfo(userInterfaceInfo,true);
*/
        // 处理放行后的响应结果
        return handleResponse(exchange, chain, interfaceInfo.getId(), invokeUser.getId());

    }

    /**
     * 处理响应
     *
     * @param exchange
     * @param chain
     * @return
     */
    public Mono<Void> handleResponse(ServerWebExchange exchange, GatewayFilterChain chain, long interfaceInfoId, long userId) {
        try {
            ServerHttpResponse originalResponse = exchange.getResponse();
            // 缓存数据的工厂
            DataBufferFactory bufferFactory = originalResponse.bufferFactory();
            // 拿到响应码
            HttpStatus statusCode = originalResponse.getStatusCode();
            if (statusCode == HttpStatus.OK) {
                ServerHttpResponseDecorator response = new ServerHttpResponseDecorator(originalResponse) {
                    // 放行结束返回响应，才会执行该方法 ==》 调用接口次数+1
                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        System.out.println("!!!!!~~~~~调用成功，接口调用次数 + 1 invokeCount");
                        // 7. 调用成功，接口调用次数 + 1 invokeCount
                        try {
                            userInterfaceInfoService.invokeCount(interfaceInfoId, userId);
                        } catch (Exception e) {
                            log.error("invokeCount error", e);
                        }
                        // if body is not a flux. never got there.
                        return super.writeWith(body);
                    }

                    @Override
                    public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                        return writeWith(Flux.from(body)
                                .flatMapSequential(p -> p));
                    }
                };
                // replace response with decorator
                return chain.filter(exchange.mutate().response(response).build());


            }
            return chain.filter(exchange); // 降级处理返回数据
        } catch (Exception e) {
            log.error("网关处理响应异常" + e);
            return chain.filter(exchange);
        }
    }

     // !! -1 d的话响应结果不会进入writeWith()
    @Override
    public int getOrder() {
        // -1 is response write filter, must be called before that
        return -2;
    }
    /**
     * 拒绝继续往下处理请求
     */
    public Mono<Void> refuseRequest(ServerHttpResponse response,HttpStatus status){
        response.setStatusCode(status);
        return response.setComplete();
    }

    /**
     * 打印请求信息
     * */
    public void LogRequestInfo(ServerHttpRequest request){
        log.info("请求ID: "+request.getId());
        log.info("请求方式: "+request.getMethod());
        log.info("请求路径: "+request.getPath().value());
        log.info("目标: "+request.getRemoteAddress());
        log.info("请求参数: "+request.getQueryParams());
        log.info("请求主机名: "+request.getLocalAddress().getHostString());
    }
    /**
     * 权限校验
     * */
    public Mono<Void> CheckAuth(ServerWebExchange exchange){
        // backend后端项目在发送请求前，都已经将用户的accesskey等信息封装到请求头中了
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        String accessKey = headers.getFirst("accessKey");
        String timeStamp =headers.getFirst ("timeStamp");
        String nonce = headers.getFirst("nonce");
        String body = ((HttpHeaders) headers).getFirst("body");
        String sign = headers.getFirst("sign");
        String userID = headers.getFirst("id");

        // 根据accessKey从数据库获取用户信息
        if(userID==null) return refuseRequest(exchange.getResponse(),HttpStatus.FORBIDDEN);
        Long id = Long.valueOf(userID);

        try {

            invokeUser = userService.getInvokeUser(accessKey);
            if(invokeUser==null){

                return refuseRequest(exchange.getResponse(),HttpStatus.FORBIDDEN);
            }
        }catch (Exception e){
            throw new RuntimeException("从数据库获取用户信息失败");
        }
        String UserAccessKey=invokeUser.getAccessKey();

        //System.out.println("用户信息==》》》"+invokeUser);

        // 2.鉴定SecretKe是否合法
        SignUtils signUtils = new SignUtils();
        String utilsSign = signUtils.getSign(invokeUser.getSecretKey()+"."+body);
        if(!sign.equals(utilsSign) ){
            return refuseRequest(exchange.getResponse(),HttpStatus.FORBIDDEN);
        }
        // 3.查看时间戳是否超时【5分钟】
        if((Long.valueOf(timeStamp)<System.currentTimeMillis()/1000)){
            return refuseRequest(exchange.getResponse(),HttpStatus.FORBIDDEN);
        }
        // 4.nonce随机数防重放【todo 生成的nouce放Redis缓存比较，后续可以做】
        if(Integer.valueOf(nonce)>100){
            return refuseRequest(exchange.getResponse(),HttpStatus.FORBIDDEN);
        }
        return null;

    }




}

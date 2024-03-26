package com.macro.mall.portal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @auther macrozheng
 * @description 支付宝支付相关配置
 * @date 2023/9/8
 * @github https://github.com/macrozheng
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "nihaopay")
public class NihaopayConfig {
    /**
     * 支付网关
     */
    private String gatewayUrl;
    /**
     * 查询订单状态
     * https://api.nihaopay.com/v1.2/transactions/merchant/{reference}
     */
    private String merchantUrl;
    /**
     * 设置Bearer 加API Token 作为认证方式
     */
    private String gatewayToken;
    /**
     * 接收支付结果异步通知的URL
     */
    private String ipn_url;
    /**
     * 支付成功后浏览器返回商户网站的URL
     */
    private String callback_url;
    /**
     * 商户网站的主页
     */
    private String redirect_home;
    private String redirect_payResult;
}

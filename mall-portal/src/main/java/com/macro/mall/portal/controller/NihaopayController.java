package com.macro.mall.portal.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.macro.mall.common.api.CommonResult;
import com.macro.mall.portal.config.AlipayConfig;
import com.macro.mall.portal.config.NihaopayConfig;
import com.macro.mall.portal.domain.AliPayParam;
import com.macro.mall.portal.domain.OmsOrderDetail;
import com.macro.mall.portal.service.AlipayService;
import com.macro.mall.portal.service.OmsPortalOrderService;
import com.macro.mall.portal.util.HttpRequestUtil;
import com.macro.mall.portal.util.SecurityUtils;
import com.macro.mall.portal.vo.API.APISecurePayRespVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @auther macrozheng
 * @description 支付宝支付Controller
 * @date 2023/9/8
 * @github https://github.com/macrozheng
 */
@Controller
@Api(tags = "NihaopayController")
@Tag(name = "NihaopayController", description = "支付相关接口")
@RequestMapping("/nihaopay")
public class NihaopayController {
    private static final Logger logger = LoggerFactory.getLogger(NihaopayController.class);
    @Autowired
    private NihaopayConfig nihaopayConfig;
    @Autowired
    private OmsPortalOrderService portalOrderService;


    @ApiOperation("用户支付成功的通知")
    @RequestMapping(value = "/nihaopayNotify", method = RequestMethod.POST)
    public String nihaopayNotify(HttpServletRequest request) {
        String token = nihaopayConfig.getGatewayToken();
        Map<String, String[]> params = request.getParameterMap();
        if (params.size() < 1) {
            return "failed";
        }
        StringBuilder str = new StringBuilder();
        str.append("Form Data:");
        for (String key : params.keySet()) {
            String[] values = params.get(key);
            for (int i = 0; i < values.length; i++) {
                String value = values[i];
                str.append("<br/>" + key + "=" + value);
            }
        }
        Set<String> keySet = params.keySet();
        List<String> keyList = new ArrayList<String>(keySet);
        Collections.sort(keyList);
        StringBuffer mdStr = new StringBuffer();
        for (String key : keyList) {
            String value = params.get(key)[0];
            if (!key.equals("verify_sign") && params.get(key) != null && value != null && !value.equals("null")) {
                mdStr.append(key + "=" + value + "&");
            }
        }
        mdStr.append(SecurityUtils.MD5(token).toLowerCase());
        String verify_sign = params.get("verify_sign")[0];
        String sign = SecurityUtils.MD5(mdStr.toString()).toLowerCase();
        if (!sign.equals(verify_sign)) {
            return "failed";
        }

        String reference = params.get("reference")[0];
        String orderSn = reference.replace("alipay_", "");
        orderSn = orderSn.replace("wechatpay_", "");
        orderSn = orderSn.replace("unionpay_", "");
        orderSn = orderSn.replace("PayPal_", "");

        Integer payType = 1;
        if (reference.contains("alipay_")) {
            payType = 1;
        } else if (reference.contains("wechatpay_")) {
            payType = 2;
        } else if (reference.contains("unionpay_")) {
            payType = 3;
        } else if (reference.contains("PayPal_")) {
            payType = 4;
        }

        portalOrderService.paySuccessByOrderSn(orderSn, payType);
        return "ok";
    }


    @RequestMapping(value = "/callback")
    public String callback(HttpServletRequest request) {
        try {
            logger.info("callback request:");
            logger.info(getClientInfo(request));

            Map<String, String[]> params = request.getParameterMap();
            if (params.size() < 1) {
                return "redirect:/"+this.nihaopayConfig.getCallback_url();
            }
            StringBuilder str = new StringBuilder();
            str.append("Form Data:");
            for (String key : params.keySet()) {
                String[] values = params.get(key);
                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    str.append("<br/>" + key + "=" + value);
                }
            }
            Set<String> keySet = params.keySet();
            List<String> keyList = new ArrayList<String>(keySet);
            Collections.sort(keyList);
            StringBuffer mdStr = new StringBuffer();
            for (String key : keyList) {
                String value = params.get(key)[0];
                if (!key.equals("verify_sign") && params.get(key) != null && value != null && !value.equals("null")) {
                    mdStr.append(key + "=" + value + "&");
                }
            }
            logger.info("sign prams string:" + mdStr.toString());
            String token = nihaopayConfig.getGatewayToken();
            mdStr.append(SecurityUtils.MD5(token).toLowerCase());
            logger.info("prams and token sign string:" + mdStr.toString());
            String verify_sign = params.get("verify_sign")[0];
            String sign = SecurityUtils.MD5(mdStr.toString()).toLowerCase();
            logger.info("sign string:" + sign);
            logger.info("verify_sign string:" + verify_sign);
            if (sign.equals(verify_sign)) {
                str.append("<br/>sign_vlidated -> true");
            } else {
                str.append("<br/>sign_vlidated -> false");
            }
            logger.info("Callback " + str.toString());
            String reference = params.get("reference")[0];
            String orderSn = reference.replace("alipay_", "");
            orderSn = orderSn.replace("wechatpay_", "");
            orderSn = orderSn.replace("unionpay_", "");
            orderSn = orderSn.replace("PayPal_", "");
            return "redirect:/"+this.nihaopayConfig.getCallback_url()+"?out_trade_no="+orderSn;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            return "redirect:/"+this.nihaopayConfig.getCallback_url();
        }
    }


    @RequestMapping(value = "/ipn")
    private @ResponseBody void receiveIPN(HttpServletResponse response, HttpServletRequest request,
                                         APISecurePayRespVO respVO) {
        try {
            logger.info("IPN request:");
            logger.info(getClientInfo(request));
            logger.info(respVO.toString());

            BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            logger.info("getInputStream to String: " + sb.toString());

            Map<String, String[]> params = request.getParameterMap();
            if (params.size() < 1) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
            logger.info(request.getParameter("id"));
            StringBuilder str = new StringBuilder();
            str.append("Form Data:");
            for (String key : params.keySet()) {
                String[] values = params.get(key);
                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    str.append("\r\n " + key + " -> " + value);
                }
            }
            Set<String> keySet = params.keySet();
            List<String> keyList = new ArrayList<String>(keySet);
            Collections.sort(keyList);
            StringBuffer mdStr = new StringBuffer();
            for (String key : keyList) {
                String value = params.get(key)[0];
                if (!key.equals("verify_sign") && params.get(key) != null && value != null && !value.equals("null")) {
                    mdStr.append(key + "=" + value + "&");
                }
            }
            logger.info("sign prams string:" + mdStr.toString());
            String token = nihaopayConfig.getGatewayToken();
            mdStr.append(SecurityUtils.MD5(token).toLowerCase());
            logger.info("prams and token sign string:" + mdStr.toString());
            String verify_sign = params.get("verify_sign")[0];
            String sign = SecurityUtils.MD5(mdStr.toString()).toLowerCase();
            logger.info("sign string:" + sign);
            logger.info("verify_sign string:" + verify_sign);
            if (sign.equals(verify_sign)) {
                str.append("\r\n sign_vlidated -> true");
            } else {
                str.append("\r\n sign_vlidated -> false");
            }
            logger.info("IPN: " + str.toString());
            response.setHeader("Content-type", "application/json");// jquery post must return json type
            response.getWriter().print(str);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
        }
    }

    public static String getClientInfo(HttpServletRequest request) {
        StringBuilder str = new StringBuilder();
        str.append("\r\n ----------New Request---------------");
        str.append("\r\n RequestURL:" + request.getRequestURL());
        str.append("\r\n Request Header");
        str.append("\r\n Origin:" + request.getHeader("Origin"));
        str.append("\r\n Referer:" + request.getHeader("Referer"));
        str.append("\r\n Accept:" + request.getHeader("Accept"));
        str.append("\r\n Host:" + request.getHeader("Host"));
        str.append("\r\n User-Agent:" + request.getHeader("User-Agent"));
        str.append("\r\n Locale:" + request.getLocale());
        str.append("\r\n Protocol:" + request.getProtocol());
        str.append("\r\n Scheme:" + request.getScheme());
        str.append("\r\n Connection:" + request.getHeader("Connection"));
        str.append("\r\n Character Encoding:" + request.getCharacterEncoding());
        str.append("\r\n Content Type:" + request.getContentType());
        str.append("\r\n Content Length:" + request.getContentLength());
        str.append("\r\n Http Method:" + request.getMethod());
        str.append("\r\n Remote Addr: " + request.getRemoteAddr());
        str.append("\r\n Remote Host: " + request.getRemoteHost());
        str.append("\r\n Remote Port: " + request.getRemotePort());
        str.append("\r\n Remote User: " + request.getRemoteUser());
        str.append("\r\n Query String: " + request.getQueryString());
        str.append("\r\n Form Data:");
        Map<String, String[]> params = request.getParameterMap();
        for (String key : params.keySet()) {
            String[] values = params.get(key);
            for (int i = 0; i < values.length; i++) {
                String value = values[i];
                str.append("\r\n " + key + " -> " + value);
            }
        }
        str.append("\r\n ----------End Request---------------");
        return str.toString();
    }
}

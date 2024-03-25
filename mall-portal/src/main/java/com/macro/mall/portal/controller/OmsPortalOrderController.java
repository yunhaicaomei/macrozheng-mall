package com.macro.mall.portal.controller;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.http.*;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.macro.mall.common.api.CommonPage;
import com.macro.mall.common.api.CommonResult;
import com.macro.mall.portal.config.AlipayConfig;
import com.macro.mall.portal.config.NihaopayConfig;
import com.macro.mall.portal.domain.ConfirmOrderResult;
import com.macro.mall.portal.domain.OmsOrderDetail;
import com.macro.mall.portal.domain.OrderParam;
import com.macro.mall.portal.service.OmsPortalOrderService;
import com.macro.mall.portal.util.HttpRequestUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 订单管理Controller
 * Created by macro on 2018/8/30.
 */
@Controller
@Api(tags = "OmsPortalOrderController")
@Tag(name = "OmsPortalOrderController", description = "订单管理")
@RequestMapping("/order")
public class OmsPortalOrderController {
    @Autowired
    private NihaopayConfig nihaopayConfig;
    @Autowired
    private OmsPortalOrderService portalOrderService;

    @ApiOperation("根据购物车信息生成确认单")
    @RequestMapping(value = "/generateConfirmOrder", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<ConfirmOrderResult> generateConfirmOrder(@RequestBody List<Long> cartIds) {
        ConfirmOrderResult confirmOrderResult = portalOrderService.generateConfirmOrder(cartIds);
        return CommonResult.success(confirmOrderResult);
    }

    @ApiOperation("根据购物车信息生成订单")
    @RequestMapping(value = "/generateOrder", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult generateOrder(@RequestBody OrderParam orderParam) {
        Map<String, Object> result = portalOrderService.generateOrder(orderParam);
        return CommonResult.success(result, "下单成功");
    }

    @ApiOperation("用户支付成功的回调")
    @RequestMapping(value = "/paySuccess", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult paySuccess(@RequestParam Long orderId,@RequestParam Integer payType) {
        Integer count = portalOrderService.paySuccess(orderId,payType);
        return CommonResult.success(count, "支付成功");
    }

    @ApiOperation("生成支付链接")
    @RequestMapping(value = "/generatePayUrl", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult paySuccess2(@RequestParam Long orderId,@RequestParam Integer payType) {
        OmsOrderDetail orderDetail = portalOrderService.detail(orderId);
        Integer status = orderDetail.getStatus();
        //非待支付状态
        if (status != 0) {
            return CommonResult.failed("支付失败");
        }
        String vendor = "alipay";
        switch (payType) {
            case 1:
                vendor = "alipay";
                break;
            case 2:
                vendor = "wechatpay";
                break;
            case 3:
                vendor = "unionpay";
                break;
            case 4:
                vendor = "PayPal";
                break;
            default:
                vendor = "alipay";
        }
        String payReqUrl = "https://api.nihaopay.com/v1.2/transactions/securepay";
        String token = "a2efdee4184c4f772666c9e05a77c586a0e2cf997c35db50dd6fd3f06b61d8c8";
        String currency = "JPY";
        String ipn_url = "http://218.90.154.14:14019/order/paySuccess";
        String callback_url = "http://218.90.154.14:14018/pages/money/paySuccess";

        String amount = orderDetail.getTotalAmount().setScale(0).toString();
        amount = "1";
        String reference = vendor+"_"+ orderDetail.getId().toString();
        reference = vendor+"_"+ orderDetail.getOrderSn();
//        reference = SecureUtil.aes(SymmetricAlgorithm.AES.getValue().getBytes()).encryptHex(reference);
        //测试用接口信息
        payReqUrl = this.nihaopayConfig.getGatewayUrl();
        token = this.nihaopayConfig.getGatewayToken();
        ipn_url = this.nihaopayConfig.getIpn_url();
        callback_url = this.nihaopayConfig.getCallback_url();

        String inputParam = "amount=" + amount + "&";
        inputParam += "currency=" + currency + "&";
        inputParam += "vendor=" + vendor + "&";
        inputParam += "reference=" + reference + "&";
        inputParam += "ipn_url=" + ipn_url + "&";
        inputParam += "callback_url=" + callback_url + "&";
        inputParam += "description=" + orderDetail.getReceiverName() + "的商品订单" + "&";
        inputParam += "inWechat=false&";
        inputParam += "response_format=JSON";

        try {
            String result = HttpRequestUtil.sendAuthPost(payReqUrl, inputParam, "Bearer " + token);
            JSONObject obj = JSONUtil.parseObj(result);
            if (obj.containsKey("url")) {
                return CommonResult.success(obj, "支付成功");
            } else if (obj.containsKey("form")) {
                JSONObject objForm = obj.getJSONObject("form");
/**
                Map<String, String> paramsMap = objForm.get("params", Map.class);
                String actionUrl = objForm.getStr("actionUrl");
                HttpRequest postRequest = HttpUtil.createPost(actionUrl);
                postRequest.header("Content-type", "text/html");
                postRequest.charset("UTF-8");
                for (Map.Entry<String, String> stringEntry : paramsMap.entrySet()) {
                    postRequest.form(stringEntry.getKey(), stringEntry.getValue());
                }
                HttpResponse response = null;
                response = postRequest.execute();
                String json = response.body();
 */
//                String htmlForm = objForm.getStr("form");
                return CommonResult.success(objForm, "支付成功");
            } else {
                return CommonResult.failed("支付失败");
            }
        } catch (Exception ex) {
            return CommonResult.failed("支付失败");
        }
    }

    @ApiOperation("自动取消超时订单")
    @RequestMapping(value = "/cancelTimeOutOrder", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult cancelTimeOutOrder() {
        portalOrderService.cancelTimeOutOrder();
        return CommonResult.success(null);
    }

    @ApiOperation("取消单个超时订单")
    @RequestMapping(value = "/cancelOrder", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult cancelOrder(Long orderId) {
        portalOrderService.sendDelayMessageCancelOrder(orderId);
        return CommonResult.success(null);
    }

    @ApiOperation("按状态分页获取用户订单列表")
    @ApiImplicitParam(name = "status", value = "订单状态：-1->全部；0->待付款；1->待发货；2->已发货；3->已完成；4->已关闭",
            defaultValue = "-1", allowableValues = "-1,0,1,2,3,4", paramType = "query", dataType = "int")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<CommonPage<OmsOrderDetail>> list(@RequestParam Integer status,
                                                   @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                                   @RequestParam(required = false, defaultValue = "5") Integer pageSize) {
        CommonPage<OmsOrderDetail> orderPage = portalOrderService.list(status,pageNum,pageSize);
        return CommonResult.success(orderPage);
    }

    @ApiOperation("根据ID获取订单详情")
    @RequestMapping(value = "/detail/{orderId}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<OmsOrderDetail> detail(@PathVariable Long orderId) {
        OmsOrderDetail orderDetail = portalOrderService.detail(orderId);
        return CommonResult.success(orderDetail);
    }

    @ApiOperation("用户取消订单")
    @RequestMapping(value = "/cancelUserOrder", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult cancelUserOrder(Long orderId) {
        portalOrderService.cancelOrder(orderId);
        return CommonResult.success(null);
    }

    @ApiOperation("用户确认收货")
    @RequestMapping(value = "/confirmReceiveOrder", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult confirmReceiveOrder(Long orderId) {
        portalOrderService.confirmReceiveOrder(orderId);
        return CommonResult.success(null);
    }

    @ApiOperation("用户删除订单")
    @RequestMapping(value = "/deleteOrder", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult deleteOrder(Long orderId) {
        portalOrderService.deleteOrder(orderId);
        return CommonResult.success(null);
    }
}

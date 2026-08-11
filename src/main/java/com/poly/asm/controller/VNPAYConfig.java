package com.poly.asm.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VNPAYConfig {

    public static final String VNP_VERSION = "2.1.0";
    public static final String VNP_COMMAND = "pay";
    public static final String VNP_CURR_CODE = "VND";
    public static final String DEFAULT_LOCALE = "vn";

    @Value("${vnpay.payment-url}")
    private String payUrl;

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    public String getPayUrl() {
        return payUrl;
    }

    public String getTmnCode() {
        return tmnCode;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public boolean isConfigured() {
        return tmnCode != null && !tmnCode.isBlank() && !"disabled".equals(tmnCode)
                && hashSecret != null && !hashSecret.isBlank() && !"disabled".equals(hashSecret);
    }
}

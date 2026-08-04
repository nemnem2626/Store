package com.poly.asm.controller;

public class VNPAYConfig {
    // SANDBOX
    public static final String VNP_PAY_URL   = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static final String VNP_TMN_CODE  = "NQ407ZXU";
    public static final String VNP_HASH_SECRET = "JM6M1WKHPZX40X7I0OAX8NM9ECUBDQ9T"; 

    // URL của bạn nhận kết quả
    public static final String VNP_RETURN_URL = "http://localhost:8080/vnpay-return";

    public static final String VNP_VERSION = "2.1.0";
    public static final String VNP_COMMAND = "pay";
    public static final String VNP_CURR_CODE = "VND";
    public static final String DEFAULT_LOCALE = "vn";

    private VNPAYConfig() {}
}
package com.shop.service;

/**
 * Outcome of a WhatsApp send attempt (Meta or Node).
 *
 * @param sent  true when Meta/Node accepted the message
 * @param error failure details when {@code sent} is false
 * @param info  optional success note (e.g. template fallback used)
 */
public record WhatsAppSendResult(boolean sent, String error, String info) {

    public static WhatsAppSendResult ok() {
        return new WhatsAppSendResult(true, null, null);
    }

    public static WhatsAppSendResult ok(String info) {
        return new WhatsAppSendResult(true, null, info);
    }

    public static WhatsAppSendResult fail(String error) {
        return new WhatsAppSendResult(false, error == null ? "WhatsApp send failed" : error, null);
    }
}

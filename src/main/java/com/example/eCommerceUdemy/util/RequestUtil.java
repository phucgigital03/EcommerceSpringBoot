package com.example.eCommerceUdemy.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestUtil {
    public static String getIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            var remoteAddr = request.getRemoteAddr();
            log.debug("remoteAddr check: {}", remoteAddr);
            if (remoteAddr == null) {
                remoteAddr = "127.0.0.1";   // TODO: the ip of this BE app
            }
            log.debug("remoteAddr: {}", remoteAddr);
            return remoteAddr;
        }
        log.debug("remoteAddr under: {}", xForwardedForHeader.split(",")[0].trim());
        return xForwardedForHeader.split(",")[0].trim();
    }
}

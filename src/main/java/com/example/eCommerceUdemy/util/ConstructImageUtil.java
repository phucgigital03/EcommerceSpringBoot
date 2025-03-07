package com.example.eCommerceUdemy.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConstructImageUtil {
    @Value("${image.base.url}")
    private String imageBaseUrl;

    public String constructImage(String imageName){
        return imageBaseUrl.endsWith("/") ? imageBaseUrl + imageName : imageBaseUrl + "/" + imageName;
    }
}

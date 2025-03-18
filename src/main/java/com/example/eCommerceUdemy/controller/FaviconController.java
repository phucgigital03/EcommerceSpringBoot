package com.example.eCommerceUdemy.controller;

import com.example.eCommerceUdemy.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FaviconController {
    @GetMapping("favicon.ico")
    public ResponseEntity<?> getFavicon() {
        return new ResponseEntity<>("favicon.ico", HttpStatus.OK);
    }
}

package com.example.nsyy.server.controller;

import com.example.nsyy.service.LocationServices;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public String ping() {
        return "SERVER OK";
    }

    @GetMapping("/location")
    public String location() {
        try {
            return LocationServices.getInstance().location();
        } catch (Exception e) {
            return "Failed to get location: Please enable location service first";
        }
    }
}

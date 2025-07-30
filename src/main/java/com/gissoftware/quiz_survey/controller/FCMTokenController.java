package com.gissoftware.quiz_survey.controller;


import com.gissoftware.quiz_survey.service.FCMTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fcm-token")
public class FCMTokenController {

    @Autowired
    private FCMTokenService deviceTokenService;

    @PostMapping("/save")
    public ResponseEntity<String> saveToken(@RequestBody String token, @RequestBody String userId) {
        deviceTokenService.saveToken(token, userId);
        return ResponseEntity.ok("Token saved");
    }
}

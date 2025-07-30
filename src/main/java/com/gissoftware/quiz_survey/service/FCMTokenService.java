package com.gissoftware.quiz_survey.service;


import com.gissoftware.quiz_survey.model.FCMToken;
import com.gissoftware.quiz_survey.repository.FCMTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FCMTokenService {

    @Autowired
    private FCMTokenRepository tokenRepository;

    public void saveToken(String token, String userId) {
        FCMToken deviceToken = new FCMToken(token, userId);
        tokenRepository.save(deviceToken);
    }

    public List<String> getAllTokens() {
        return tokenRepository.findAll()
                .stream()
                .map(FCMToken::getToken)
                .collect(Collectors.toList());
    }

    public List<String> getTokensByUserId(String userId) {
        return tokenRepository.findByUserId(userId)
                .stream()
                .map(FCMToken::getToken)
                .collect(Collectors.toList());
    }
}


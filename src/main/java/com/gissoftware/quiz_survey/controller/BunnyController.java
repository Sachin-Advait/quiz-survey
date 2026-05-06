package com.gissoftware.quiz_survey.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class BunnyController {

    @Value("${bunny.library-id}")
    private String libraryId;

    @Value("${bunny.api-key}")
    private String bunnyApiKey;

    public Map<String, Object> getVideoStatus(String videoId) {
        String url = "https://video.bunnycdn.com/library/" + libraryId + "/videos/" + videoId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("AccessKey", bunnyApiKey);

        var response = new RestTemplate().exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        var body = response.getBody();
        return Map.of(
                "status", body.get("status"),
                "encodeProgress", body.getOrDefault("encodeProgress", 0));
    }
}
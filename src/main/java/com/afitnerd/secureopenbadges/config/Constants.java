package com.afitnerd.secureopenbadges.config;

public interface Constants {

    String API_URI = "/api";
    String API_VERSION_URI = "/v1";
    String BADGE_URI = "/user/{githubUser}/repo/{githubRepo}/badge/{badgeSlug}";
    String IMAGES_PATH = "/images";
    String GITHUB_RAW_URL = "https://raw.githubusercontent.com";
    String BADGES_FLE_NAME = "badges.json";
}

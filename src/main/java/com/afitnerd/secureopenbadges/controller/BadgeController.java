package com.afitnerd.secureopenbadges.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

import static com.afitnerd.secureopenbadges.config.Constants.API_URI;
import static com.afitnerd.secureopenbadges.config.Constants.API_VERSION_URI;
import static com.afitnerd.secureopenbadges.config.Constants.BADGE_URI;
import static com.afitnerd.secureopenbadges.config.Constants.IMAGES_PATH;

@RestController
@RequestMapping(API_URI + API_VERSION_URI)
public class BadgeController {

    private final Logger log = LoggerFactory.getLogger(BadgeController.class);

    private byte[] notFound;

    private byte[] getImage(String name) throws IOException {
        InputStream is = getClass().getResourceAsStream(IMAGES_PATH + "/" + name + ".png");
        if (is == null) { throw new IOException("Image Not Found!"); }
        return is.readAllBytes();
    }

    @PostConstruct
    public void setup() {
        try {
            notFound = getImage("404");
        } catch (IOException e) {
            log.error("unable to pre-load 404 image, Error: {}", e.getMessage(), e);
        }
    }

    @GetMapping(value = BADGE_URI, produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getBadge(
        @PathVariable String githubUser, @PathVariable String githubRepo, @PathVariable String badgeSlug
    ) {
        try {
            return getImage(badgeSlug);
        } catch (IOException e) {
            log.error("Badge: {} not retrieved. Error: {}", badgeSlug, e.getMessage(), e);
            return notFound;
        }
    }
}

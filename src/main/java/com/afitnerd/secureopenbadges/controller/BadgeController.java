package com.afitnerd.secureopenbadges.controller;

import com.afitnerd.secureopenbadges.exception.InvalidBadgeException;
import com.afitnerd.secureopenbadges.model.Badge;
import com.afitnerd.secureopenbadges.service.BadgeVerifierService;
import com.afitnerd.secureopenbadges.service.GithubService;
import com.afitnerd.secureopenbadges.service.ImageService;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

import static com.afitnerd.secureopenbadges.config.Constants.API_URI;
import static com.afitnerd.secureopenbadges.config.Constants.API_VERSION_URI;
import static com.afitnerd.secureopenbadges.config.Constants.BADGE_URI;

@RestController
@RequestMapping(API_URI + API_VERSION_URI)
public class BadgeController {

    private GithubService githubService;
    private BadgeVerifierService badgeVerifierService;
    private ImageService imageService;

    private final Logger log = LoggerFactory.getLogger(BadgeController.class);

    byte[] notFound;

    public BadgeController(
        GithubService githubService, BadgeVerifierService badgeVerifierService, ImageService imageService
    ) {
        this.githubService = githubService;
        this.badgeVerifierService = badgeVerifierService;
        this.imageService = imageService;
    }

    @PostConstruct
    public void setup() {
        try {
            notFound = imageService.getImage("404");
        } catch (IOException e) {
            log.error("unable to pre-load 404 image, Error: {}", e.getMessage(), e);
        }
    }


    @GetMapping(value = BADGE_URI, produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getBadge(
        @PathVariable String githubUser, @PathVariable String githubRepo, @PathVariable String badgeSlug,
        @RequestParam(required = false) String width,
        @RequestParam(required = false) String datePosition, @RequestParam(required = false) String userPosition,
        @RequestParam(required = false) String fontColor, @RequestParam(required = false) String fontSize,
        @RequestParam(required = false) String fontFamily, @RequestParam(required = false) String fontAttr
    ) {
        // TODO - needs sanitization - duh
        try {
            GHRepository repository = githubService.getRepository(githubUser, githubRepo);
            GHCommit commit = githubService.getLatestVerifiedCommit(repository);
            List<Badge> badges = githubService.getBadge(commit, githubUser, githubRepo);
            Badge badge = badgeVerifierService.verify(badges, badgeSlug);
            log.info("Validated {} for {}/{}", badgeSlug, githubUser, githubRepo);
            return imageService.getImage(
                badge.getBadgeSlug(), githubUser, width,
                datePosition, userPosition, fontColor, fontSize, fontFamily, fontAttr, true
            );
        } catch (IOException | InvalidBadgeException e) {
            log.error(
                "Badge: {} not retrieved for {}/{}. Error: {}", badgeSlug, githubUser, githubRepo, e.getMessage(), e
            );
            byte[] ret = notFound;
            if (width != null) {
                try {
                    ret = imageService.getImage(
                        "404", null, width, null, null,
                        null, null, null, null, false
                    );
                } catch (IOException ioe) {
                    log.error("Problem returning custom notFound image. Error: {}", ioe.getMessage(), ioe);
                }
            }
            return ret;
        }
    }
}

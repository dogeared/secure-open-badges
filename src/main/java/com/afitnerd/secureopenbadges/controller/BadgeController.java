package com.afitnerd.secureopenbadges.controller;

import com.afitnerd.secureopenbadges.exception.InvalidBadgeException;
import com.afitnerd.secureopenbadges.model.Badge;
import com.afitnerd.secureopenbadges.model.ImageBuilder;
import com.afitnerd.secureopenbadges.service.BadgeVerifierService;
import com.afitnerd.secureopenbadges.service.GithubService;
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
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.AttributedString;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.afitnerd.secureopenbadges.config.Constants.API_URI;
import static com.afitnerd.secureopenbadges.config.Constants.API_VERSION_URI;
import static com.afitnerd.secureopenbadges.config.Constants.BADGE_URI;
import static com.afitnerd.secureopenbadges.config.Constants.IMAGES_PATH;

@RestController
@RequestMapping(API_URI + API_VERSION_URI)
public class BadgeController {

    private GithubService githubService;
    private BadgeVerifierService badgeVerifierService;

    private final Logger log = LoggerFactory.getLogger(BadgeController.class);

    byte[] notFound;

    enum FontAttr {
        BOLD(Font.BOLD),
        ITALIC(Font.ITALIC),
        BOLD_ITALIC(Font.BOLD|Font.ITALIC),
        PLAIN(Font.PLAIN);

        private final int value;

        FontAttr(int value) {
            this.value = value;
        }

        int getValue() {
            return value;
        }
    }

    public BadgeController(GithubService githubService, BadgeVerifierService badgeVerifierService) {
        this.githubService = githubService;
        this.badgeVerifierService = badgeVerifierService;
    }

    // TODO - ripe for caching
    private byte[] getImage(String name) throws IOException {
        return getImage(name, null, null, null, null, null, null);
    }

    private byte[] getImage(
        String name, String width, String vPos,
        String fontColor, String fontSize, String fontFamily, String fontAttr
    ) throws IOException {
        Integer _width = null;
        if (width != null) {
            try {
                _width = Integer.parseInt(width);
            } catch (NumberFormatException e) {
                log.error("{} is not a valid number for width.", width);
            }
        }
        ImageBuilder.VerticalPosition _vPos = null;
        if (vPos != null) {
            try {
                _vPos = ImageBuilder.VerticalPosition.valueOf(vPos.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("{} is not a valid VerticalPosition", vPos);
            }
        }
        Color _fontColor = null;
        if (fontColor != null) {
            // TODO - gross
            try {
                Field field = Class.forName("java.awt.Color").getField(fontColor);
                _fontColor = (Color)field.get(null);
            } catch (Exception e) {
                log.error("{} is not a valid color.", fontColor);
            }
        }
        Integer _fontSize = null;
        if (fontSize != null) {
            try {
                _fontSize = Integer.parseInt(fontSize);
            } catch (NumberFormatException e) {
                log.error("{} is not a valid number for fontSize.", fontSize);
            }
        }
        int _fontAttr = Font.PLAIN;
        if (fontAttr != null) {
            try {
                _fontAttr = FontAttr.valueOf(fontAttr.toUpperCase()).getValue();
            } catch (IllegalArgumentException e) {
                log.error("{} is not a valid font attribute", vPos);
            }
        }
        return ImageBuilder.start(name)
            .width(_width)
            .verticalPosition(_vPos)
            .font(fontFamily, _fontAttr, _fontSize)
            .fontColor(_fontColor)
            .build();
    }

    @GetMapping(value = BADGE_URI, produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getBadge(
        @PathVariable String githubUser, @PathVariable String githubRepo, @PathVariable String badgeSlug,
        @RequestParam(required = false) String width, @RequestParam(required = false) String vPos,
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
            return getImage(badge.getBadgeSlug(), width, vPos, fontColor, fontSize, fontFamily, fontAttr);
        } catch (IOException | InvalidBadgeException e) {
            log.error(
                "Badge: {} not retrieved for {}/{}. Error: {}", badgeSlug, githubUser, githubRepo, e.getMessage(), e
            );
            return notFound;
        }
    }
}

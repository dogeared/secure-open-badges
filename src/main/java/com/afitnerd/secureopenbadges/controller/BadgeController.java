package com.afitnerd.secureopenbadges.controller;

import com.afitnerd.secureopenbadges.exception.InvalidBadgeException;
import com.afitnerd.secureopenbadges.model.Badge;
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

    enum VerticalPosition {
        TOP("top"),
        BOTTOM("bottom"),
        MIDDLE("middle");

        private final String position;

        VerticalPosition(String position) {
            this.position = position;
        }

        String position() {
            return position;
        }
    }

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

    private BufferedImage resizeImage(BufferedImage image, Integer width) {
        if (width == null || width < 1 || width >= image.getWidth()) {
            return image;
        }
        double aspectRatio = (double) width / image.getWidth();
        int height = (int) Math.ceil(image.getHeight() * aspectRatio);

        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        resized.getGraphics().drawImage(scaled, 0, 0, null);
        return resized;
    }

    // TODO - ripe for caching
    private byte[] getImage(String name) throws IOException {
        return getImage(name, null, null, Color.MAGENTA, 100, Font.SANS_SERIF, FontAttr.BOLD);
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
        VerticalPosition _vPos = null;
        if (vPos != null) {
            try {
                _vPos = VerticalPosition.valueOf(vPos.toUpperCase());
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
        String _fontFamily = null;
        if (fontFamily != null) {
            _fontFamily = fontFamily.substring(0,1).toUpperCase() + fontFamily.substring(1).toLowerCase();
        }
        FontAttr _fontAttr = null;
        if (fontAttr != null) {
            try {
                _fontAttr = FontAttr.valueOf(fontAttr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("{} is not a valid font attribute", vPos);
            }
        }
        return getImage(name, _width, _vPos, _fontColor, _fontSize, _fontFamily, _fontAttr);
    }

    private byte[] getImage(
        String name, Integer width, VerticalPosition vPos,
        Color fontColor, Integer fontSize, String fontFamily, FontAttr fontAttr
    ) throws IOException {
        InputStream is = getClass().getResourceAsStream(IMAGES_PATH + "/" + name + ".png");
        if (is == null) { throw new IOException("Image Not Found!"); }
        BufferedImage bi = ImageIO.read(is);

        if (fontColor == null) {
            fontColor = Color.BLACK;
        }

        if (fontSize == null) {
            fontSize = 100;
        }

        if (
            !Font.SANS_SERIF.equals(fontFamily) && !Font.SERIF.equals(fontFamily) &&
            !Font.MONOSPACED.equals(fontFamily) && !Font.DIALOG.equals(fontFamily) &&
            !Font.DIALOG_INPUT.equals(fontFamily)
        ) {
            fontFamily = Font.SANS_SERIF;
        }

        if (fontAttr == null) {
            fontAttr = FontAttr.PLAIN;
        }

        // transformations here
        Font font = new Font(fontFamily, fontAttr.getValue(), fontSize);

        String text = new Date().toString();
        AttributedString attributedText = new AttributedString(text);
        attributedText.addAttribute(TextAttribute.FONT, font);
        attributedText.addAttribute(TextAttribute.FOREGROUND, fontColor);

        Graphics g = bi.getGraphics();

        FontMetrics metrics = g.getFontMetrics(font);
        GlyphVector vector = font.createGlyphVector(metrics.getFontRenderContext(), text);
        Shape outline = vector.getOutline(0, 0);
        double expectedWidth = outline.getBounds().getWidth();
        double expectedHeight = outline.getBounds().getHeight();
        boolean textFits = bi.getWidth() >= expectedWidth && bi.getHeight() >= expectedHeight;
        if (!textFits) {
            double widthBasedFontSize = ((font.getSize2D()*bi.getWidth())/expectedWidth)-10;
            double heightBasedFontSize = ((font.getSize2D()*bi.getHeight())/expectedHeight)-10;

            double newFontSize = Math.min(widthBasedFontSize, heightBasedFontSize);
            font = font.deriveFont(font.getStyle(), (float)newFontSize);
            metrics = g.getFontMetrics(font);
            attributedText = new AttributedString(text);
            attributedText.addAttribute(TextAttribute.FONT, font);
            attributedText.addAttribute(TextAttribute.FOREGROUND, fontColor);
        }

        int positionX = (bi.getWidth() - metrics.stringWidth(text)) / 2;
        int positionY = (bi.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        if (vPos == VerticalPosition.TOP) {
            positionY = metrics.getAscent();
        } else if (vPos == VerticalPosition.BOTTOM) {
            positionY =  bi.getHeight() - metrics.getHeight() + metrics.getAscent();
        }

        g.drawString(attributedText.getIterator(), positionX, positionY);

        bi = resizeImage(bi, width);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi , "png", baos);

        return baos.toByteArray();
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

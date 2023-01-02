package com.afitnerd.secureopenbadges.service;

import com.afitnerd.secureopenbadges.model.ImageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Locale;

@Service
public class ImageServiceImpl implements ImageService {

    private final Logger log = LoggerFactory.getLogger(ImageServiceImpl.class);

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

    // TODO - ripe for caching
    @Override
    public byte[] getImage(String name) throws IOException {
        return getImage(
            name, null, null,
            null, null,
            null, null, null, null, false
        );
    }

    private ImageBuilder.VerticalPosition deriveVerticalPosition(String vPos) {
        ImageBuilder.VerticalPosition _vPos = null;
        if (vPos != null) {
            try {
                _vPos = ImageBuilder.VerticalPosition.valueOf(vPos.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("{} is not a valid VerticalPosition", vPos);
            }
        }
        return _vPos;
    }


    // TODO - ripe for caching
    @Override
    public byte[] getImage(
        String name, String githubUser, String width, String datePosition, String userPosition,
        String fontColor, String fontSize, String fontFamily, String fontAttr, boolean shouldShowDate
    ) throws IOException {
        Integer _width = null;
        if (width != null) {
            try {
                _width = Integer.parseInt(width);
            } catch (NumberFormatException e) {
                log.error("{} is not a valid number for width.", width);
            }
        }
        ImageBuilder.VerticalPosition _datePosition = deriveVerticalPosition(datePosition);
        ImageBuilder.VerticalPosition _userPosition = deriveVerticalPosition(userPosition);
        Color _fontColor = null;
        if (fontColor != null) {
            // TODO - gross
            try {
                Field field = Class.forName("java.awt.Color").getField(fontColor.toUpperCase());
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
                log.error("{} is not a valid font attribute", fontAttr);
            }
        }
        return ImageBuilder.start(name)
            .githubUser(githubUser)
            .width(_width)
            .datePosition(_datePosition)
            .userPosition(_userPosition)
            .font(fontFamily, _fontAttr, _fontSize)
            .fontColor(_fontColor)
            .shouldShowDate(shouldShowDate)
            .build();
    }
}

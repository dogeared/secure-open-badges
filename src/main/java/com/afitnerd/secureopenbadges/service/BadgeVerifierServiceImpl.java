package com.afitnerd.secureopenbadges.service;

import com.afitnerd.secureopenbadges.exception.InvalidBadgeException;
import com.afitnerd.secureopenbadges.model.Badge;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BadgeVerifierServiceImpl implements BadgeVerifierService {

    @Override
    public Badge verify(List<Badge> badges, String badgeSlug) throws InvalidBadgeException {
        Badge badge = badges.stream().filter(b -> b.getBadgeSlug().equals(badgeSlug)).findAny()
            .orElseThrow(() -> new InvalidBadgeException("Badge not found: " + badgeSlug));
        // TODO - check date range
        return badge;
    }
}

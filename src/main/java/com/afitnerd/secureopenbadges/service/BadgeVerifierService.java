package com.afitnerd.secureopenbadges.service;

import com.afitnerd.secureopenbadges.exception.InvalidBadgeException;
import com.afitnerd.secureopenbadges.model.Badge;

import java.util.List;

public interface BadgeVerifierService {

    Badge verify(List<Badge> badges, String badgeSlug) throws InvalidBadgeException;
}

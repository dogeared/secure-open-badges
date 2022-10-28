package com.afitnerd.secureopenbadges.service;

import com.afitnerd.secureopenbadges.exception.InvalidBadgeException;
import com.afitnerd.secureopenbadges.model.Badge;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.util.List;

public interface GithubService {


    GHRepository getRepository(String username, String repo) throws IOException;
    GHCommit getLatestVerifiedCommit(GHRepository repository) throws IOException, InvalidBadgeException;
    List<Badge> getBadge(GHCommit commit, String username, String repo) throws IOException;
}

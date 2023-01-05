package com.afitnerd.secureopenbadges.service;

import com.afitnerd.secureopenbadges.exception.InvalidBadgeException;
import com.afitnerd.secureopenbadges.model.Badge;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static com.afitnerd.secureopenbadges.config.Constants.BADGES_FLE_NAME;
import static com.afitnerd.secureopenbadges.config.Constants.GITHUB_RAW_URL;

@Service
public class GithubServiceImpl implements GithubService {

    @Value("#{ @environment['github.api.token'] }")
    private String githubAPIToken;

    @Value("#{ @environment['github.authorized-verifiers'] }")
    private List<String> authorizedVerifiers;

    private final Logger log = LoggerFactory.getLogger(GithubServiceImpl.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeReference<List<Badge>> badgeListType = new TypeReference<List<Badge>>() {};

    private GitHub github;

    @PostConstruct
    public void setup() {
        try {
            github = new GitHubBuilder().withOAuthToken(githubAPIToken).build();
        } catch (IOException e) {
           log.error("Error creating Github client: {}", e.getMessage(), e);
        }
    }

    @Override
    public GHRepository getRepository(String username, String repo) throws IOException {
        return github.getRepository(username + "/" + repo);
    }

    @Override
    public GHCommit getLatestVerifiedCommit(GHRepository repository) throws IOException, InvalidBadgeException {
        String defaultBranch = repository.getDefaultBranch();
        PagedIterable<GHCommit> commits = repository.listCommits();
        for (GHCommit commit : commits) {
            if (!authorizedVerifiers.contains(commit.getAuthor().getLogin())) {
                continue;
            }
            if (commit.getCommitShortInfo().getVerification().isVerified()) {
                return commit;
            }
        }
        throw new InvalidBadgeException("Signed badge not found");
    }

    @Override
    public List<Badge> getBadge(GHCommit commit, String username, String repo) throws IOException {
        InputStream is = new URL(
            GITHUB_RAW_URL + "/" + username + "/" + repo + "/" +
            commit.getSHA1() + "/" + BADGES_FLE_NAME
        ).openStream();
        return mapper.readValue(is, badgeListType);
    }
}

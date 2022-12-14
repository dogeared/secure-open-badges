## Secure Open Badges

NOTE: This repo is currently in the POC stage

Achievement badges are a fun way to show the world that you've accomplished something.

Existing badging services can be very costly.

This open source project is an effort to create a badging system that offers a high degree of credibility via
cryptographically provable assets.

Don't worry - this isn't another Blockchain project! Instead, this project leverages the commit signing capabilities
of Github.

## Requirements

### Roles & Terms

| Name | Description |
|------|-------------|
| badge requester | someone who has earned a badge |
| badge org | a github organization whose members are authorized to issue badges |
| badge provider | a github user who is authorized to issue badges | 
| badge slug | a unique identifier for a badge |

### Badge Requester

As a badge requester, you need a Github account. You create a fresh, public repo 
(if your badge repo doesn't already exist) - with a name of your choice - and request that the badge provider issue you 
a badge based on a badge slug.

### Badge Provider

As a badge provider, you need a Github account. You process incoming badge requests by forking a repo, adding or 
editing a `badges.json` file  and signing your commit. You create a PR and once merged, the badge is ready to be
referenced.

### Referencing a Badge

Badges are ultimately images that are returned (after validation) from a running instance of this project.

The basic format is:

```
https://<base url>/api/v1/user/<github user>/repo/<repo name>/badge/<badge slug>
```

The following query string parameters are supported:

| param | acceptable values | default |
|-------|-------------------|---------|
| fontFamily | serif, sans_serif, monospaced, dialog, dialog_input | sans_serif |
| fontSize | integer > 1  | 80 |
| fontColor | white, light_gray, gray, dark_gray, black, red, pink, orange, yellow, green, magenta, cyan, blue | black |
| fontAttr | plain, bold, italic, bold_italic | plain |
| width | integer > 1 | default width of badge image |

All of the query string parameters are optional. Here's an example:

```
http://localhost:8080/api/v1/user/afitnerd/repo/badge-test/badge/participation-trophy?fontColor=black&fontFamily=monospaced&fontSize=200&fontAttr=bold_italic&width=500
```

The following checks are done before returning the badge:

1) Does the repository exist for the named user?
2) Is there a commit from the list of authorized committers?
3) Is the commit verified (implying that it was properly signed)
4) Is there a `badges.json` file in the repo?
5) Is there a badge in the list of badges that matches the referenced `badge slug`?

If any of the above checks fails, the issue is logged and a generic `404` badge image is returned.

If all the checks pass, an image that matches the `badge slug` is returned.

## Building and Running the App

Execute the following to build the app:

```
./mvnw clean install
```

In order to run the app, you'll need two properties:

| property | description |
|----------|-------------|
| `github.api.token` | A Github API token |
| `github.authorized-verifiers` | A list of Github login names authorized to issue badges |

Here's an example of running the application from the command line:

```
GITHUB_API_TOKEN=<token> \
GITHUB_AUTHORIZED_VERIFIERS="<login 1>, <login 2>, ... <login n>" \
./mvnw spring-boot:run 
```

## Badge Provider Setup

1) Create an organization in Github
2) Add a verified domain
3) Add Github users to the organization
4) Create a GPG key
5) Import the key into your Github account

TODO - Add more detail here

## Badge Provider Signing Process

1) Fork the repo created by the badge requester
2) add or edit a file called `badges.json` (see format below)
3) commit the file WITH signature AND `on-behalf-of` as follows:

```
git commit -S -m "added rookie badge


on-behalf-of: @{organization name} <email address in verified domain>"
```

Notice that there are newlines in the commit comment. The use of `on-behalf-of` triggers a beta feature in Github
that gives a visual indication that the committer is a member of the organization. More information can be found at
[here](https://docs.github.com/en/pull-requests/committing-changes-to-your-project/creating-and-editing-commits/creating-a-commit-on-behalf-of-an-organization)

## `badges.json` format

Below is an example of a `badges.json` file:

```
[
    { "badge-slug": "stranger-danger-ranger-rookie", "start-date": "2022-10-24T00:00:00.000Z" },
    ...
    { "badge-slug": "stranger-danger-ranger-advanced", "start-date": "2022-10-24T00:00:00.000Z" }
]
```

Each record in the file should have these keys:

| key | required | description |
|-----|----------|-------------|
| `badge-slug` | yes | the unique identifier for the badge |
| `start-date` | yes | when the badge was issued |
| `end-date`   | no  | when the badge expires |

## Issues & Concerns

* Perhaps an authorized user shouldn't be able to issue a badge to themself. This could be managed by making sure
the owner and committer are not the same, but that the committer is still in the list of authorized verifiers
* What if an authorized verifier is no longer part of the organization? What if they delete their github account? On
one hand, a new signed commit could be made from a current authorized verifier. It might be useful to have a set of
dates indicating start and end date of service for an authorized verifier.
* It should be easier to add new badges without having to rebuild and redeploy the app
* The Java image library as it's currently used does not preserve the transparent background of a png when overlaying username and date
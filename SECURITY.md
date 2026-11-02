# Security policy

## Reporting a vulnerability

**Please do not open a public issue for a suspected vulnerability.**

Report it privately through GitHub instead: open the **Security and quality** tab of this
repository (labelled **Security** in older GitHub layouts) and choose **Report a
vulnerability**. The report is visible only to you and the
maintainers. We can discuss it there, fix it in a private fork, and publish an advisory
(with a CVE where one is warranted) once a fixed release is available.

If you do not have a GitHub account, use the contact form at
<https://mcpdbwizard.com/contact/> and choose "Something else" as the topic. Say it is a
security report and we will move the conversation somewhere private.

## What to include

- The MCPDBWizard version (the Docker image tag, or the release you built from).
- The Oracle version, if the issue involves the database (`select banner from v$version`).
- What an attacker can do, and what they need first: network access to the MCP endpoint,
  a valid bearer token, an account on the web console, control of a config file, or an
  agent's prompt.
- Steps or a minimal config that reproduce it.

## Scope

In scope: the generator, the MCP servers it emits, the runtime library they link
against, the web console (Design and Runtime) and the published Docker image.

Two behaviours are deliberate and are not vulnerabilities in themselves:

- **What a config selects is exposed as written.** A selected `delete` tool deletes. The
  config is the authorization boundary; an object that is not selected has no code
  generated for it at all.
- **Every caller of one generated server shares that server's Oracle account.** Its
  grants cap what any tool can do, so give it least privilege.

A way to reach an object that was *not* selected, to supply statement text rather than
bind values, or to bypass the bearer token, the web console's login or its access matrix
is exactly what we want to hear about.

## Supported versions

Fixes are made on the latest release. Please check that the issue reproduces there
before reporting.

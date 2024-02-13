<div align="center">

# Keepup
[![](https://img.shields.io/github/v/release/MineInAbyss/Keepup)](https://github.com/MineInAbyss/Keepup/releases/)
[![Contribute](https://shields.io/badge/Contribute-e57be5?logo=github%20sponsors&style=flat&logoColor=white)](https://wiki.mineinabyss.com/contributing/)
</div>

Keeps Minecraft server dependencies up to date based on a hocon/json definition.
Keepup downloads all relevant files using [rclone remotes](https://rclone.org/overview/), URLs, or GitHub artifacts in
the config, then creates symlinks to a desired folder.

![Keepup Preview](https://github.com/MineInAbyss/Keepup/assets/16233018/d1513138-b683-4e54-9525-66b6b5ee0efd)

## Features

- Download all files in parallel, caching them by last updated/filesize headers for very fast downloads.
- Format configs nicely and reuse across multiple servers on a network thanks to HOCON's substitutions,
  and `--json-path` option.
- Automatically find the latest release for GitHub artifacts, or force it with `--override-github-release`.
- Support for GitHub authorization tokens to bypass ratelimits or access private repos.
- Use any service supported by rclone! (Google Drive, S3, etc...)

## Usage

- Download latest release and unzip it, ensure Java is installed on your system.
- Run Keepup in the `bin` file or add it to your path.

```yaml
Usage: keepup [OPTIONS] INPUT DOWNLOADPATH DEST

Options:
  --json-path TEXT                 Path to the root object to download from,
  uses keys separated by .
  --file-type [json|hocon]         Type of file for the input stream
  --ignore-similar                 Don't create symlinks for files with
                                   matching characters before the first number
  --fail-all-downloads             Don't actually download anything, useful
  for testing
  --hide-progress-bar              Does not show progress bar if set to true
  --override-github-release [NONE|LATEST_RELEASE|LATEST]
                                   Force downloading the latest version of
                                   files from GitHub
  --cache-expiration-time VALUE
  --github-auth-token TEXT         Used to access private repos or get a
                                   higher rate limit
  -h, --help                       Show this message and exit

Arguments:
  INPUT         Path to the file
  DOWNLOADPATH  Path to download files to
  DEST
```

### Config
The config can be a nested, with the only requirement being that all leaf nodes are strings, ex:
```hocon
general {
  plugins {
    dep1: "https://..."
    dep2: "https://..."
  }
  dep3: "https://..."
}
```

### Nodes
Each node may be:

- An http(s) url
- `github:owner/repo:version:artifactRegex` to download a GitHub release, supports `latest` for version.
- An rclone remote (defined in the global rclone config or manually) and path `remotename:path/to/file`
- A `.` to ignore a dependency (useful with HOCON)

### HOCON features
HOCON's [substitutions](https://github.com/lightbend/config#uses-of-substitutions) feature is useful for creating bundles of plugins that you can inherit from.

## Our usecases

### Production servers

We have one config file with all our dependencies and use the `--json-path` option to narrow it down to the currently running server. We run the script on startup in a Docker image.

### Dev environment

In a separate dev Docker container, on startup we:
- Download our production config from GitHub
- Merge this config with a `local.conf`
- Run keepup with `--json-path local` so devs can quickly emulate production environments or pick and choose bundles

<div align="center">

# Keepup
[![](https://img.shields.io/github/v/release/MineInAbyss/Keepup)](https://github.com/MineInAbyss/Keepup/releases/)
[![Contribute](https://shields.io/badge/Contribute-e57be5?logo=github%20sponsors&style=flat&logoColor=white)](https://wiki.mineinabyss.com/contributing/)
</div>
Keeps dependencies up to date based on a hocon/json definition.

The tool downloads all relevant files using [rclone remotes](https://rclone.org/overview/) or URLs defined in the config, then creates symlinks to a desired folder, removing old ones.

## Usage

Download latest release and unzip it, ensure Java is installed on your system.

```yaml
Usage: keepup [OPTIONS] INPUT DOWNLOADPATH DEST

Options:
  --json-path TEXT          JsonPath to the root value to keep
  --file-type [json|hocon]  Type of file for the input stream
  --ignore-similar          Don't create symlinks for files with matching
                            characters before the first number
  -h, --help                Show this message and exit

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
- An https url
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

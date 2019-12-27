# Jenkins Wake on LAN Agent plugin

A plugin for Jenkins CI to start agents with Wake on LAN functionality

## Development

### Setup
Before starting to develop for this plugin and for every time you add or remove a localization key, you have to execute
the following command to generate some Java classes:

```
gradlew localizer
```

### Build
To build the plugin *jpi* file, simply run the following Gradle command:

```
gradlew jpi
```
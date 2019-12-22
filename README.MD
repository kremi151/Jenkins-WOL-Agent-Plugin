# Jenkins Wake on LAN Agent plugin

A plugin for Jenkins CI to start agents with Wake on LAN functionality

## Development

### Setup
The development requires some manual tasks to get started:
1. Download the latest *.hpi* artifact for the *ssh-slaves* Jenkins plugin
2. Open the *hpi* file with an archive manager (e.g. 7-Zip)
3. Extract *WEB_INF/lib/ssh-slaves.jar* into *lib/ssh-slaves.jar*
4. Refresh the Gradle project

### Build
To build the plugin *hpi* file, simply run the following Gradle command:

gradlew hpi
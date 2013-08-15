# Steamshots
Steamshots is a Steam screenshot uploader for Android.

For details, see the [Google Play page](https://play.google.com/store/apps/details?id=com.steamcommunity.siplus.steamscreenshots).

# Building
The source code comes as an ADT Eclipse Juno project. Just copy the SteamScreenshots folder to your workspace and press Debug or Run.

To modify the Steam protobuf messages (located in `/proto`), you need the Protocol Buffers addon for Eclipse and the protobuf compiler `protoc`. In the Protocol Buffers settings in Eclipse, enable Java output to `src` folder.

## Installing Protocol Buffers
To install the Protocol Buffers editor to compile the protobuf messages, you need to do the following steps:

1. Install Xtext-Antlr 2.0.0 from `http://download.itemis.de/updates/` Eclipse repo.
2. Install the latest version of Xtext from `http://download.eclipse.org/modeling/tmf/xtext/updates/composite/releases/` Eclipse repo.
3. Install everything from `http://protobuf-dt.googlecode.com/git/update-site` Eclipse repo.
4. Download the Protobuf compiler `protoc`.
5. In Protocol Buffers editor options in workspace or project settings, set the path to `protoc` and enable Java output to `src` directory.

Clean the project to build the protobuf messages.

# Used libraries
* [Guava](http://code.google.com/p/guava-libraries) by Google.
* [Protocol Buffers](http://code.google.com/p/protobuf) by Google.
* [TouchImageView](https://github.com/MikeOrtiz/TouchImageView) by [MikeOrtiz](https://github.com/MikeOrtiz).

# Links
* [Google Play](https://play.google.com/store/apps/details?id=com.steamcommunity.siplus.steamscreenshots)
* [Steam Forums](http://forums.steampowered.com/forums/showthread.php?p=34678860)
* [Steam Discussions](http://steamcommunity.com/discussions/forum/8/864973577799365388)
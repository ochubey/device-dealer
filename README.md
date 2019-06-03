[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/1da616b4ab1e4bce8e661925c19fd6cb)](https://www.codacy.com/app/chubej/Device-Dealer?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ochubey/Device-Dealer&amp;utm_campaign=Badge_Grade)
![Device Dealer icon](https://raw.githubusercontent.com/ochubey/Device-Dealer/master/src/main/resources/device_dealer.png)
# Device-Dealer
Device management service that will help you to run Appium tests in parallel with real devices in your local lab.

![Device Dealer example screenshot](https://raw.githubusercontent.com/ochubey/Device-Dealer/master/src/main/resources/dd_example_screen.png)

## Service compilation
Before running service, please update config.properties according to your needs:

| Configuration value | Purpose |
| --- | --- |
|DEVICE_POOL_TIMEOUT=1000 | Time in milliseconds to check if device execution state has been changed |
|BLACK_LIST_BUNDLE_IDS=WebDriverAgentRunner,appium| List of comma separated bundle ids of application that need to be removed from the device after it was connected to service |
|DEVELOPMENT_TEAM=0000000000| Unique id of your team that could be found on https://developer.apple.com/account and needed for execution of Appium tests on real iOS devices. [Please refer to Appium documentation for more information].|
|PRODUCT_BUNDLE_IDENTIFIER=com.facebook.WebDriverAgentRunner| Default value of WDA bundle. In some cases need to be updated. [Please refer to Appium documentation for more information].|
|CODE_SIGN_IDENTITY=iPhone Developer| Another option that needed for WebDriverAgentRunner to be installed on iOS devices. [Please refer to Appium documentation for more information].|
|USE_EMULATORS=true | Flag will tell service if Android Emulators should be ignored during Android devices search in your system.|
 Configuration value | Purpose 
 --- | --- 
DEVICE_POOL_TIMEOUT=1000 | Time in milliseconds to check if device execution state has been changed 
BLACK_LIST_BUNDLE_IDS=WebDriverAgentRunner,appium| List of comma separated bundle ids of application that need to be removed from the device after it was connected to service 
DEVELOPMENT_TEAM=0000000000| Unique id of your team that could be found on [Apple Developer Account](https://developer.apple.com/account) and needed for execution of Appium tests on real iOS devices. [read more].
PRODUCT_BUNDLE_IDENTIFIER=com.facebook.WebDriverAgentRunner| Default value of WDA bundle. In some cases need to be updated. [read more].
CODE_SIGN_IDENTITY=iPhone Developer| Another option that needed for WebDriverAgentRunner to be installed on iOS devices. [read more].
USE_EMULATORS=true | Flag will tell service if Android Emulators should be ignored during Android devices search in your system.

In order to run service from code execute following command from the root folder of the project:
```sh 
./gradlew clean bootRun
``` 

In order to get distributable .jar file execute following command from the root folder of the project:
```sh
./gradlew clean build
```
Then navigate to /build/libs/ folder and copy dd-\<version>.jar file. Jar file can be exeucted on workstation with JRE using basic command:
```bash
java -jar dd-<version>.jar
```
## Device-Dealer usage
Device-Dealer provides information about device via API. Currently there is an Agent for Java based Appium frameworks that provide Appium Server URL by pooling for idle device of specific platform.

In order to establish connection between Device Dealer and Appium Driver, please, use following steps:
1. add latest [device-java-agent] dependency to your build file;
2. use code bellow (snippet#1) to book available device and get Appium Server URL before Driver initialization.

 [![](https://jitpack.io/v/org.bitbucket.ochubey/device-java-agent.svg)](https://jitpack.io/#org.bitbucket.ochubey/device-java-agent)

### Code snippets
Snippet#1: by default Device Dealer will be executed on localhost:8989 and agent will check for free device 360 times every 10 seconds and if device would not be found - nothing would be returned.
Both host and lookup strategy could be changed based on infrastructure needs in further releases.

```java
public class AppiumTest{
    private static Device device;
    private static DeviceDealerAgent deviceDealerAgent = new DeviceDealerAgent();
    
    private void initDriver(String platform) {
        DesiredCapabilities desiredCapabilities = DEVICE_CAPABILITIES.getDesiredCapabilities();
        
        device = deviceDealerAgent.bookDevice(platform);
        assertNotNull("Unable to find free device after 60 minutes or Device Dealer is unreachable", device);
    
        if (platform.equalsIgnoreCase("ios")) {
            driver = new IOSDriver(device.getAppiumServerUrl(), desiredCapabilities) {};
        } else if (platform.equalsIgnoreCase("android")) {
            driver = new AndroidDriver(device.getAppiumServerUrl(), desiredCapabilities) {};
        }
        assert driver != null;
    }
}
```

In case if non-defaults settings are needed - both service location and pooling frequency/timeout could be changed:
```java
public class AppiumTest{
    //Agent will try to communicate with service with endpoints on 192.168.1.1:9999
    DeviceDealerAgent deviceDealerAgent = new DeviceDealerAgent("192.168.1.1", 9999);
    
    private void initDriver(String platform) {
        //Agent will try to get idle device 10 times every 10 seconds
        deviceDealerAgent.bookDevice(platform, 10, 60000);
    }
}
``` 

## Initial setup of your system
Presumably your environment is ready for test execution at least on one Android and one iOS device.

Just to make sure that all dependencies and prerequisites are meat - device dealer will check basic configuration of your system. Please follow error messages, if any appear during service execution, and restart service after each issue was resolved.  

### Xcode
Install latest Xcode either from the Mac App Store or the developer pages
Make sure that Xcode is selected and can be used from terminal:
```sh
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
```
### Homebrew
Install Homebrew by executing the following in a terminal
```sh
ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
```
### node.js
Download and install [node.js]. Make sure node is available on your $PATH.

To install using homebrew run:
```sh
brew install node
```
### Other dependencies
Install all locally managed node modules:
```sh
npm install
```
Authorise iOS Instrumentation (needed after Xcode install/update):
```sh
npm run authorize-ios
```
Install ideviceinstaller and carthage for Appium to install application and WDA. Also ios-deploy is needed globaly in order to install applications to the real devices.
```sh
brew install ideviceinstaller
brew install carthage
npm install -g ios-deploy
```
### Install and configure Android SDK
[Download Android SDK] and follow installation instructions to configure it.
Make sure that your PATH, ANDROID_HOME, JAVA_HOME are properly configured.

Follow instructions from Appium Doctor to configure workstation configuration:
```sh
npm run doctor
```

<div>Icons made by <a href="http://www.freepik.com" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a> is licensed by <a href="http://creativecommons.org/licenses/by/3.0/" title="Creative Commons BY 3.0" target="_blank">CC 3.0 BY</a></div>

[node.js]:http://nodejs.org/download/
[Download Android SDK]:http://developer.android.com/sdk/installing/index.html
[device-java-agent]:https://jitpack.io/#org.bitbucket.ochubey/device-java-agent
[read more]:http://appium.io/docs/en/drivers/ios-xcuitest-real-devices/

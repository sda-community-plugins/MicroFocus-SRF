# Micro Focus Storm Runner Functional (SRF) plugin

The _Micro Focus Storm Runner Functional (SRF) plugin is a quality automation based plugin. 
It is run during development and deployment process to automate the execution of functional tests.

This plugin is a work in progress but it is intended to provide the following steps:

* [x] **Run Test** - Run a functional test
* [x] **Check Status** - Check the status of a functional test run
* [x] **Publish Results to HTML** - Publish the results of a test run to a file in HTML format

### Installing the plugin
 
Download the latest version from the _release_ directory and install into Deployment Automation from the 
**Administration\Automation\Plugins** page.

### Building the plugin

To build the plugin you will need to clone the following repositories (at the same level as this repository):

 - [mavenBuildConfig](https://github.com/sda-community-plugins/mavenBuildConfig)
 - [plugins-build-parent](https://github.com/sda-community-plugins/plugins-build-parent)
 - [air-plugin-build-script](https://github.com/sda-community-plugins/air-plugin-build-script)
 
 and then compile using the following command:
 ```
   mvn clean package
 ```  

This will create a _.zip_ file in the `target` directory when you can then install into Deployment Automation
from the **Administration\Automation\Plugins** page.

If you have any feedback or suggestions on this template then please contact me using the details below.

Kevin A. Lee

kevin.lee@microfocus.com

**Please note: this plugins is provided as a "community" plugin and is not supported by Micro Focus in any way**.

# ReqIF extension for [Requirement Yogi](https://www.requirementyogi.com/)

This is a demo of Requirement Yogi's [Public API](https://docs.requirementyogi.com/). Feel free to clone and try it.
This is the source code of the app named "RY extension - ReqIF" on the Atlassian Marketplace.

# How to create my own importer?

You can create your own Requirement Yogi importer for XML, ReqIF, Caliber, HPQC, any format!

 - Clone this repository,
 - Change the <groupId> and <artifactId> in the pom.xml (only if you want to create your own importer),
 - Download the Atlassian SDK,
 - Use: `atlas-mvn clean install` to compile this project,
 - The file to install in Confluence is the `target/___.jar` file.

Tips:
 - You can launch Confluence using the Atlassian SDK's commands (certainly atlas-run or atlas-standalone, we haven't checked it),
 - If you've launched Confluence using the SDK, you don't even have to upload the file to Confluence! If it is in
   the same directory where you've launched the Atlassian SDK command, it will monitor the current directory and
   reinstall the .jar, as long as its version is above.

# Can I contribute ?

Sure, as long as you accept the license. Send us a pull request.

# Is it open-source ?

Yes. The license is APL v2. It is business-friendly, meaning you can keep your project internally, or publish it
as part of a closed-source project (as long as you publish the infos required by the license), etc.

The license text should be available [in this repository](src/license/LICENSE.txt)
 or (on the Apache website](https://www.apache.org/licenses/LICENSE-2.0)
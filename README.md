#
# This is the Scientist Info Console for upload indexing xlsx files and reindexing for data updating.
#

1. Do Maven build, and will package war in the target folder
mvn clean install

#OR

mvn clean package

2. For deployment, copy the genetated console.war to tomcat webapps folder, and start the tomcat

3. For properties, copy upload.properties and update.properties in the folder properties to C:\property

#
# This is the Scientist Info Console for upload indexing xlsx files and reindexing for data updating.
#

# Do Maven build, and will package war in the target folder
mvn clean install
mvn clean package

# For deployment
# copy the genetated console.war to tomcat webapps folder, and start the tomcat

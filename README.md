hibernate4-dll-maven-plugin
===========================

A maven plugin which generates ddl using hibernate 4

Usage
=====

Run `mvn install` in the plugin directory

Add these lines to ~/.m2/settings.xml :

    <settings>
	...
    <pluginGroups>
		<pluginGroup>com.aziphael</pluginGroup>
	</pluginGroups>
	...
	<settings>

Add the plugin to your project :

    <plugin>
		<groupId>com.aziphael</groupId>
		<artifactId>hibernate4-dll-maven-plugin</artifactId>
		<configuration>
			<entityPackage>com.aziphael.modular.persistence.model</entityPackage>
			<!-- Generate drop queries ? Default is false -->
			<drop>false</drop>
			<!-- Generate create queries ? Default is true -->
			<create>true</create>
			<dialect>org.hibernate.dialect.MySQL5Dialect</dialect>
			<!-- Export file. Default is ${project.build.directory}/generated-sources/sql/schema-export.sql -->
			<exportFile>${project.build.directory}/sql/export.sql</exportFile>
		</configuration>
	</plugin>
	
Run `mvn hibernate4-dll:export` and voilà !
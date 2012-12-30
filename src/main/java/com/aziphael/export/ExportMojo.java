package com.aziphael.export;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.reflections.Reflections;

/**
 * Goal which exports a database schema using hibernate 4.
 * 
 * @author Geoffroy Warin (https://github.com/geowarin)
 * 
 * @goal export
 * @phase process-classes
 * @requiresOnline false
 * @requiresDependencyResolution
 */
public class ExportMojo extends AbstractMojo {
	
	/**
	 * The package containing the entities.
	 * @parameter expression="${export.entityPackage}"
	 * @required
	 */
	private String entityPackage;
	
	/**
	 * Hibernate dialect to use.
	 * @parameter expression="${export.dialect}"
	 * @required
	 */
	private String dialect;
	
	/**
	 * Location of the file.
	 * 
	 * @parameter 
	 * 		expression="${export.exportFile}"
	 * 		default-value="${project.build.directory}/generated-sources/sql/schema-export.sql"
	 * @required
	 */
	private String exportFile;
	
	/**
     * Generate drop script.
     *
     * @parameter default-value="true"
     */
    private boolean drop;
    
    /**
     * Generate create script.
     *
     * @parameter default-value="true"
     */
    private boolean create;
    
    /**
	 * The project currently being built.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	public void execute() throws MojoExecutionException {
		
		File export = new File(exportFile);
		getLog().info("exportFile = " + export.getAbsolutePath());
		getLog().info("entityPackage = " + entityPackage);
		getLog().info("dialect = " + dialect);
		
		PrintWriter writer = null;
		try  {
			
			if (!export.getParentFile().exists())
				export.getParentFile().mkdirs();
			
			// Adds project classes and jar dependencies to the classpath
			URL[] urls = getCompileClasspathElementsURLs();
			URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
			Thread.currentThread().setContextClassLoader(classLoader);

			// Creates hibernate config with annotated classes and provided dialect
			Configuration hibernateConfiguration = createHibernateConfig();
			
			writer = new PrintWriter(export);
			
			if (drop) {
				String[] dropSQL = hibernateConfiguration.generateDropSchemaScript(Dialect.getDialect(hibernateConfiguration.getProperties()));
				writeToFile(writer, dropSQL);
			}
			
			if (create) {
				String[] createSQL = hibernateConfiguration.generateSchemaCreationScript(Dialect.getDialect(hibernateConfiguration.getProperties()));
				writeToFile(writer, createSQL);
			}
			
		} catch (FileNotFoundException e) {
			
			throw new MojoExecutionException("Could not create export file", e);
			
		} finally {
			if (writer != null)
				writer.close();
		}
	}


	private URL[] getCompileClasspathElementsURLs() throws MojoExecutionException {
		
		try {
			
			@SuppressWarnings("unchecked")
			List<String> compileClasspathElements = project.getCompileClasspathElements();
			URL[] urls = new URL[compileClasspathElements.size()];
			int i = 0;
			for (String element : compileClasspathElements) {
				urls[i++] = new File(element).toURI().toURL();
			}
			
			return urls;
			
		} catch (DependencyResolutionRequiredException e) {
			throw new MojoExecutionException("Could not resolve dependencies", e);
		} catch (MalformedURLException e) {
			throw new MojoExecutionException("MalformedURLException", e);
		} 
	}
	
	private Configuration createHibernateConfig() {
		
		Configuration hibernateConfiguration = new Configuration();

		final Reflections reflections = new Reflections(entityPackage);
		for (Class<?> cl : reflections.getTypesAnnotatedWith(MappedSuperclass.class)) {
			hibernateConfiguration.addAnnotatedClass(cl);
			getLog().info("Mapped = " + cl.getName());
		}
		for (Class<?> cl : reflections.getTypesAnnotatedWith(Entity.class)) {
			hibernateConfiguration.addAnnotatedClass(cl);
			getLog().info("Mapped = " + cl.getName());
		}
		hibernateConfiguration.setProperty(AvailableSettings.DIALECT, dialect);
		return hibernateConfiguration;
	}

	private void writeToFile(PrintWriter writer, String[] lines) {

		Formatter formatter = FormatStyle.DDL.getFormatter();
		for (String string : lines)
			writer.println(formatter.format(string));
	}
}

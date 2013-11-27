package pl.net.ptak;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import pl.net.ptak.helpers.LoggerImplementation;

/**
 * @author Tomasz Ptak
 */
@Mojo( name = "prepare-dependencies", requiresDependencyResolution = ResolutionScope.COMPILE )
public class PrepareDependenciesMojo
    extends AbstractMojo
{

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    /**
     * The folder in which data will be placed for packaging
     */
    @Parameter( defaultValue = "${project.build.directory}/dependency", property = "dependencyFolder",
                    readonly = true, required = true )
    private File dependencyFolder;

    /**
     * A folder holding static files to be provided as dependencies for the project.<br>
     * These files are copied to ${staticFilesTargetFolder} with their structure preserved. <br>
     * Useful when you want to maintain a prerequisite with an exe file only, or with bat and exe.<br>
     * These are different from standard resources as they are copied to target directory and will make it to prz
     * archive within the ${artifactId}/static subfolder.<br>
     * The rule of thumb here is to refer to files in ${staticFilesTargetFolder} when working with a prerequisite file.
     */
    @Parameter( defaultValue = "${project.source.directory}/static", property = "staticFilesFolder",
                    readonly = true, required = true )
    private File staticFilesFolder;

    /**
     * A folder holding static files within the target directory.
     */
    @Parameter( defaultValue = "${project.build.directory}/static", property = "staticFilesTargetFolder",
                    readonly = true, required = true )
    private File staticFilesTargetFolder;

    /**
     * List of artifact to unzip. This is done by simple startsWith comparison on Artifact identifier in form
     * groupId:artifactId:type:version. If you enter an empty unzip, or the list is empty or not provided, nothing will
     * be extracted.
     */
    @Parameter( property = "unzip" )
    private List<String> unzip;

    @Component( role = BuildPluginManager.class )
    private BuildPluginManager pluginManager;

    /**
     * Based on dependencies entered, selects what to copy and what to copy and unpack.
     * 
     * @throws MojoExecutionException .
     * @throws MojoFailureException when plugin execution result was other than expected
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        executeMojoWithLogs( "org.apache.maven.plugins", "maven-dependency-plugin", "2.8", "copy-dependencies",
            configuration() );

        if ( dependencyFolder.exists() )
        {
            ZipUnArchiver unpacker = new ZipUnArchiver();
            unpacker.enableLogging( new LoggerImplementation( getLog() ) );

            getLog().info( "Unpacking prz files" );
            String[] flatUnpackExtensions = { "prz", };
            Collection<File> flatUnpackFiles = FileUtils.listFiles( dependencyFolder, flatUnpackExtensions, false );

            unpacker.setDestDirectory( dependencyFolder );
            for ( File flatUnpackFile : flatUnpackFiles )
            {
                getLog().info( String.format( "Unpacking %s", flatUnpackFile.getName() ) );
                unpacker.setSourceFile( flatUnpackFile );
                unpacker.extract();
            }

            if ( !( null == unzip || unzip.isEmpty() ) )
            {
                getLog().info( "Unpack zip-compressed files" );
                Set<Artifact> artifacts = project.getArtifacts();
                Map<String, String> targetFoldersForFiles = new HashMap<String, String>();

                for ( Artifact artifact : artifacts )
                {
                    for ( String unzipSelection : unzip )
                    {

                        if ( artifact.toString().startsWith( unzipSelection ) )
                        {
                            targetFoldersForFiles.put( artifact.getFile().getName(),
                                String.format(
                                    "%s_%s",
                                    artifact.getArtifactId(),
                                    artifact.getType() ) );
                        }
                    }
                }
                if ( !targetFoldersForFiles.isEmpty() )
                {
                    Collection<File> subfolderUnpackFiles = FileUtils.listFiles( dependencyFolder, null, false );
                    for ( File subfolderUnpackFile : subfolderUnpackFiles )
                    {
                        if ( targetFoldersForFiles.containsKey( subfolderUnpackFile.getName() ) )
                        {
                            File destDirectory = new File( dependencyFolder,
                                                           targetFoldersForFiles.get( subfolderUnpackFile.getName() ) );
                            try
                            {
                                getLog().info(
                                    String.format( "Unpacking %s to %s", subfolderUnpackFile.getName(),
                                        destDirectory.getCanonicalPath() ) );
                                if ( !destDirectory.exists() )
                                {
                                    destDirectory.mkdirs();
                                }
                            }
                            catch ( IOException e )
                            {
                                throw new MojoFailureException( "Failed to extract a zip file", e );
                            }
                            unpacker.setSourceFile( subfolderUnpackFile );
                            unpacker.setDestDirectory(
                                    destDirectory );
                            unpacker.extract();
                        }
                    }
                }
            }
            else
            {
                getLog().info( "Nothing to unzip" );
            }
        }
        else
        {
            getLog().info( "No dependencies to extract" );
        }
        try
        {
            if ( staticFilesFolder.exists() )
            {
                getLog().info( "Copy static resources" );
                if ( !staticFilesTargetFolder.exists() )
                {
                    staticFilesTargetFolder.mkdirs();
                }
                org.codehaus.plexus.util.FileUtils.copyDirectoryStructure( staticFilesFolder,
                    staticFilesTargetFolder );
            }
            else
            {
                getLog().info( "No static resources to copy" );
            }
        }
        catch ( IOException e )
        {
            String message =
                String.format( "Failed to copy %s to %s", staticFilesFolder, staticFilesTargetFolder );
            String shortMessage = "Failed to copy resources";
            getLog().debug( message, e );
            throw new MojoFailureException( e, shortMessage, message );
        }
    }

    private void executeMojoWithLogs( String groupId, String artifactId, String version, String goal,
                                      Xpp3Dom configuration )
        throws MojoExecutionException
    {
        getLog().info( String.format( "--- %s:%s:%s (call within prepare-dependencies) @ empty-project ---",
            artifactId, version, goal ) );
        getLog().debug( configuration.toString() );
        executeMojo(
            plugin(
                groupId( groupId ),
                artifactId( artifactId ),
                version( version )
            ),
            goal( goal ),
            configuration,
            executionEnvironment( project, session, pluginManager ) );

        getLog().info( String.format( "--- %s:%s:%s ended ---",
            artifactId, version, goal ) );

    }

}

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
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import pl.net.ptak.helpers.LoggerImplementation;
import pl.net.ptak.helpers.Unzip;

/**
 * Copies all dependencies to dependencyFolder, copies static files to staticFilesTargetFolder, unpacks what needs
 * unpacking
 * 
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
     * The folder in which data are placed for dependency resolution
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
    @Parameter( defaultValue = "src/static", property = "staticFilesFolder",
                    readonly = true, required = true )
    private File staticFilesFolder;

    /**
     * A folder holding static files within the target directory.
     */
    @Parameter( defaultValue = "${project.build.directory}/static", property = "staticFilesTargetFolder",
                    readonly = true, required = true )
    private File staticFilesTargetFolder;

    /**
     * List of artifacts to unzip. <br>
     * Each unzip entry should look like this<br>
     * 
     * <pre>
     *  &lt;unzip>
     *      &lt;what>what_to_unpack&lt;/what>          &lt;-- determines what to unpack
     *      &lt;where>where_to_unpack_it&lt;/where>    &lt;-- (optional) where to unpack it within target/dependency
     *      &lt;files>                              &lt;-- (optional) what to unpack from the source zip
     *          &lt;includes>                              &lt;-- (optional)
     *              &lt;include>some/path/&lowast;&lowast;/&lowast;&lt;/include>
     *          &lt;/includes>
     *          &lt;excludes>                              &lt;-- (optional)
     *              &lt;exclude>some/path/inside/exclude_this_file.txt&lt;/exclude>
     *          &lt;/excludes>
     *          &lt;caseSensitive>true&lt;/caseSensitive> &lt;-- (optional)
     *      &lt;/files>
     *  &lt;/unzip>
     * </pre>
     * 
     * Now a couple explanations:
     * <ul>
     * <li>what_to_unpack - This is selected by simple startsWith comparison on Artifact identifier in form
     * groupId:artifactId:type:version. If you write just a part of that, it will still work. If more than one element
     * is found, all will be unpacked.</li>
     * <li>where_to_unpack_it - optional, by default output folder is artifactId_type, eg. super-artifact_zip</li>
     * <li>Even if you select a subfolder to extract, the whole folder path is preserved in extracted folder</li>
     * </ul>
     * While the form above looks scary, the simplest form is:
     * 
     * <pre>
     *  &lt;unzip>
     *      &lt;what>what_to_unpack&lt;/what>
     *  &lt;/unzip>
     * </pre>
     * 
     * <br>
     * If you enter an empty unzips element, or the list is not provided, nothing will be extracted.
     */
    @Parameter( property = "unzips" )
    private List<Unzip> unzips;

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

        unzipDependenciesIfNeeded();
        copyStaticFiles();
    }

    private void unzipDependenciesIfNeeded()
        throws MojoFailureException
    {
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

            if ( !( null == unzips || unzips.isEmpty() ) )
            {
                getLog().info( "Unpack zip-compressed files" );
                Set<Artifact> artifacts = project.getArtifacts();
                Map<String, Unzip> unzipsForFiles = new HashMap<String, Unzip>();

                for ( Artifact artifact : artifacts )
                {
                    for ( Unzip unzipSelection : unzips )
                    {
                        if ( artifact.toString().startsWith( unzipSelection.getWhat() ) )
                        {
                            if ( unzipSelection.getWhere() == null )
                            {
                                unzipSelection.setWhere(  String.format(
                                    "%s_%s",
                                    artifact.getArtifactId(),
                                    artifact.getType() ) );
                        }
                            unzipsForFiles.put( artifact.getFile().getName(),
                                unzipSelection );
                        }
                    }
                }
                if ( !unzipsForFiles.isEmpty() )
                {
                    Collection<File> subfolderUnpackFiles = FileUtils.listFiles( dependencyFolder, null, false );
                    for ( File subfolderUnpackFile : subfolderUnpackFiles )
                    {
                        if ( unzipsForFiles.containsKey( subfolderUnpackFile.getName() ) )
                        {
                            Unzip unzipSelection = unzipsForFiles.get( subfolderUnpackFile.getName() );

                                            
                            File destDirectory = new File( dependencyFolder,
                                unzipSelection.getWhere() );
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
                            unpacker.setOverwrite( false );
                            if ( unzipSelection.getFiles() != null )
                            {
                                FileSelector[] fileSelectors = { unzipSelection.getFiles(), };
                                unpacker.setFileSelectors( fileSelectors );
                            }
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
    }

    private void copyStaticFiles()
        throws MojoFailureException
    {
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

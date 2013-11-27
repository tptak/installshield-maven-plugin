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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author Tomasz Ptak
 */
@Mojo( name = "prq-prepackage" )
public class PrqPrePackageMojo
    extends AbstractMojo
{

    /**
     * Location of the output of the InstallShield build.
     */
    @Parameter( defaultValue = "${project.build.directory}/output", property = "outputDir", required = true )
    private File installshieldOutputDirectory;

    /**
     * The folder in which data will be placed for packaging
     */
    @Parameter( defaultValue = "${project.build.directory}/${project.artifactId}", property = "prePackageFolder",
                    readonly = true, required = true )
    private File prePackageFolder;

    /**
     * The folder in which data will be placed for packaging
     */
    @Parameter( defaultValue = "${project.build.directory}/${project.artifactId}/${project.artifactId}",
                    property = "prePackageInstallerSubFolder", readonly = true, required = true )
    private File prePackageInstallerSubFolder;

    /**
     * A folder holding static files within the target directory.
     */
    @Parameter( defaultValue = "${project.build.directory}/static", property = "staticFilesTargetFolder",
                    readonly = true, required = true )
    private File staticFilesTargetFolder;

    /**
     * The prerequisite file to be included
     */
    @Parameter( defaultValue = "${project.artifactId}.prq", property = "prqFile", required = true )
    private File prerequisite;

    /**
     * This Mojo gathers all deliverables into one folder for packaging
     * 
     * @throws MojoExecutionException .
     * @throws MojoFailureException when plugin execution result was other than expected
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( String.format( "Beginning preparation of data in folder %s", prePackageFolder.getName() ) );
        if ( !prePackageFolder.exists() )
        {
            prePackageFolder.mkdirs();
            prePackageInstallerSubFolder.mkdirs();
        }
        if ( !installshieldOutputDirectory.exists() )
        {
            getLog().error( String.format( "IS Project Build Output available: %b",
                installshieldOutputDirectory.exists() ) );
            throw new MojoFailureException( "InstallShield project file not found" );
        }

        if ( !prerequisite.exists() )
        {

            getLog().error( String.format( "Prq File available: %b", prerequisite.exists() ) );
            throw new MojoFailureException( "InstallShield prerequisite file not found" );
        }

        File folderToCopy = null;
        boolean folderCopied = false;

        try
        {
            String folderName = "DiskImages";
            Iterator<File> iterator =
                FileUtils.iterateFilesAndDirs( installshieldOutputDirectory,
                    new NotFileFilter( TrueFileFilter.INSTANCE ),
                    DirectoryFileFilter.INSTANCE );

            while ( iterator.hasNext() )
            {
                folderToCopy = iterator.next();
                if ( folderToCopy.getName().equalsIgnoreCase( folderName ) )
                {
                    getLog().info( String.format( "Preparing %s for packaging", folderToCopy.getCanonicalPath() ) );

                    File diskImagesTarget = new File( prePackageInstallerSubFolder, folderName );

                    diskImagesTarget.mkdirs();

                    org.codehaus.plexus.util.FileUtils.copyDirectoryStructure( folderToCopy, diskImagesTarget );
                    folderCopied = true;
                    break;
                }
            }

            if ( !folderCopied )
            {
                String message = String.format( "%s folder not found within the InstallShieldOutput", folderName );
                getLog().error( message );
                throw new MojoFailureException( message );
            }

            while ( iterator.hasNext() )
            {
                folderToCopy = iterator.next();
                if ( folderToCopy.getName().equalsIgnoreCase( folderName ) )
                {
                    String message =
                        String.format( "More than one %s folder found within the InstallShieldOutput, "
                            + "the first one will be used and the rest ignored", folderName );
                    getLog().warn( message );
                    break;
                }
            }
        }
        catch ( IOException e )
        {
            String message = String.format( "Failed to copy %s to %s", folderToCopy, prePackageInstallerSubFolder );
            String shortMessage = "Failed to copy resources";
            getLog().debug( message, e );
            throw new MojoFailureException( e, shortMessage, message );
        }

        try
        {
            if ( staticFilesTargetFolder.exists() )
            {
                File staticFilesCopyDestination = new File( prePackageInstallerSubFolder, "static" );
                if ( !staticFilesCopyDestination.exists() )
                {
                    staticFilesCopyDestination.mkdirs();
                }
                org.codehaus.plexus.util.FileUtils.copyDirectoryStructure( staticFilesTargetFolder,
                    staticFilesCopyDestination );
            }
        }
        catch ( IOException e )
        {
            String message =
                String.format( "Failed to copy %s to %s", staticFilesTargetFolder, prePackageInstallerSubFolder );
            String shortMessage = "Failed to copy resources";
            getLog().debug( message, e );
            throw new MojoFailureException( e, shortMessage, message );
        }

        try
        {
            getLog().info( String.format( "Preparing %s for packaging", prerequisite.getCanonicalPath() ) );
            org.codehaus.plexus.util.FileUtils.copyFileToDirectory( prerequisite, prePackageFolder );
        }
        catch ( IOException e )
        {
            String message = String.format( "Failed to copy %s to %s", prerequisite, prePackageFolder );
            String shortMessage = "Failed to copy resources";
            getLog().debug( message, e );
            throw new MojoFailureException( e, shortMessage, message );
        }
        getLog().info( "Done prepackaging" );
    }

}

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

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.OS;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * This is a Mojo class responsible for building the InstallShield project.
 * 
 * @author Tomasz Ptak
 */
@Mojo( name = "build-is-project" )
public class InstallShieldBuildMojo
    extends AbstractMojo
{

    /**
     * Location of the output of the InstallShield build.
     */
    @Parameter( defaultValue = "${project.build.directory}/output", property = "outputDir", required = true )
    private File installshieldOutputDirectory;

    /**
     * Location of the InstallShield project file.
     */
    @Parameter( property = "installshieldProjectFile", defaultValue = "${project.artifactId}.ism", required = true )
    private File installshieldProjectFile;

    /**
     * Where to look for InstallShield executable. By default it is assumed that ISCmdBld.exe is put on the Path
     * variable
     */
    @Parameter( property = "installshieldExecutable", defaultValue = "ISCmdBld.exe", required = true )
    private String installshieldExecutable;

    /**
     * Verifies that configuration is satisfied to bild the project and builds it.
     * 
     * @throws MojoExecutionException when plugin is misconfigured
     * @throws MojoFailureException when plugin execution result was othere than expected
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        if ( !OS.isFamilyWindows() )
        {
            throw new MojoExecutionException( "This plugin is for Windows systems only" );
        }

        if ( !installshieldOutputDirectory.exists() )
        {
            installshieldOutputDirectory.mkdirs();
        }

        if ( !installshieldProjectFile.exists() )
        {

            getLog().error( String.format( "IS Project File available: %b", installshieldProjectFile.exists() ) );
            throw new MojoFailureException( "InstallShield project file not found" );
        }

        String canonicalProjectFilePath = resolveCanonicalPath( installshieldProjectFile );

        getLog().info( String.format( "About to build file %s", canonicalProjectFilePath ) );

        String canonicalOutputDirectoryPath = resolveCanonicalPath( installshieldOutputDirectory );

        getLog().info( String.format( "Output will be placed in %s", canonicalProjectFilePath ) );

        CommandLine installshieldCommandLine = new CommandLine( installshieldExecutable );

        installshieldCommandLine.addArgument( "-p" );
        installshieldCommandLine.addArgument( canonicalProjectFilePath );
        installshieldCommandLine.addArgument( "-b" );
        installshieldCommandLine.addArgument( canonicalOutputDirectoryPath );

        Executor exec = new DefaultExecutor();

        try
        {
            exec.execute( installshieldCommandLine );
        }
        catch ( IOException e )
        {
            String errorMessage = "Failed to execute InstallShield build";
            getLog().error( errorMessage );
            getLog().debug( "Details to failure: ", e );
            throw new MojoFailureException( errorMessage );
        }
    }

    private String resolveCanonicalPath( File file )
        throws MojoFailureException
    {
        try
        {
            return file.getCanonicalPath();
        }
        catch ( IOException e )
        {
            String errorMessage = "Failed to resolve canonicalPath for the project file";
            getLog().error( errorMessage );
            getLog().debug( "Something is wrong with the project file path", e );
            throw new MojoFailureException( errorMessage );
        }
    }

}

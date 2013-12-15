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
import java.util.Map.Entry;
import java.util.TreeMap;

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
     * Project value setting. If not empty, this is the value to which version parameter is set.<br>
     */
    @Parameter( defaultValue = "${project.version}", property = "version" )
    private String version;

    /**
     * Location of the InstallShield project file. If empty, there will be no InstallShield build, everything else
     * (dependency preparation, prepackaging, packaging) will be done normally.
     */
    @Parameter( property = "installshieldProjectFile", defaultValue = "${project.artifactId}.ism" )
    private File installshieldProjectFile;

    /**
     * Should the build fail if isProjectFile is not found? Handy when you have a prerequisite with binary project
     */
    @Parameter( property = "failWhenNoInstallshieldFile", defaultValue = "true", required = true )
    private boolean failWhenNoInstallshieldFile;

    /**
     * Where to look for InstallShield executable. By default it is assumed that ISCmdBld.exe is put on the Path
     * variable
     */
    @Parameter( property = "installshieldExecutable", defaultValue = "ISCmdBld.exe", required = true )
    private String installshieldExecutable;

    /**
     * Use pom version for the InstallShield project being built. When set to true, version from project pom will be
     * passed to InstallShield through -y parameter.
     */
    @Parameter( property = "usePomVersion", defaultValue = "true", required = false )
    private boolean usePomVersion;

    /** The product configuration. If set, it will be passed to InstallShield through -a parameter */
    @Parameter( property = "productConfiguration" )
    private String productConfiguration;

    /** The product release. If set, it will be passed to InstallShield through -r parameter */
    @Parameter( property = "productRelease" )
    private String productRelease;

    /**
     * The properties. If any values are passed, they will be passed to InstallShield each with -z parameter.<br>
     */
    @Parameter( property = "properties" )
    private TreeMap<String, String> properties;

    /**
     * The path variables. If any values are passed, they will be passed to InstallShield each with -l parameter.<br>
     */
    @Parameter( property = "pathVariables" )
    private TreeMap<String, String> pathVariables;

    /**
     * Verifies that configuration is satisfied to bild the project and builds it.
     * 
     * @throws MojoExecutionException when plugin is misconfigured
     * @throws MojoFailureException when plugin execution result was other than expected
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

        if ( installshieldProjectFile == null || !installshieldProjectFile.exists() )
        {
            if ( failWhenNoInstallshieldFile )
            {
                getLog().error( String.format( "IS Project File available: %b", installshieldProjectFile.exists() ) );
                throw new MojoFailureException( "InstallShield project file not found" );
            }
            else
            {
                getLog().info( "IS Project File not found. IS build skipped" );
            }
        }
        else
        {

            String canonicalProjectFilePath = resolveCanonicalPath( installshieldProjectFile );

            getLog().info( String.format( "About to build file %s", canonicalProjectFilePath ) );

            String canonicalOutputDirectoryPath = resolveCanonicalPath( installshieldOutputDirectory );

            getLog().info( String.format( "Output will be placed in %s", canonicalOutputDirectoryPath ) );

            CommandLine installshieldCommandLine = new CommandLine( installshieldExecutable );

            addCmdLnArguments( installshieldCommandLine, "-p", canonicalProjectFilePath );
            addCmdLnArguments( installshieldCommandLine, "-b", canonicalOutputDirectoryPath );
            if ( usePomVersion && null != version && !version.isEmpty() )
            {
                addCmdLnArguments( installshieldCommandLine, "-y", version );

            }

            if ( null != productConfiguration && !productConfiguration.isEmpty() )
            {
                addCmdLnArguments( installshieldCommandLine, "-a", productConfiguration );
            }

            if ( null != productRelease && !productRelease.isEmpty() )
            {
                addCmdLnArguments( installshieldCommandLine, "-r", productRelease );
            }

            if ( null != properties && !properties.isEmpty() )
            {
                for ( Entry<String, String> entry : properties.entrySet() )
                {
                    addCmdLnArguments( installshieldCommandLine, "-z",
                        String.format( "%s=%s", entry.getKey(), entry.getValue() ) );
                }
            }

            if ( null != pathVariables && !pathVariables.isEmpty() )
            {
                for ( Entry<String, String> entry : pathVariables.entrySet() )
                {
                    addCmdLnArguments( installshieldCommandLine, "-l",
                        String.format( "%s=%s", entry.getKey(), entry.getValue() ) );
                }
            }

            Executor exec = new DefaultExecutor();

            getLog().debug(
                String.format( "IS Build Command to be executed: %s", installshieldCommandLine.toString() ) );

            try
            {
                int exitCode = exec.execute( installshieldCommandLine );
                getLog().debug( String.format( "IS build exit code: %d", exitCode ) );
                if ( exitCode != 0 )
                {
                    throw new MojoFailureException( "Failed to build IS project" );
                }
            }
            catch ( IOException e )
            {
                String errorMessage = "Failed to execute InstallShield build";
                getLog().error( errorMessage );
                getLog().debug( "Details to failure: ", e );
                throw new MojoFailureException( errorMessage );
            }
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

    private void addCmdLnArguments( CommandLine cl, String... params )
    {
        cl.addArguments( params );
    }

}

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

/**
 * @author Tomasz Ptak
 */
@Mojo( name = "prq-package" )
public class PrqPackageMojo
    extends AbstractMojo
{

    /**
     * The Archiver which will be responsible for packaging of the artifact
     */
    @Component( hint = "zip", role = Archiver.class )
    private ZipArchiver zipArchiver;

    /**
     * The name of folder in which data is placed for packaging
     */
    @Parameter( defaultValue = "${project.artifactId}", property = "finalName", readonly = true, required = true )
    private String finalName;

    /**
     * The folder in which data will be placed for packaging
     */
    @Parameter( defaultValue = "${project.build.directory}/${project.artifactId}", property = "finalName",
                    readonly = true, required = true )
    private File prePackageFolder;

    @Parameter( defaultValue = "${project.build.directory}", readonly = true, required = true )
    private String targetFolder;

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    /**
     * This Mojo gathers all deliverables into one archive and selects it as a project artifact
     * 
     * @throws MojoExecutionException .
     * @throws MojoFailureException when plugin execution result was other than expected
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        File output = new File( String.format( "%s/%s.prz", targetFolder, finalName ) );
        getLog().info( String.format( "Packaging data from %s to %s", prePackageFolder, output.getName() ) );
        DefaultFileSet fileSet = new DefaultFileSet();
        fileSet.setDirectory( prePackageFolder );
        zipArchiver.addFileSet( fileSet );
        zipArchiver.setDestFile( output );
        try
        {
            zipArchiver.createArchive();
        }
        catch ( Exception e )
        {
            getLog().debug( "Failed to create archive", e );
            throw new MojoFailureException( e, "Failed to create archive",
                                            "Exception caught during an attempt to create an archive" );
        }

        project.getArtifact().setFile( output );
        getLog().info( "Done packaging" );
    }
}

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
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

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
import org.codehaus.plexus.util.xml.Xpp3Dom;

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
        executeMojoWithLogs( "org.apache.maven.plugins", "maven-dependency-plugin", "2.8", "unpack-dependencies",
                             configuration(
                             element( name( "skip" ), "true" )
                             ) );
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

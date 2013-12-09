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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Copies files for packaging into a folder which will be archived and prepares prerequisite. <br>
 * What will be selected for packaging:
 * <ul>
 * <li>All static files</li>
 * <li>Full content of DiskImages folder (if there are more DiskImages folders, only the first one is included, rest is
 * ignored)</li>
 * <li>Files from dependency folder referenced in prerequisite</li>
 * </ul>
 * What's done to prerequisite:
 * <ul>
 * <li>paths are updated to point to right files</li>
 * <li>checksums are updated</li>
 * <li>sizes are updated</li>
 * </ul>
 * 
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
     * A prefix for setting new dependency relative path in prq
     */
    @Parameter( defaultValue = ".\\${project.artifactId}", readonly = true, required = true )
    private String relativePackageSubFolderPrefix;

    /**
     * A folder holding static files within the target directory.
     */
    @Parameter( defaultValue = "${project.build.directory}/static", property = "staticFilesTargetFolder",
                    readonly = true, required = true )
    private File staticFilesTargetFolder;

    /**
     * Target folder
     */
    @Parameter( defaultValue = "${project.build.directory}", readonly = true, required = true )
    private File targetFolder;

    /**
     * The prerequisite file to be included
     */
    @Parameter( defaultValue = "${project.artifactId}.prq", property = "prqFile", required = true )
    private File prerequisite;

    /**
     * The folder in which data are placed for dependency resolution
     */
    @Parameter( defaultValue = "${project.build.directory}/dependency", property = "dependencyFolder",
                    readonly = true, required = true )
    private File dependencyFolder;


    /**
     * Should the build fail if isProjectFile is not found? Handy when you have a prerequisite with binary project
     */
    @Parameter( property = "failWhenNoInstallshieldFile", defaultValue = "true", required = true )
    private boolean failWhenNoInstallshieldFile;

    /**
     * A base directory for given project
     */
    @Parameter( defaultValue = "${basedir}", readonly = true, required = true )
    private File basedir;

    /**
     * This is a reference to a packaged DiskImages Folder. It is one of three possible sources of files referenced in a
     * prerequisite.
     */
    private File packagedDiskImagesFolder = null;

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
        if ( !installshieldOutputDirectory.exists() && failWhenNoInstallshieldFile )
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

        prepareBuildOutputForPackaging();

        prepareStaticFilesForPackaging();

        preparePrerequisiteForPackaging();

        getLog().info( "Done prepackaging" );
    }

    private void preparePrerequisiteForPackaging()
        throws MojoFailureException
    {
        try
        {
            getLog().info( String.format( "Preparing %s for packaging", prerequisite.getCanonicalPath() ) );
            org.codehaus.plexus.util.FileUtils.copyFileToDirectory( prerequisite, prePackageFolder );
            File targetPrqFile = new File( prePackageFolder, prerequisite.getName() );

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse( targetPrqFile );

            Element root = (Element) doc.getFirstChild();
            NodeList filesElements = root.getElementsByTagName( "files" );

            if ( filesElements.getLength() > 1 )
            {
                throw new MojoFailureException( "There should be at most one files element" );
            }

            if ( filesElements.getLength() == 1 )
            {
                Node filesList = filesElements.item( 0 );

                NodeList files = filesList.getChildNodes();

                for ( int i = 0; i < files.getLength(); i++ )
                {
                    Node fileNode = files.item( i );

                    if ( !( fileNode instanceof Element ) )
                    {
                        continue;
                    }

                    NamedNodeMap fileAttributes = fileNode.getAttributes();

                    Attr dependencyFileAttr = (Attr) fileAttributes.getNamedItem( "LocalFile" );

                    File dependencyFile = getFileFromPrq( dependencyFileAttr, i );

                    if ( !dependencyFile.exists() )
                    {
                        String message =
                            String.format( "%s is referenced in prq file, but it does not exist",
                                dependencyFile.getAbsolutePath() );
                        throw new MojoFailureException( message );
                    }

                    // set new relative path to dependency
                    setNewRelativePathForFile( dependencyFileAttr, dependencyFile );
                    // calculate new md5 checksum
                    Attr checkSumAttr = (Attr) fileAttributes.getNamedItem( "CheckSum" );
                    setNewMd5ChecksumForFile( checkSumAttr, dependencyFile );
                    // calculate new size
                    Attr fileSizeAttr = (Attr) fileAttributes.getNamedItem( "FileSize" );
                    setNewSizeForFile( fileSizeAttr, dependencyFile );
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource( doc );
            StreamResult result = new StreamResult( targetPrqFile );
            transformer.transform( source, result );

        }
        catch ( IOException e )
        {
            String message = String.format( "Failed to copy %s to %s", prerequisite, prePackageFolder );
            String shortMessage = "Failed to copy resources";
            getLog().debug( message, e );
            throw new MojoFailureException( e, shortMessage, message );
        }
        catch ( ParserConfigurationException e )
        {
            String message = "Failed to create prq file parser";
            String shortMessage = "Failed to prepare prerequisite for packaging";
            getLog().debug( message, e );
            throw new MojoFailureException( e, shortMessage, message );
        }
        catch ( SAXException e )
        {
            String message = "Failed to modify prq file";
            String shortMessage = "Failed to prepare prerequisite for packaging";
            getLog().debug( message, e );
            throw new MojoFailureException( e, shortMessage, message );
        }
        catch ( TransformerConfigurationException e )
        {
            String message = "Failed to prepare transformer for storing prq file";
            String shortMessage = "Failed to prepare prerequisite for packaging";
            getLog().debug( message, e );
            throw new MojoFailureException( e, shortMessage, message );
        }
        catch ( TransformerException e )
        {
            String message = "Failed to run transformer for storing prq file";
            String shortMessage = "Failed to prepare prerequisite for packaging";
            getLog().debug( message, e );
            throw new MojoFailureException( e, shortMessage, message );
        }
    }

    /**
     * @param fileSizeAttr
     * @param dependencyFile
     */
    private void setNewSizeForFile( Attr fileSizeAttr, File dependencyFile )
    {
        fileSizeAttr.setValue( String.format( "0,%d", dependencyFile.length() ) );
    }

    /**
     * @param dependencyFile
     * @param dependencyFileAttr
     */
    private void setNewMd5ChecksumForFile( Attr dependencyFileAttr, File dependencyFile )
        throws MojoFailureException
    {
        FileInputStream fileInputStream = null;
        try
        {
            fileInputStream = new FileInputStream( dependencyFile );
            String md5 = DigestUtils.md5Hex( fileInputStream );
            dependencyFileAttr.setValue( md5.toUpperCase() );
        }
        catch ( FileNotFoundException e )
        {
            String message = String.format( "File %s not found", dependencyFile );
            String shortMessage = "File not found";
            getLog().debug( message, e );
            throw new MojoFailureException( e, shortMessage, message );
        }
        catch ( IOException e )
        {
            String message = String.format( "Failed to calculate checksum for file %s", dependencyFile );
            String shortMessage = "Failed to calculate checksum";
            getLog().debug( message, e );
            throw new MojoFailureException( e, shortMessage, message );
        }
        finally
        {
            try
            {
                fileInputStream.close();
            }
            catch ( IOException e )
            {
                String message = String.format( "Failed to close filestream for file %s", dependencyFile );
                String shortMessage = "Failed to close filestream";
                getLog().debug( message, e );
                throw new MojoFailureException( e, shortMessage, message );
            }
        }
    }

    private String setNewRelativePathForFile( Attr dependencyFileAttr, File dependencyFile )
        throws MojoFailureException
    {
        try
        {
            String canonicalPath = dependencyFile.getCanonicalPath();

            // check if the file is within the project structure (do not allow external files)
            if ( !canonicalPath.startsWith( targetFolder.getCanonicalPath() ) )
            {
                String message =
                    String.format(
                        "%s is not within the %s folder. You do NOT want to use files from outside this folder",
                        dependencyFile, targetFolder );
                throw new MojoFailureException( message );
            }

            // prepare new relative path and update it
            if ( canonicalPath.startsWith( dependencyFolder.getCanonicalPath() ) )
            {
                // IS: ./target/output/dependency/....
                // SHOUD BE: ./target/${project.artifactId}/dependency/....
                String relativeOutputPath =
                    calculateAndSetNewRelativePath( dependencyFile, targetFolder, dependencyFileAttr );

                File copyTarget = new File( prePackageInstallerSubFolder, relativeOutputPath );

                getLog().info(
                    String.format( "Copying dependency file %s to %s", dependencyFile.getCanonicalPath(),
                        copyTarget.getParentFile().getCanonicalPath() ) );

                FileUtils.copyFile( dependencyFile, copyTarget );
            }
            else if ( null != packagedDiskImagesFolder
                && canonicalPath.startsWith( packagedDiskImagesFolder.getCanonicalPath() ) )
            {
                // IS: ./target/output/something/something/DiskImages/....
                // SHOUD BE: ./target/${project.artifactId}/DiskImages/....

                calculateAndSetNewRelativePath( dependencyFile, packagedDiskImagesFolder.getParentFile(),
                    dependencyFileAttr );
            }
            else if ( canonicalPath.startsWith( staticFilesTargetFolder.getCanonicalPath() ) )
            {
                // IS: ./target/static/....
                // SHOUD BE: ./target/${project.artifactId}/static/....
                calculateAndSetNewRelativePath( dependencyFile, targetFolder, dependencyFileAttr );
            }
            else
            {
                throw new MojoFailureException( String.format(
                    "It is expected that referenced files come from either of these locations: %s, %s, %s",
                    dependencyFolder, packagedDiskImagesFolder, staticFilesTargetFolder ) );
            }
            return canonicalPath;
        }
        catch ( IOException e )
        {

            String message = String.format( "Failed to prepare relative path for file %s", dependencyFile );
            String shortMessage = "Failed to prepare relative path";
            getLog().debug( message, e );
            throw new MojoFailureException( e, shortMessage, message );
        }
    }

    private String calculateAndSetNewRelativePath( File dependencyFile, File relativePathRoot, Attr dependencyFileAttr )
        throws IOException
    {

        String relativeOutputPath =
            dependencyFile.getCanonicalPath()
                          .substring( relativePathRoot.getCanonicalPath().length() );
        String newRelativePath = relativePackageSubFolderPrefix + relativeOutputPath;

        dependencyFileAttr.setValue( newRelativePath );

        getLog().debug(
            String.format( "New relative path for %s was set to %s",
                dependencyFile.getCanonicalPath(), newRelativePath ) );

        return relativeOutputPath;
    }

    /**
     * @param fileAttributes
     * @return
     * @throws MojoFailureException
     */
    private File getFileFromPrq( Attr filePath, int index )
        throws MojoFailureException
    {
        if ( filePath == null )
        {
            throw new MojoFailureException( String.format( "No file location in prerequisite file %d", index ) );
        }
        return new File( basedir, filePath.getValue() );
    }

    private void prepareStaticFilesForPackaging()
        throws MojoFailureException
    {
        try
        {
            if ( staticFilesTargetFolder.exists() )
            {
                File staticFilesCopyDestination = new File( prePackageInstallerSubFolder, "static" );
                if ( !staticFilesCopyDestination.exists() )
                {
                    staticFilesCopyDestination.mkdirs();
                }
                getLog().info(
                    String.format( "Copying directory structure from %s to %s",
                        staticFilesTargetFolder.getCanonicalPath(), staticFilesCopyDestination.getCanonicalPath() ) );
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
    }

    private void prepareBuildOutputForPackaging()
        throws MojoFailureException
    {
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
                    packagedDiskImagesFolder = folderToCopy;
                    break;
                }
            }

            if ( !folderCopied && failWhenNoInstallshieldFile )
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
    }

}

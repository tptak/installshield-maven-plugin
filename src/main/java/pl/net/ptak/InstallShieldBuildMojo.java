package pl.net.ptak;

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

@Mojo(name = "build-is-project")
public class InstallShieldBuildMojo extends AbstractMojo {

	/**
	 * Location of the file.
	 */
	@Parameter(defaultValue = "${project.build.directory}/output", property = "outputDir", required = true)
	private File installshieldOutputDirectory;

	@Parameter(property = "installshieldProjectFile", defaultValue = "${project.artifactId}.ism", required = true)
	private File installshieldProjectFile;

	@Parameter(property = "installshieldExecutable", defaultValue = "ISCmdBld.exe", required = true)
	private String installshieldExecutable;

	public void execute() throws MojoExecutionException, MojoFailureException {

		if (!OS.isFamilyWindows()) {
			throw new MojoExecutionException(
					"This plugin is for Windows systems only");
		}

		if (!installshieldOutputDirectory.exists()) {
			installshieldOutputDirectory.mkdirs();
		}

		if (!installshieldProjectFile.exists()) {

			getLog().error(
					String.format("IS Project File available: %b",
							installshieldProjectFile.exists()));
			throw new MojoFailureException(
					"InstallShield project file not found");
		}

		String canonicalProjectFilePath = resolveCanonicalPath(installshieldProjectFile);
		
		getLog().info(String.format("About to build file %s", canonicalProjectFilePath));
		
		String canonicalOutputDirectoryPath = resolveCanonicalPath(installshieldOutputDirectory);
		
		getLog().info(String.format("Output will be placed in %s", canonicalProjectFilePath));
		
		CommandLine installshieldCommandLine = new CommandLine(
				installshieldExecutable);

		installshieldCommandLine.addArgument("-p");
		installshieldCommandLine.addArgument(canonicalProjectFilePath);
		installshieldCommandLine.addArgument("-b");
		installshieldCommandLine.addArgument(canonicalOutputDirectoryPath);

		Executor exec = new DefaultExecutor();

		try {
			exec.execute(installshieldCommandLine);
		} catch (IOException e) {
			String errorMessage = "Failed to execute InstallShield build";
			getLog().error(errorMessage);
			getLog().debug("Details to failure: ", e);
			throw new MojoFailureException(errorMessage);
		}
	}

	private String resolveCanonicalPath(File file) throws MojoFailureException {
		try {
			return file.getCanonicalPath();
		} catch (IOException e) {
			String errorMessage = "Failed to resolve canonicalPath for the project file";
			getLog().error(errorMessage);
			getLog().debug("Something is wrong with the project file path", e);
			throw new MojoFailureException(errorMessage);
		}
	}

}

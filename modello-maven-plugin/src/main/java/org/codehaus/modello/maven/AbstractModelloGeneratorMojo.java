package org.codehaus.modello.maven;

/*
 * Copyright (c) 2004, Codehaus.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.modello.ModelloException;
import org.codehaus.modello.ModelloParameterConstants;
import org.codehaus.modello.core.ModelloCore;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.ModelValidationException;
import org.codehaus.plexus.build.BuildContext;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 */
public abstract class AbstractModelloGeneratorMojo extends AbstractMojo {
    // ----------------------------------------------------------------------
    // Parameters
    // ----------------------------------------------------------------------

    /**
     * Base directory of the project, from where the Modello models are loaded.
     */
    @Parameter(defaultValue = "${basedir}", required = true)
    private String basedir;

    /**
     * List of relative paths to mdo files containing the models.
     */
    @Parameter(required = true)
    private String[] models;

    /**
     * The version of the model we will be working on.
     */
    @Parameter(property = "version", required = true)
    private String version;

    /**
     * True if the generated package names should include the version.
     */
    @Parameter(property = "packageWithVersion", defaultValue = "false", required = true)
    private boolean packageWithVersion;

    /**
     * <p>Note: This is passed by Maven and must not be configured by the user.</p>
     */
    @Component
    private ModelloCore modelloCore;

    /**
     * The Maven project instance for the executing project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Additional historical versions to generate, each being packaged with the version regardless of the
     * <code>packageWithVersion</code> setting.
     */
    @Parameter
    private List<String> packagedVersions = new ArrayList<String>();

    /**
     * The contents of license header text, verbatim.
     *
     * @since 2.3.1
     */
    @Parameter
    private String licenseText;

    /**
     * The file that contains license header text. If both configured, the {@link #licenseText} prevails.
     *
     * @since 2.3.1
     */
    @Parameter
    private File licenseFile;

    /**
     * Additional exceptions to the singularization rules, changing plural noun to singular.
     * <p>
     * As a key we provide plural noun and as value we provide singular noun, example:
     * <pre>
     *     &lt;kisses&gt;kiss&lt;/kisses&gt;
     * </pre>
     *
     * @since 2.5.0
     */
    @Parameter
    private Map<String, String> pluralExceptions;

    /**
     * @since 1.0.1
     */
    @Component
    private BuildContext buildContext;

    // ----------------------------------------------------------------------
    // Overridables
    // ----------------------------------------------------------------------

    protected abstract String getGeneratorType();

    public abstract File getOutputDirectory();

    protected boolean producesCompilableResult() {
        return false;
    }

    protected boolean producesResources() {
        return false;
    }

    /**
     * Creates a Properties objects.
     * <br>
     * The abstract mojo will override the output directory, the version and the
     * package with version flag.
     *
     * @return the parameters
     */
    protected Map<String, Object> createParameters() {
        return new HashMap<>();
    }

    /**
     * Override this method to customize the values in the properties set.
     * <p>
     * This method will be called after the parameters have been populated with the
     * parameters in the abstract mojo.
     *
     * @param parameters the parameters to customize
     */
    protected void customizeParameters(Map<String, Object> parameters) {}

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void execute() throws MojoExecutionException {
        String outputDirectory = getOutputDirectory().getAbsolutePath();

        getLog().info("outputDirectory: " + outputDirectory);

        // ----------------------------------------------------------------------
        // Initialize the parameters
        // ----------------------------------------------------------------------

        Map<String, Object> parameters = createParameters();

        parameters.put(ModelloParameterConstants.OUTPUT_DIRECTORY, outputDirectory);

        parameters.put(ModelloParameterConstants.VERSION, version);

        parameters.put(ModelloParameterConstants.PACKAGE_WITH_VERSION, Boolean.toString(packageWithVersion));

        parameters.put(ModelloParameterConstants.PLURAL_EXCEPTIONS, pluralExceptions);

        if (!packagedVersions.isEmpty()) {
            parameters.put(ModelloParameterConstants.ALL_VERSIONS, StringUtils.join(packagedVersions.iterator(), ","));
        }

        if (licenseText != null || licenseFile != null) {
            List<String> license;
            if (licenseText != null) {
                // licenseText prevails
                license = Arrays.asList(licenseText.split(System.lineSeparator()));
            } else {
                try {
                    // load it up and hard fail if cannot, as it is user misconfiguration
                    license = Files.readAllLines(licenseFile.toPath()).stream()
                            .map(l -> StringUtils.stripEnd(l, null))
                            .collect(Collectors.toList());
                } catch (IOException e) {
                    throw new MojoExecutionException("Could not load up license text from " + licenseFile, e);
                }
            }
            parameters.put(ModelloParameterConstants.LICENSE_TEXT, license);
        }

        customizeParameters(parameters);

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        MojoExecutionException firstError = null;
        for (String modelStr : models) {
            try {
                doExecute(modelStr, outputDirectory, parameters);
            } catch (MojoExecutionException e) {
                if (firstError == null) {
                    firstError = e;
                }
                getLog().error(e);
            }
        }
        if (firstError != null) {
            throw firstError;
        }
    }

    /**
     * Performs execute on a single specified model.
     */
    private void doExecute(String modelStr, String outputDirectory, Map<String, Object> parameters)
            throws MojoExecutionException {
        if (!buildContext.hasDelta(modelStr)) {
            getLog().debug("Skipping unchanged model: " + modelStr);
            return;
        }

        getLog().info("Working on model: " + modelStr);

        File modelFile = new File(basedir, modelStr);
        buildContext.removeMessages(modelFile);

        try {
            Model model = modelloCore.loadModel(modelFile);

            // TODO: dynamically resolve/load the generator type
            getLog().info("Generating current version: " + version);
            modelloCore.generate(model, getGeneratorType(), parameters);

            for (String version : packagedVersions) {
                parameters.put(ModelloParameterConstants.VERSION, version);

                parameters.put(ModelloParameterConstants.PACKAGE_WITH_VERSION, Boolean.toString(true));

                getLog().info("Generating packaged version: " + version);
                modelloCore.generate(model, getGeneratorType(), parameters);
            }

            if (producesCompilableResult() && project != null) {
                project.addCompileSourceRoot(outputDirectory);
            }

            if (producesResources() && project != null) {
                Resource resource = new Resource();
                resource.setDirectory(outputDirectory);
                project.addResource(resource);
            }
        } catch (FileNotFoundException e) {
            MojoExecutionException mojoExecutionException = new MojoExecutionException(e.getMessage(), e);
            buildContext.addMessage(
                    modelFile,
                    1 /* line */,
                    1 /* column */,
                    mojoExecutionException.getMessage(),
                    BuildContext.SEVERITY_ERROR,
                    mojoExecutionException);
            throw mojoExecutionException;
        } catch (ModelloException e) {
            MojoExecutionException mojoExecutionException =
                    new MojoExecutionException("Error generating: " + e.getMessage(), e);
            // TODO: Provide actual line/column numbers
            buildContext.addMessage(
                    modelFile,
                    1 /* line */,
                    1 /* column */,
                    mojoExecutionException.getMessage(),
                    BuildContext.SEVERITY_ERROR,
                    mojoExecutionException);
            throw mojoExecutionException;
        } catch (ModelValidationException e) {
            MojoExecutionException mojoExecutionException =
                    new MojoExecutionException("Error generating: " + e.getMessage(), e);
            // TODO: Provide actual line/column numbers
            buildContext.addMessage(
                    modelFile,
                    1 /* line */,
                    1 /* column */,
                    mojoExecutionException.getMessage(),
                    BuildContext.SEVERITY_ERROR,
                    mojoExecutionException);
            throw mojoExecutionException;
        } catch (IOException e) {
            MojoExecutionException mojoExecutionException =
                    new MojoExecutionException("Couldn't read file: " + e.getMessage(), e);
            buildContext.addMessage(
                    modelFile,
                    1 /* line */,
                    1 /* column */,
                    mojoExecutionException.getMessage(),
                    BuildContext.SEVERITY_ERROR,
                    mojoExecutionException);
            throw mojoExecutionException;
        } catch (RuntimeException e) {
            MojoExecutionException mojoExecutionException =
                    new MojoExecutionException("Error generating: " + e.getMessage(), e);
            buildContext.addMessage(
                    modelFile,
                    1 /* line */,
                    1 /* column */,
                    mojoExecutionException.getMessage(),
                    BuildContext.SEVERITY_ERROR,
                    mojoExecutionException);
            throw mojoExecutionException;
        }
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public String getBasedir() {
        return basedir;
    }

    public void setBasedir(String basedir) {
        this.basedir = basedir;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean getPackageWithVersion() {
        return packageWithVersion;
    }

    public void setPackageWithVersion(boolean packageWithVersion) {
        this.packageWithVersion = packageWithVersion;
    }

    public ModelloCore getModelloCore() {
        return modelloCore;
    }

    public void setModelloCore(ModelloCore modelloCore) {
        this.modelloCore = modelloCore;
    }

    public void setBuildContext(BuildContext context) {
        this.buildContext = context;
    }

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public void setPackagedVersions(List<String> packagedVersions) {
        this.packagedVersions = Collections.unmodifiableList(packagedVersions);
    }

    /**
     * @return Returns the paths to the models.
     */
    public String[] getModels() {
        return models;
    }

    /**
     * @param models Sets the paths to the models.
     */
    public void setModels(String[] models) {
        this.models = models;
    }
}

package org.codehaus.modello.maven;

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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.modello.plugin.velocity.VelocityGenerator;

/**
 * Creates files from the model using Velocity templates.
 * <p>
 * This mojo can be given a list of templates and a list of parameters.
 * Each template from the {@link #templates} property will be run with the following context:
 * <ul>
 *    <li>{@code version}: the version of the model to generate</li>
 *    <li>{@code model}: the modello model</li>
 *    <li>{@code Helper}: a {@link org.codehaus.modello.plugin.velocity.Helper} object instance</li>
 *    <li>any additional parameters specified using the {@link #params} property</li>
 * </ul>
 * The output file is controlled from within the template using the {@code #MODELLO-VELOCITY#SAVE-OUTPUT-TO}
 * <a href="https://velocity.apache.org/engine/2.3/vtl-reference.html#directives">VTL directive</a>.
 * This allows a single template to generate multiple files. For example, the following
 * directive will redirect further output from the template to a file named
 * {@code org/apache/maven/api/model/Plugin.java} if the variable {@code package} is set to
 * {@code org.apache.maven.api.model} and the variable {@code className} is set to {@code Plugin}.
 * <p>
 *     {@code #MODELLO-VELOCITY#SAVE-OUTPUT-TO ${package.replace('.','/')}/${className}.java}
 */
@Mojo(name = "velocity", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class ModelloVelocityMojo extends AbstractModelloGeneratorMojo {
    /**
     * The output directory of the generated files.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/modello")
    private File outputDirectory;

    /**
     * The directory where Velocity templates are looked for.
     */
    @Parameter(defaultValue = "${project.basedir}")
    private File velocityBasedir;

    /**
     * A list of template paths to be run against the loaded Modello model.
     * Those are {@code .vm} files as described in the
     * <a href="https://velocity.apache.org/engine/devel/user-guide.html">Velocity Users Guide</a>
     * relative to {@code velocityBasedir}.
     */
    @Parameter
    private List<String> templates;

    /**
     * A list of parameters, using the syntax {@code key=value}.
     * Those parameters will be made accessible to the templates.
     */
    @Parameter
    private List<String> params;

    protected String getGeneratorType() {
        return "velocity";
    }

    @Override
    protected void customizeParameters(Map<String, Object> parameters) {
        super.customizeParameters(parameters);

        Map<String, String> params = this.params == null
                ? Collections.emptyMap()
                : this.params.stream()
                        .collect(Collectors.toMap(
                                s -> s.substring(0, s.indexOf('=')), s -> s.substring(s.indexOf('=') + 1)));

        parameters.put(VelocityGenerator.VELOCITY_BASEDIR, velocityBasedir.getAbsolutePath());

        parameters.put(VelocityGenerator.VELOCITY_TEMPLATES, templates.stream().collect(Collectors.joining(",")));
        parameters.put(VelocityGenerator.VELOCITY_PARAMETERS, params);
    }

    protected boolean producesCompilableResult() {
        return true;
    }

    @Override
    public File getOutputDirectory() {
        return outputDirectory;
    }
}

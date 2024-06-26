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

import java.util.Map;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.modello.ModelloParameterConstants;

/**
 * Creates an Jackson writer from the model.
 *
 * @since 1.8
 * @author <a href="mailto:simonetripodi@apache.org">Simone Tripodi</a>
 */
@Mojo(name = "jackson-writer", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class ModelloJacksonWriterMojo extends AbstractModelloSourceGeneratorMojo {
    protected String getGeneratorType() {
        return "jackson-writer";
    }

    @Override
    protected void customizeParameters(Map<String, Object> parameters) {
        super.customizeParameters(parameters);

        if ("true".equals(parameters.get(ModelloParameterConstants.DOM_AS_XPP3))
                && !getProject().getArtifactMap().containsKey("com.fasterxml.jackson.core:jackson-databind")) {
            getLog().warn("Jackson DOM support requires auxiliary com.fasterxml.jackson.core:jackson-databind module!");
        }
    }
}

/**
 * Mule AppKit
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.maven.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.mule.tools.maven.plugin.cloudhub.CloudHubAdapter;

import java.io.File;
import java.util.Map;

/**
 * @phase deploy
 * @goal cloudhub-deploy
 */
public class CloudHubDeployMojo extends AbstractCloudHubMojo {

    private static final String LATEST_MULE_ESB_VERSION = "3.3.1";

    static final String MULE_TYPE = "mule";

    /**
     * @parameter expression="${cloudhub.workers}" default-value="1"
     */
    protected int workers;

    /**
     * @parameter expression="${cloudhub.muleVersion}"
     */
    protected String muleVersion;

    /**
     * @parameter expression="${cloudhub.maxWaitTime}" default-value="120000"
     */
    protected long maxWaitTime;

    /**
     * @parameter
     */
    protected Map<String, String> properties;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        final String type = this.project.getArtifact().getType();
        if (!MULE_TYPE.equals(type)) {
            throw new IllegalArgumentException("Only supports mule packaging type, not <"+type+">.");
        }

        if (this.project.getAttachedArtifacts().isEmpty()) {
            throw new IllegalArgumentException("No Mule application attached. This probably means `package` phase has not been executed.");
        }

        final File file = ((Artifact) this.project.getAttachedArtifacts().get(0)).getFile();
        getLog().info("Deploying <"+file+">");

        /* Ok, no muleVersion set. Let's search for the mule-core dependency. */
        if( muleVersion == null ) {
            for (Object o : project.getDependencies()) {
                Dependency dependency = (Dependency) o;
                if ( "org.mule".equals(dependency.getGroupId()) && "mule-core".equals(dependency.getArtifactId()) ) {
                    muleVersion = dependency.getVersion();
                    break;
                }
            }
        }

        /* Ok, no mule-core dependency found. What about the 'mule.version' property? */
        if (muleVersion == null) {
            String muleVersionProperty = project.getProperties().getProperty("mule.version");
            if ( muleVersionProperty != null && "".equals(muleVersionProperty) ) {
                muleVersion = muleVersionProperty;
            }

        }

        /* Buh, no property found. Let's use this hard-code latest MULE_ESB_VERSION */
        if (muleVersion == null ) {
            muleVersion = LATEST_MULE_ESB_VERSION;
        }

        final CloudHubAdapter adapter = createDomainConnection();
        adapter.deploy(file, this.muleVersion, this.workers, this.maxWaitTime, this.properties);

    }
}
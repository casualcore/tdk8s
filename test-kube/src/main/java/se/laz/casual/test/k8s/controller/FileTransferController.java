/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.k8s.controller;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.PodResource;
import se.laz.casual.test.k8s.TestKube;
import se.laz.casual.test.k8s.store.ResourceNotFoundException;

import java.nio.file.Path;

/**
 * Controller responsible for handling file transfers.
 */
public class FileTransferController
{
    private final TestKube testKube;

    public FileTransferController( TestKube testKube )
    {
        this.testKube = testKube;
    }

    public boolean download( String pod, String source, Path destination )
    {
        PodResource resource = getPodResource( pod );

        return resource.file( source ).copy( destination );
    }

    public boolean upload( String pod, String source, Path destination )
    {
        PodResource resource = getPodResource( pod );

        return resource.file( source ).upload( destination );
    }

    private PodResource getPodResource( String pod )
    {
        Pod p = null;

        if( this.testKube.getResourcesStore().getPods().containsKey( pod ) )
        {
            p = this.testKube.getResourcesStore().getPod( pod );
        }

        if( p == null )
        {
            p = this.testKube.getClient().pods().withName( pod ).get();
        }

        if( p == null )
        {
            throw new ResourceNotFoundException( "Unable to find pod " + pod );
        }

        return this.testKube.getClient().pods().resource( p );
    }
}

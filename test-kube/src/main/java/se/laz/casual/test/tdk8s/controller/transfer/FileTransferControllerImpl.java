/*
 * Copyright (c) 2025 - 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.transfer;

import io.fabric8.kubernetes.client.dsl.PodResource;
import se.laz.casual.test.tdk8s.controller.provisioning.ResourceLookupController;
import se.laz.casual.test.tdk8s.store.ResourceNotFoundException;

import java.nio.file.Path;

/**
 * Controls file transfers to and from resources.
 */
public class FileTransferControllerImpl implements FileTransferController
{
    private final ResourceLookupController lookupController;

    public FileTransferControllerImpl( ResourceLookupController lookupController )
    {
        this.lookupController = lookupController;
    }

    @Override
    public boolean download( String resource, String source, Path destination )
    {
        PodResource pod = getPodResource( resource );

        return pod.file( source ).copy( destination );
    }

    @Override
    public boolean upload( String resource, Path source, String destination )
    {
        PodResource pod = getPodResource( resource );

        return pod.file( destination ).upload( source );
    }

    private PodResource getPodResource( String resource )
    {
        return lookupController.findFirstPodForResource( resource )
                .orElseThrow( ()-> new ResourceNotFoundException( "Unable to find Pod for resource: " + resource ) );
    }
}

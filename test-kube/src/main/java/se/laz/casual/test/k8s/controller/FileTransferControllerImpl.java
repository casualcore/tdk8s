/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.k8s.controller;

import io.fabric8.kubernetes.client.dsl.PodResource;
import se.laz.casual.test.k8s.store.ResourceNotFoundException;

import java.nio.file.Path;

/**
 * Controller responsible for handling file transfers.
 */
public class FileTransferControllerImpl implements FileTransferController
{
    private final ResourceLookupController lookupController;

    public FileTransferControllerImpl( ResourceLookupController lookupController )
    {
        this.lookupController = lookupController;
    }

    @Override
    public boolean download( String pod, String source, Path destination )
    {
        PodResource resource = getPodResource( pod );

        return resource.file( source ).copy( destination );
    }

    @Override
    public boolean upload( String pod, String source, Path destination )
    {
        PodResource resource = getPodResource( pod );

        return resource.file( source ).upload( destination );
    }

    private PodResource getPodResource( String pod )
    {
        return lookupController.getPodResource( pod )
                .orElseThrow( ()-> new ResourceNotFoundException( "Unable to find pod " + pod ) );
    }
}

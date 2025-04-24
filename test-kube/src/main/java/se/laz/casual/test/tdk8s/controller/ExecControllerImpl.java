/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.PodResource;
import se.laz.casual.test.tdk8s.exec.ExecResult;
import se.laz.casual.test.tdk8s.store.ResourceNotFoundException;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;

/**
 * Execute commands against a running pod.
 */
public class ExecControllerImpl implements ExecController
{
    private final ResourceLookupController lookupController;

    public ExecControllerImpl( ResourceLookupController lookupController )
    {
        this.lookupController = lookupController;
    }

    @Override
    public ExecResult executeCommand( String pod, String... command )
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PodResource podResource = lookupController.getPodResource( pod )
                .orElseThrow( () -> new ResourceNotFoundException( "Resource not found: " + pod ) );

        try( ExecWatch watch = podResource.writingOutput( out ).writingError( out )
                .exec( command ) )
        {
            Integer exitCode = watch.exitCode().join();
            return ExecResult.newBuilder()
                    .exitCode( exitCode )
                    .output( out.toString() )
                    .build();
        }
    }

    @Override
    public CompletableFuture<ExecResult> executeCommandAsync( String pod, String... command )
    {
        return CompletableFuture.supplyAsync( () -> executeCommand( pod, command ) );
    }
}

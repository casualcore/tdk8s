/*
 * Copyright (c) 2025 - 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.exec;

import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.PodResource;
import se.laz.casual.test.tdk8s.controller.provisioning.ResourceLookupController;
import se.laz.casual.test.tdk8s.exec.ExecResult;
import se.laz.casual.test.tdk8s.store.ResourceNotFoundException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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
    public ExecResult executeCommand( String resource, String... command )
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PodResource podResource = lookupController.findFirstPodForResource( resource )
                .orElseThrow( () -> new ResourceNotFoundException( "Unable to find Pod for resource: " + resource ) );

        try( ExecWatch watch = podResource.writingOutput( out ).writingError( out )
                .exec( command ) )
        {
            Integer exitCode = watch.exitCode().join();
            return ExecResult.newBuilder()
                    .exitCode( exitCode )
                    .output( out.toString( StandardCharsets.UTF_8 ) )
                    .build();
        }
    }

    @Override
    public CompletableFuture<ExecResult> executeCommandAsync( String resource, String... command )
    {
        return CompletableFuture.supplyAsync( () -> executeCommand( resource, command ) );
    }
}

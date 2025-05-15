/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeExecutor;
import dev.failsafe.RetryPolicy;
import dev.failsafe.Timeout;
import se.laz.casual.test.tdk8s.TestKube;
import se.laz.casual.test.tdk8s.TestKubeException;
import se.laz.casual.test.tdk8s.probe.ProvisioningProbe;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controls the execution of ProvisioningProbes, running them in parallel with
 * appropriate retry, backoff and timeout settings.
 */
public class ProvisioningProbeControllerImpl implements ProvisioningProbeController
{
    static Logger log = Logger.getLogger( ProvisioningProbeControllerImpl.class.getName() );
    private final FailsafeExecutor<Object> executor;

    private final TestKube testKube;

    public ProvisioningProbeControllerImpl( TestKube testKube )
    {
        this( testKube, getDefaultFailsafe() );
    }

    ProvisioningProbeControllerImpl( TestKube testKube, FailsafeExecutor<Object> executor )
    {
        this.testKube = testKube;
        this.executor = executor;
    }

    static FailsafeExecutor<Object> getDefaultFailsafe()
    {
        return Failsafe.with( getDefaultTimeoutPolicy() ).compose( getDefaultRetryPolicy() );
    }

    static RetryPolicy<Object> getDefaultRetryPolicy()
    {
        return RetryPolicy.builder()
                .handleResult( false ) //retry when result of execution is value `false`
                .withBackoff( 500L, 10000L, ChronoUnit.MILLIS )
                .withMaxRetries( -1 ) //unlimited retries.
                .build();
    }

    static Timeout<Object> getDefaultTimeoutPolicy()
    {
        return Timeout.builder( Duration.ofSeconds( 60 ) )
                .withInterrupt()
                .build();
    }

    @Override
    public void runAll( Map<String, ProvisioningProbe> probes, long timeout, TimeUnit timeoutUnit )
    {
        if( probes.values().isEmpty() )
        {
            return;
        }

        List<CompletableFuture<Boolean>> futures = startAllProbes( probes );

        waitUntilProbesComplete( futures, timeout, timeoutUnit );
    }

    private List<CompletableFuture<Boolean>> startAllProbes( Map<String, ProvisioningProbe> probes )
    {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for( Map.Entry<String, ProvisioningProbe> entry : probes.entrySet() )
        {
            CompletableFuture<Boolean> future = executor.getAsync( () -> {
                log.finest( () -> "Running provisioning probe: " + entry.getKey() );
                try
                {
                    boolean result = entry.getValue().ready( testKube );
                    log.finest( () -> "Provisioning probe returned " + result + " : " + entry.getKey() );
                    return result;
                }
                catch( Exception e )
                {
                    log.log( Level.WARNING, e, () -> "Provisioning probe completed exceptionally : " + entry.getKey() );
                    return false;
                }
            } );

            futures.add( future );
        }
        return futures;
    }

    private static void waitUntilProbesComplete( List<CompletableFuture<Boolean>> futures, long timeout, TimeUnit timeoutUnit )
    {
        CompletableFuture<Void> all = CompletableFuture.allOf( futures.toArray( new CompletableFuture<?>[ 0 ] ) );

        try
        {
            all.get( timeout, timeoutUnit );
        }
        catch( InterruptedException e )
        {
            Thread.currentThread().interrupt();
            throw new TestKubeException( "Provisioning probes were interrupted.", e );
        }
        catch( ExecutionException | TimeoutException e )
        {
            throw new TestKubeException( "Provisioning probes did not complete successfully.", e );
        }
    }

}

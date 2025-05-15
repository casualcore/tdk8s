/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller

import dev.failsafe.Failsafe
import dev.failsafe.FailsafeExecutor
import dev.failsafe.RetryPolicy
import dev.failsafe.Timeout
import se.laz.casual.test.tdk8s.TestKube
import se.laz.casual.test.tdk8s.TestKubeException
import se.laz.casual.test.tdk8s.probe.ProvisioningProbe
import spock.lang.Specification

import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ProvisioningProbeControllerTest extends Specification
{
    ProvisioningProbeControllerImpl instance
    TestKube testKube = Mock()

    Executor exec = Executors.newFixedThreadPool( 2 )

    FailsafeExecutor<Object> failsafeExec
    RetryPolicy<Object> retryPolicy = RetryPolicy.builder( ProvisioningProbeControllerImpl.getDefaultRetryPolicy(  ).getConfig(  ) )
            .withBackoff( 1, 2, ChronoUnit.MILLIS )
            .build(  )

    //RetryPolicy<Object> retryPolicy = ProvisioningProbeController.getDefaultRetryPolicy(  )

    //Timeout<Object> timeoutPolicy = Timeout.builder( Duration.ofSeconds( 1 ) ).withInterrupt(  ).build()
    Timeout<Object> timeoutPolicy = ProvisioningProbeControllerImpl.getDefaultTimeoutPolicy(  )


    def setup()
    {
        failsafeExec = Failsafe.with( timeoutPolicy ).compose( retryPolicy ).with( exec )
        instance = new ProvisioningProbeControllerImpl( testKube, failsafeExec )
    }

    def cleanup()
    {
        exec.shutdownNow(  )
        exec.awaitTermination( 1, TimeUnit.SECONDS )
    }

    def "Run check first fails."()
    {
        given:
        final CountDownLatch latch = new CountDownLatch(2 )
        ProvisioningProbe probe = ( t )-> {
            latch.countDown()
            return latch.getCount() == 0
        }

        when:
        instance.runAll( ["test": probe], 1, TimeUnit.SECONDS )

        then:
        noExceptionThrown(  )
        latch.getCount(  ) == 0
    }

    def "No probes runs without issue."()
    {
        when:
        instance.runAll( [:], 1, TimeUnit.SECONDS )

        then:
        noExceptionThrown(  )
    }

    def "Multiple probes, both pass."()
    {
        given:
        ProvisioningProbe probe1 = (t)-> { return true }
        ProvisioningProbe probe2 = (t)-> { return true }

        when:
        instance.runAll( ["p1":probe1, "p2": probe2 ], 1, TimeUnit.SECONDS )

        then:
        noExceptionThrown(  )
    }

    def "Multiple probes, one pass, one fail, fails overall."()
    {
        given:
        AtomicInteger p1Count = new AtomicInteger()
        AtomicInteger p2Count = new AtomicInteger()
        ProvisioningProbe probe1 = (t)-> {
            p1Count.getAndIncrement(  )
            return true
        }
        ProvisioningProbe probe2 = (t)-> {
            p2Count.getAndIncrement(  )
            return false
        }

        when:
        instance.runAll( ["p1":probe1, "p2": probe2 ], 100, TimeUnit.MILLISECONDS )

        then:
        thrown TestKubeException
        p1Count.get(  ) == 1
        p2Count.get(  ) >= 2
    }

    def "Multiple probes, one fails then pass, other just passes, passed overall."()
    {
        given:
        final CountDownLatch latch = new CountDownLatch(2 )
        ProvisioningProbe probe1 = ( t )-> {
            latch.countDown()
            return latch.getCount() == 0
        }
        ProvisioningProbe probe2 = (t)-> { return true }

        when:
        instance.runAll( ["p1":probe1, "p2": probe2 ], 1, TimeUnit.SECONDS )

        then:
        noExceptionThrown(  )
        latch.getCount(  ) == 0
    }

    def "Run check first throws exception, then pass."()
    {
        given:
        final CountDownLatch latch = new CountDownLatch(2 )
        ProvisioningProbe probe1 = ( t )-> {
            latch.countDown()
            if( latch.getCount(  ) != 0 )
            {
                throw new RuntimeException( "fake" )
            }
            return latch.getCount() == 0
        }

        when:
        instance.runAll( ["p1":probe1 ], 1, TimeUnit.SECONDS )

        then:
        noExceptionThrown(  )
        latch.getCount(  ) == 0
    }

    def "Run check always fails, times out."()
    {
        given:
        AtomicInteger count = new AtomicInteger()
        ProvisioningProbe probe = (t)-> {
            count.getAndIncrement(  )
            return false
        }

        when:
        instance.runAll( ["p1": probe ], 100, TimeUnit.MILLISECONDS )

        then:
        thrown TestKubeException
        count.get(  ) >= 2
    }

    def "Run check always throws exception, times out."()
    {
        given:
        AtomicInteger count = new AtomicInteger()
        ProvisioningProbe probe = (t)-> {
            count.getAndIncrement(  )
            throw new RuntimeException( "fake" )
        }

        when:
        instance.runAll( ["p1": probe ], 100, TimeUnit.MILLISECONDS )

        then:
        thrown TestKubeException
        count.get(  ) >= 2
    }

    def "Run checks interrupted?"()
    {
        given:
        Executor exec = Executors.newSingleThreadExecutor()
        instance = new ProvisioningProbeControllerImpl( testKube )

        CountDownLatch latch = new CountDownLatch( 1 )
        ProvisioningProbe probe = (t)->{
            latch.countDown(  )
            sleep( 10000 )
        }

        when:
        Future<?> task = exec.submit( ()-> {
            instance.runAll( ["p":probe], 10, TimeUnit.SECONDS )
            }
        )

        latch.await()
        exec.shutdownNow(  )
        assert exec.awaitTermination( 1, TimeUnit.SECONDS )
        unwrapFuture( task )

        then:
        thrown TestKubeException
    }

    <T>T unwrapFuture( Future<T> future )
    {
        try
        {
            return future.get()
        }
        catch( ExecutionException e )
        {
            throw e.getCause(  )
        }
    }
}

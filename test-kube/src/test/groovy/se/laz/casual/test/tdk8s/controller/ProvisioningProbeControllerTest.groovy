/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller

import dev.failsafe.RetryPolicy
import dev.failsafe.Timeout
import se.laz.casual.test.tdk8s.TestKube
import se.laz.casual.test.tdk8s.probe.ProvisioningProbe
import spock.lang.Specification

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ProvisioningProbeControllerTest extends Specification
{
    ProvisioningProbeController instance
    TestKube testKube = Mock()

    RetryPolicy<Object> retryPolicy = RetryPolicy.builder( ProvisioningProbeController.getDefaultRetryPolicy(  ).getConfig(  ) )
            .withBackoff( 1, 2, ChronoUnit.MILLIS )
            .build(  )

    Timeout<Object> timeoutPolicy = Timeout.builder( Duration.ofSeconds( 1 ) ).withInterrupt(  ).build()


    def setup()
    {
        instance = new ProvisioningProbeController( testKube, retryPolicy, timeoutPolicy )
    }

    def "Run check first fails."()
    {
        given:
        final CountDownLatch latch = new CountDownLatch(2 )
        ProvisioningProbe probe = ( t )-> {
            latch.countDown()
            println latch.getCount(  )
            return latch.getCount() == 0
        }

        when:
        instance.runAll( ["test": probe], 1, TimeUnit.SECONDS )

        then:
        noExceptionThrown(  )
        latch.getCount(  ) == 0
    }
}

/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.probe;

import se.laz.casual.test.tdk8s.TestKube;

@FunctionalInterface
public interface ProvisioningProbe
{
    /**
     * Probe a test kube to determine if provisioning is complete.
     * <br/>
     * A probe can be expected to be run multiple times until
     * it returns true.
     *
     * @param testKube the testKube instance to probe against.
     * @return if the provisioning probe was successful or not.
     */
    boolean ready( TestKube testKube );
}

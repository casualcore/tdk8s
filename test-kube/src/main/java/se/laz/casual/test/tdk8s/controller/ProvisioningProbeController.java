/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import se.laz.casual.test.tdk8s.probe.ProvisioningProbe;

import java.util.Map;
import java.util.concurrent.TimeUnit;


public interface ProvisioningProbeController
{
    /**
     * Runs all provisioning probes until they all complete successfully or timeout
     * has been exceeded.
     * <p>
     * When a probe completes successfully, it is not run again.
     * When a probe returns false or throws an exception it will be retried with
     * a backoff until the timeout or success occurs, which ever occurs first.
     * </p>
     *
     * @param probes the probes to run.
     * @param timeout the total maximum time to wait for all probes to complete successfully.
     * @param unit the timeout time unit.
     * @throws se.laz.casual.test.tdk8s.TestKubeException when probe execution times out without all succeeding.
     */
    void runAll( Map<String, ProvisioningProbe> probes, long timeout, TimeUnit unit );
}

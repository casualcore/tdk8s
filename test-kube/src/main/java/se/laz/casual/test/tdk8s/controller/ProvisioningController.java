/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

public interface ProvisioningController extends Provisionable
{
    /**
     * Initialises the managed resources without waiting until they are ready.
     */
    void initAsync();

    /**
     * Wait for the managed resource to become ready.
     */
    void waitUntilReady();

    /**
     * Deletes the managed resources without waiting until they are deleted.
     */
    void destroyAsync();

    /**
     * Wait for the managed resources to be deleted.
     */
    void waitUntilDestroyed();
}

/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import java.util.concurrent.CompletableFuture;

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

    /**
     * Scale the named resource to the requested number of replicas and wait
     * until complete.
     * <p>
     * When the scale operation is complete depends on the change in the number of
     * replicas compared to the existing number of replicas.
     * <ul>
     *     <li>unchanged - this is a no op.</li>
     *     <li>less - it is complete one the additional replicas are terminated.</li>
     *     <li>more - it is complete one the additional replicas are all ready.</li>
     * </ul>
     * This is enforced to ensure determinism around the state of the resources after
     * the scale operation.
     * </p>
     * <p>
     * If you do not wish to wait for the scale to be complete, you can instead
     * use {@link #scaleAsync(String, int)}.
     * </p>
     *
     * @param name deployment to scale.
     * @param replicas number of replicas requested.
     */
    void scale( String name, int replicas );

    /**
     * Scale the named resource to the requested number of replicas without waiting.
     *
     * @param name deployment to scale.
     * @param replicas number of replicas requested.
     * @return CompletableFuture of updated deployment after scaling operation.
     */
    CompletableFuture<Void> scaleAsync( String name, int replicas );
}

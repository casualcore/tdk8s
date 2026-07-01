/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.provisioning;

import se.laz.casual.test.tdk8s.store.ResourceNotFoundException;

import java.util.concurrent.CompletableFuture;

public interface ProvisioningController extends Provisionable, ProvisionableAsync
{
    /**
     * Scale the resource to the requested number of replicas and wait
     * until complete.
     * <p>
     * The resource name can be either the alias for the managed resource
     * or the actual underlying name of the resource inside the cluster.
     * </p>
     * <p>
     * When the scale operation is complete depends on the change in the number of
     * replicas compared to the existing number of replicas.
     * <ul>
     * <li>unchanged - this is a no op.</li>
     * <li>less - it is complete one the additional replicas are terminated.</li>
     * <li>more - it is complete one the additional replicas are all ready.</li>
     * </ul>
     * This is enforced to ensure determinism around the state of the resources after
     * the scale operation.
     * </p>
     * <p>
     * If you do not wish to wait for the scale to be complete, you can instead
     * use {@link #scaleAsync(String, int)}.
     * </p>
     *
     * @param resource to scale.
     * @param replicas number of replicas requested.
     * @throws ResourceNotFoundException if the resource was not found.
     */
    void scale( String resource, int replicas );

    /**
     * Scale the resource to the requested number of replicas without waiting.
     * <p>
     * The resource name can be either the alias for the managed resource
     * or the actual underlying name of the resource inside the cluster.
     * </p>
     * The returned future can be used to wait for completion as described
     * in {@link #scale(String, int)}.
     *
     * @param resource to scale.
     * @param replicas number of replicas requested.
     * @return CompletableFuture for the scaling operation.
     */
    CompletableFuture<Void> scaleAsync( String resource, int replicas );
}

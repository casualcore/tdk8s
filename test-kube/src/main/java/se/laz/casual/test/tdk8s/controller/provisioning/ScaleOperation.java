/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.provisioning;

import se.laz.casual.test.tdk8s.store.ResourceNotFoundException;

public interface ScaleOperation<T>
{
    /**
     * Scale the resource to the requested number of replicas.
     * <p>
     * The resource name can be either the alias for the managed resource
     * or the actual underlying name of the resource inside the cluster.
     * </p>
     *
     * @param resource to be scaled.
     * @param replicas number of replicas requested.
     * @return updated resource.
     * @throws ResourceNotFoundException if the resource was not found.
     */
    T scale( String resource, int replicas );
}

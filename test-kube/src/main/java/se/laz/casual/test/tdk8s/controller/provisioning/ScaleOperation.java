/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.provisioning;

public interface ScaleOperation<T>
{
    /**
     * Scale the resource to the requested number of replicas.
     *
     * @param resource to be scaled.
     * @param replicas number of replicas requested.
     * @return updated resource.
     */
    T scale( String resource, int replicas );
}

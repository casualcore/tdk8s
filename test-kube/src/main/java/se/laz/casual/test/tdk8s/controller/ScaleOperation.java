/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

public interface ScaleOperation<T>
{
    /**
     * Scale the resource to the requested number of replicas.
     *
     * @param resource to be scaled.
     * @param replicas number of replicas requested.
     * @return updated resource.
     */
    T scale( T resource, int replicas );
}

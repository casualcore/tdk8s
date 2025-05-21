/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;

import java.util.Optional;


public interface ResourceLookupController
{
    /**
     * Retrieves the PodResource for a pod matching the name.
     * </br>
     * The name can be either the alias for the managed resource or
     * the actual underlying name of the resource inside the cluster.
     *
     * @param name of the pod alias in the store, or pod name in the cluster.
     * @return PodResource of the found pod or empty if not found.
     */
    Optional<PodResource> getPodResource( String name );

    /**
     * Retrieves the RollableScalableResource<Deployment> for a deployment matching the name.
     * </br>
     * The name can be either the alias for the managed resource or
     * the actual underlying name of the resource inside the cluster.
     *
     * @param name of the deployment alias in the store, or deployment name in the cluster.
     * @return RollableScalableResource<Deployment> of the found deployment or empty if not found.
     */
    Optional<RollableScalableResource<Deployment>> getDeploymentResource( String name );

    /**
     * Retrieves the ServiceResource for a service matching the name.
     * </br>
     * The name can be either the alias for the managed resource or
     * the actual underlying name of the resource inside the cluster.
     *
     * @param name of the service alias in the store, or service name in the cluster.
     * @return ServiceResource of the found service or empty if not found.
     */
    Optional<ServiceResource<Service>> getServiceResource( String name );
}

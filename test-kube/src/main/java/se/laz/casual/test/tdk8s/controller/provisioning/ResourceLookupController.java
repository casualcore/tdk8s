/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.provisioning;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;

import java.util.List;
import java.util.Optional;


public interface ResourceLookupController
{
    /**
     * Retrieves the PodResource for a Pod matching the name.
     * <p>
     * The name can be either the alias for the managed Pod
     * or the actual underlying name of the Pod inside the cluster.
     * </p>
     *
     * @param name of the Pod.
     * @return PodResource of the found Pod or empty if not found.
     */
    Optional<PodResource> getPodAsResource( String name );

    /**
     * Retrieves the RollableScalableResource<Deployment> for a Deployment matching the name.
     * <p>
     * The name can be either the alias for the managed Deployment
     * or the actual underlying name of the Deployment inside the cluster.
     * </p>
     *
     * @param name of the Deployment.
     * @return RollableScalableResource<Deployment> of the found Deployment or empty if not found.
     */
    Optional<RollableScalableResource<Deployment>> getDeploymentAsResource( String name );

    /**
     * Retrieve the PodResources for the Deployment matching the name.
     * <p>
     * The name can be either the alias for the managed Deployment
     * or the actual underlying name of the Deployment inside the cluster.
     * </p>
     *
     * @param name of the Deployment.
     * @return List of PodResources for the Deployment. List is empty if none found.
     */
    List<PodResource> getPodsForDeploymentAsResources( String name );

    /**
     * Retrieve the Pods for the named Deployment directly from the cluster.
     * <p>
     * The name can be either the alias for the managed Deployment
     * or the actual underlying name of the Deployment inside the cluster.
     * </p>
     * <p>
     * The list of Pods returned is always retrieved from the cluster.
     * </p>
     *
     * @param name of the Deployment.
     * @return List of the Pods for the Deployment found. List is empty if none found.
     */
    List<Pod> retrievePodsForDeployment( String name );

    /**
     * Retrieves the ServiceResource for a Service matching the name.
     * <p>
     * The name can be either the alias for the managed Service
     * or the actual underlying name of the Service inside the cluster.
     * </p>
     *
     * @param name of the Service.
     * @return ServiceResource for the Service found, or empty if not found.
     */
    Optional<ServiceResource<Service>> getServiceAsResource( String name );

    /**
     * Find a Pod for the resource, returning the first match.
     * <p>
     * The resource name can be either the alias for the managed resource
     * or the actual underlying name of the resource inside the cluster.
     * </p>
     * <p>
     * Note: If the resource has multiple pods associated the first is used.
     * </p>
     *
     * Search order for resource Pods is:
     * <ol>
     * <li>Managed Pods</li>
     * <li>Unmanaged Pods</li>
     * <li>Managed Deployment Pods</li>
     * <li>Unmanaged Deployment Pods</li>
     * </ol>
     *
     * @param resource for which to find a Pod.
     * @return PodResource for the resource found or empty if not found.
     */
    Optional<PodResource> findFirstPodForResource( String resource );
}

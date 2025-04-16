/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.k8s.controller;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import se.laz.casual.test.k8s.store.ResourcesStore;

import java.util.Optional;

/**
 * Lookup resources by name, checking the cache, then cluster.
 * Return the resource instance of the found type, allowing for operations
 * to be performed.
 */
public class ResourceLookupController
{
    private final KubernetesClient client;
    private final ResourcesStore resourcesStore;

    public ResourceLookupController( KubernetesClient client, ResourcesStore resourcesStore )
    {
        this.client = client;
        this.resourcesStore = resourcesStore;
    }

    /**
     * Retrieve the PodResource for a pod matching the name.
     * Checks the store first, then cluster.
     *
     * @param name of the pod alias in the store, or pod name in the cluster.
     * @return PodResource of the found pod.
     */
    public Optional<PodResource> getPodResource( String name )
    {
        Pod pod = null;
        if( this.resourcesStore.containsPod( name ) )
        {
            pod = this.resourcesStore.getPod( name );
        }

        if( pod == null )
        {
            pod = this.client.pods().withName( name ).get();
        }

        if( pod == null )
        {
            return Optional.empty();
        }

        return Optional.of( this.client.pods().resource( pod ) );
    }

    /**
     * Retrieve the ServiceResource for a service matching the name.
     * Checks the store first, then cluster.
     *
     * @param name of the service alias in the store, or service name in the cluster.
     * @return ServiceResource of the found servce.
     */
    public Optional<ServiceResource<Service>> getServiceResource( String name )
    {
        Service service = null;
        if( this.resourcesStore.containsService( name ) )
        {
            service = this.resourcesStore.getService( name );
        }

        if( service == null )
        {
            service = this.client.services().withName( name ).get();
        }

        if( service == null )
        {
            return Optional.empty();
        }

        return Optional.of( this.client.services().resource( service ) );
    }
}

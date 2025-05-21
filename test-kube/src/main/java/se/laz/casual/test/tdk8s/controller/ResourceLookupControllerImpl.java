/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import se.laz.casual.test.tdk8s.store.ResourcesStore;

import java.util.Optional;

/**
 * Lookup resources by name, checking the store cache, then cluster.
 * Return the resource instance of the found type, allowing for operations
 * to be performed.
 */
public class ResourceLookupControllerImpl implements ResourceLookupController
{
    private final KubernetesClient client;
    private final ResourcesStore resourcesStore;

    public ResourceLookupControllerImpl( KubernetesClient client, ResourcesStore resourcesStore )
    {
        this.client = client;
        this.resourcesStore = resourcesStore;
    }

    @Override
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

    @Override
    public Optional<RollableScalableResource<Deployment>> getDeploymentResource( String name )
    {
        Deployment deployment = null;
        if( this.resourcesStore.containsDeployment( name ) )
        {
            deployment = this.resourcesStore.getDeployment( name );
        }

        if( deployment == null )
        {
            deployment = this.client.apps().deployments().withName( name ).get();
        }

        if( deployment == null )
        {
            return Optional.empty();
        }

        return Optional.of( this.client.apps().deployments().resource( deployment ) );
    }

    @Override
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

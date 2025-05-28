/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.provisioning;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import se.laz.casual.test.tdk8s.store.ResourcesStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Lookup resources by name, checking the store cache, then cluster.
 * Return the resource instance of the found type, allowing for operations
 * to be performed.
 */
public class ResourceLookupControllerImpl implements ResourceLookupController
{
    Logger log = Logger.getLogger( ResourceLookupControllerImpl.class.getName());

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
        return getOrRetrievePod( name ).map( pod -> this.client.pods().resource( pod ) );
    }

    private Optional<Pod> getOrRetrievePod( String name )
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
        return Optional.ofNullable( pod );
    }

    @Override
    public Optional<RollableScalableResource<Deployment>> getDeploymentResource( String name )
    {
        return getOrRetrieveDeployment( name )
                .map( deployment-> this.client.apps().deployments().resource( deployment ) );
    }

    @Override
    public List<PodResource> getDeploymentPodResources( String name )
    {
        List<Pod> pods = null;
        if( this.resourcesStore.containsDeploymentPods( name ) )
        {
            pods = this.resourcesStore.getDeploymentPods( name );
        }

        if( pods == null )
        {
            Deployment deployment = client.apps().deployments().withName( name ).get();
            if( deployment != null )
            {
                pods = findDeploymentPods( deployment );
            }
        }

        if( pods == null )
        {
            return Collections.emptyList();
        }

        List<PodResource> podResources = new ArrayList<>();
        for( Pod p : pods )
        {
            podResources.add( client.pods().resource( p ) );
        }
        return podResources;
    }

    @Override
    public List<Pod> findDeploymentPods( String name )
    {
        Optional<Deployment> deployment = getOrRetrieveDeployment( name );
        if( deployment.isPresent() )
        {
            return findDeploymentPods( deployment.get() );
        }
        return Collections.emptyList();
    }

    private List<Pod> findDeploymentPods( Deployment deployment )
    {
        return this.client.pods().withLabelSelector( deployment.getSpec().getSelector() ).list().getItems();
    }

    private Optional<Deployment> getOrRetrieveDeployment( String name )
    {
        Deployment d = null;
        if( this.resourcesStore.containsDeployment( name ) )
        {
            d = this.resourcesStore.getDeployment( name );
        }

        if( d == null )
        {
            d = this.client.apps().deployments().withName( name ).get();
        }
        return Optional.ofNullable( d );
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

    @Override
    public Optional<PodResource> findPodResource( String name )
    {
        return getPodResource( name ).or( () -> getFirstDeploymentPodResource( name ) );
    }

    private Optional<PodResource> getFirstDeploymentPodResource( String name )
    {
        List<PodResource> podList = getDeploymentPodResources( name );
        PodResource podResource = null;
        if( !podList.isEmpty() )
        {
            podResource = podList.get( 0 );
        }
        if( podList.size() > 1 )
        {
            log.warning( ()-> "Retrieved first pod from a deployment with multiple replicas." );
        }
        return Optional.ofNullable( podResource );
    }
}

/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.store;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import se.laz.casual.test.tdk8s.probe.ProvisioningProbe;

import java.util.Map;

/**
 * Store for the resources which are part of the TestKube.
 */
public class ResourcesStore
{
    private final PodStore podStore;
    private final ServiceStore serviceStore;
    private final ProvisioningProbeStore provisioningProbeStore;

    public ResourcesStore()
    {
        this.podStore = new PodStore();
        this.serviceStore = new ServiceStore();
        this.provisioningProbeStore = new ProvisioningProbeStore();
    }

    /**
     * Get a pod by name.
     *
     * @param name of the pod.
     * @return pod by name
     * @throws ResourceNotFoundException if pod not stored.
     */
    public Pod getPod( String name )
    {
        return this.podStore.get( name );
    }

    /**
     * Get all stored pods.
     *
     * @return map of pods stored.
     */
    public Map<String,Pod> getPods( )
    {
        return this.podStore.getAll();
    }

    /**
     * Check if the pod with name is stored.
     *
     * @param name of the pod.
     * @return if it is stored.
     */
    public boolean containsPod( String name )
    {
        return this.podStore.contains( name );
    }

    /**
     * Store a pod by name.
     * @param name of the pod to store.
     * @param pod to store.
     */
    public void putPod( String name, Pod pod )
    {
        this.podStore.put( name, pod );
    }

    /**
     * Store all pods provided.
     *
     * @param pods to store.
     */
    public void putPods( Map<String,Pod> pods )
    {
        this.podStore.putAll( pods );
    }

    /**
     * Remove pod by name.
     *
     * @param name of pod to remove.
     * @return the removed pod.
     * @throws ResourceNotFoundException if pod is not stored.
     */
    public Pod removePod( String name )
    {
        return this.podStore.remove( name );
    }

    /**
     * Get a service by name.
     *
     * @param name of the service.
     * @return service by name
     * @throws ResourceNotFoundException if service not stored.
     */
    public Service getService( String name )
    {
        return this.serviceStore.get( name );
    }

    /**
     * Get all stored services.
     *
     * @return map of services stored.
     */
    public Map<String,Service> getServices( )
    {
        return this.serviceStore.getAll();
    }

    /**
     * Check if the service with name is stored.
     *
     * @param name of the service.
     * @return if it is stored.
     */
    public boolean containsService( String name )
    {
        return this.serviceStore.contains( name );
    }

    /**
     * Store a service by name.
     * @param name of the service to store.
     * @param service to store.
     */
    public void putService( String name, Service service )
    {
        this.serviceStore.put( name, service );
    }

    /**
     * Store all services provided.
     *
     * @param services to store.
     */
    public void putServices( Map<String,Service> services )
    {
        this.serviceStore.putAll( services );
    }

    /**
     * Remove service by name.
     *
     * @param name of service to remove.
     * @return the removed service.
     * @throws ResourceNotFoundException if service is not stored.
     */
    public Service removeService( String name )
    {
        return this.serviceStore.remove( name );
    }

    /**
     * Get all stored provisioning probes.
     *
     * @return map of provisioning probes stored.
     */
    public Map<String, ProvisioningProbe> getProvisioningProbes()
    {
        return this.provisioningProbeStore.getAll();
    }

    /**
     * Store all provisioning probes provided.
     *
     * @param provisioningProbes to store.
     */
    public void putProvisioningProbes( Map<String, ProvisioningProbe> provisioningProbes )
    {
        this.provisioningProbeStore.putAll( provisioningProbes );
    }

}

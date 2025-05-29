/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.store;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import se.laz.casual.test.tdk8s.probe.ProvisioningProbe;

import java.util.List;
import java.util.Map;

/**
 * Store for the all managed resources which are part of the TestKube.
 */
public class ResourcesStore
{
    private final PodStore podStore;
    private final DeploymentStore deploymentStore;
    private final DeploymentPodsStore deploymentPodsStore;
    private final ServiceStore serviceStore;
    private final ProvisioningProbeStore provisioningProbeStore;

    public ResourcesStore()
    {
        this.podStore = new PodStore();
        this.deploymentStore = new DeploymentStore();
        this.deploymentPodsStore = new DeploymentPodsStore();
        this.serviceStore = new ServiceStore();
        this.provisioningProbeStore = new ProvisioningProbeStore();
    }

    /**
     * Get a Pod by name.
     *
     * @param name of the Pod.
     * @return stored Pod
     * @throws ResourceNotFoundException if Pod not stored.
     */
    public Pod getPod( String name )
    {
        return this.podStore.get( name );
    }

    /**
     * Get all stored Pods.
     *
     * @return map of Pods stored.
     */
    public Map<String,Pod> getPods( )
    {
        return this.podStore.getAll();
    }

    /**
     * Check if the Pod with name is stored.
     *
     * @param name of the Pod.
     * @return if named Pod is stored.
     */
    public boolean containsPod( String name )
    {
        return this.podStore.contains( name );
    }

    /**
     * Store a Pod by name.
     * @param name of the Pod to store.
     * @param pod to store.
     */
    public void putPod( String name, Pod pod )
    {
        this.podStore.put( name, pod );
    }

    /**
     * Store all Pods provided.
     *
     * @param pods to store.
     */
    public void putPods( Map<String,Pod> pods )
    {
        this.podStore.putAll( pods );
    }

    /**
     * Remove Pod by name.
     *
     * @param name of Pod to remove.
     * @return the removed Pod.
     * @throws ResourceNotFoundException if Pod is not stored.
     */
    public Pod removePod( String name )
    {
        return this.podStore.remove( name );
    }

    /**
     * Get a Deployment by name.
     *
     * @param name of the Deployment.
     * @return stored Deployment
     * @throws ResourceNotFoundException if Deployment not stored.
     */
    public Deployment getDeployment( String name )
    {
        return this.deploymentStore.get( name );
    }

    /**
     * Get all stored Deployments.
     *
     * @return map of Deployments stored.
     */
    public Map<String,Deployment> getDeployments( )
    {
        return this.deploymentStore.getAll();
    }

    /**
     * Check if the Deployment with name is stored.
     *
     * @param name of the Deployment.
     * @return if named Deployment is stored.
     */
    public boolean containsDeployment( String name )
    {
        return this.deploymentStore.contains( name );
    }

    /**
     * Store a Deployment by name.
     * @param name of the Deployment to store.
     * @param deployment to store.
     */
    public void putDeployment( String name, Deployment deployment )
    {
        this.deploymentStore.put( name, deployment );
    }

    /**
     * Store all Deployments provided.
     *
     * @param deployments to store.
     */
    public void putDeployments( Map<String,Deployment> deployments )
    {
        this.deploymentStore.putAll( deployments );
    }

    /**
     * Remove Deployment by name.
     *
     * @param name of Deployment to remove.
     * @return the removed Deployment.
     * @throws ResourceNotFoundException if deployment is not stored.
     */
    public Deployment removeDeployment( String name )
    {
        return this.deploymentStore.remove( name );
    }

    /**
     * Get Pods for Deployment by Deployment name.
     *
     * @param name of the Deployment.
     * @return Pods stored for the Deployment.
     * @throws ResourceNotFoundException if deployment not stored.
     */
    public List<Pod> getPodsForDeployment( String name )
    {
        return this.deploymentPodsStore.get( name );
    }

    /**
     * Get all stored Deployments Pods.
     *
     * @return map of Deployment Pods stored.
     */
    public Map<String, List<Pod>> getPodsForDeployments( )
    {
        return this.deploymentPodsStore.getAll();
    }

    /**
     * Check if the Pods for Deployment with Deployment name is stored.
     *
     * @param name of the Deployment.
     * @return if Pods for the Deployment are stored.
     */
    public boolean containsPodsForDeployment( String name )
    {
        return this.deploymentPodsStore.contains( name );
    }

    /**
     * Store Pods for a Deployment by Deployment name.
     *
     * @param name of the Deployment for which to store Pods.
     * @param pods to store.
     */
    public void putPodsForDeployment( String name, List<Pod> pods )
    {
        this.deploymentPodsStore.put( name, pods );
    }

    /**
     * Store all Pods for Deployments provided.
     *
     * @param podsForDeployments to store.
     */
    public void putPodsForDeployments( Map<String,List<Pod>> podsForDeployments )
    {
        this.deploymentPodsStore.putAll( podsForDeployments );
    }

    /**
     * Remove Pods for Deployment by Deployment name.
     *
     * @param name of Deployment to remove Pods.
     * @return the removed Pods for the Deployment.
     * @throws ResourceNotFoundException if Pods for the Deployment are not stored.
     */
    public List<Pod> removePodsForDeployment( String name )
    {
        return this.deploymentPodsStore.remove( name );
    }

    /**
     * Get a Service by name.
     *
     * @param name of the Service.
     * @return Service by name.
     * @throws ResourceNotFoundException if Service not stored.
     */
    public Service getService( String name )
    {
        return this.serviceStore.get( name );
    }

    /**
     * Get all stored Services.
     *
     * @return map of Services stored.
     */
    public Map<String,Service> getServices( )
    {
        return this.serviceStore.getAll();
    }

    /**
     * Check if the Service with name is stored.
     *
     * @param name of the Service.
     * @return if it is stored.
     */
    public boolean containsService( String name )
    {
        return this.serviceStore.contains( name );
    }

    /**
     * Store a Service by name.
     * @param name of the Service to store.
     * @param service to store.
     */
    public void putService( String name, Service service )
    {
        this.serviceStore.put( name, service );
    }

    /**
     * Store all Services provided.
     *
     * @param services to store.
     */
    public void putServices( Map<String,Service> services )
    {
        this.serviceStore.putAll( services );
    }

    /**
     * Remove Service by name.
     *
     * @param name of Service to remove.
     * @return the removed Service.
     * @throws ResourceNotFoundException if Service is not stored.
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

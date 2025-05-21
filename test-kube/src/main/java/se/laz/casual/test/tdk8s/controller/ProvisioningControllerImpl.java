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
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import se.laz.casual.test.tdk8s.store.ResourcesStore;
import se.laz.casual.test.tdk8s.watchers.DeleteWatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static se.laz.casual.test.tdk8s.TestKube.RESOURCE_LABEL_NAME;

/**
 * Controller responsible for provisioning and destruction of
 * resources within the TestKube.
 */
public class ProvisioningControllerImpl implements ProvisioningController
{
    private final List<Watch> watches = new ArrayList<>();
    private final List<DeleteWatcher<?>> deleteWatchers = new ArrayList<>();

    private final ProvisioningProbeController provisioningProbeController;
    private final KubernetesClient client;
    private final ResourcesStore resourcesStore;
    private final String labelValue;

    public ProvisioningControllerImpl( ProvisioningProbeController provisioningProbeController, KubernetesClient client, ResourcesStore resourcesStore, String labelValue )
    {
        this.provisioningProbeController = provisioningProbeController;
        this.client = client;
        this.resourcesStore = resourcesStore;
        this.labelValue = labelValue;
    }

    @Override
    public void init()
    {
        initAsync();
        waitUntilReady();
    }

    @Override
    public void initAsync()
    {
        for( Map.Entry<String, Deployment> entry : resourcesStore.getDeployments().entrySet() )
        {
            String name = entry.getKey();
            Deployment d = entry.getValue();
            Deployment updated = d.edit().editMetadata().addToLabels( RESOURCE_LABEL_NAME, labelValue ).endMetadata()
                    .editSpec()
                    .editTemplate()
                    .editMetadata()
                    .addToLabels( RESOURCE_LABEL_NAME, labelValue )
                    .endMetadata()
                    .endTemplate()
                    .endSpec()
                    .build();
            updated = client.apps().deployments().resource( updated ).serverSideApply();
            resourcesStore.putDeployment( name, updated );
        }

        for( Map.Entry<String, Pod> entry : resourcesStore.getPods().entrySet() )
        {
            String name = entry.getKey();
            Pod p = entry.getValue();
            Pod updated = p.edit().editMetadata().addToLabels( RESOURCE_LABEL_NAME, labelValue ).endMetadata().build();
            updated = client.pods().resource( updated ).serverSideApply();
            resourcesStore.putPod( name, updated );
        }

        for( Map.Entry<String, Service> entry : resourcesStore.getServices().entrySet() )
        {
            String name = entry.getKey();
            Service s = entry.getValue();
            Service updated = s.edit().editMetadata().addToLabels( RESOURCE_LABEL_NAME, labelValue ).endMetadata().build();
            updated = client.services().resource( updated ).serverSideApply();
            resourcesStore.putService( name, updated );
        }
    }

    @Override
    public void waitUntilReady()
    {
        for( Deployment d: resourcesStore.getDeployments().values() )
        {
            client.apps().deployments().resource( d ).waitUntilReady( 1, TimeUnit.MINUTES );
        }

        for( Pod p: resourcesStore.getPods().values() )
        {
            client.pods().resource( p ).waitUntilReady( 1, TimeUnit.MINUTES );
        }

        provisioningProbeController.runAll( resourcesStore.getProvisioningProbes(), 1, TimeUnit.MINUTES );
    }

    @Override
    public void destroy()
    {
        destroyAsync();
        waitUntilDestroyed();
    }

    @Override
    public void destroyAsync()
    {
        for( Deployment d: resourcesStore.getDeployments().values() )
        {
            DeleteWatcher<Deployment> deploymentWatcher = new DeleteWatcher<>();
            RollableScalableResource<Deployment> deploymentResource = client.apps().deployments().resource( d );
            Watch watch = deploymentResource.watch( deploymentWatcher );

            watches.add( watch );
            deleteWatchers.add( deploymentWatcher );

            //Add watches for all deployment pods to delete.
            List<Pod> podList =  client.pods().withLabelSelector( d.getSpec().getSelector() ).list().getItems();
            for( Pod p: podList )
            {
                DeleteWatcher<Pod> deploymentPodWatcher = new DeleteWatcher<>();
                Watch pWatch = client.pods().resource( p ).watch( deploymentPodWatcher );
                watches.add( pWatch );
                deleteWatchers.add( deploymentPodWatcher );
            }

            deploymentResource.delete();
        }

        for( Pod p: resourcesStore.getPods().values() )
        {
            DeleteWatcher<Pod> podWatcher = new DeleteWatcher<>();
            PodResource podResource = client.pods().resource( p );
            Watch watch = podResource.watch( podWatcher );

            watches.add( watch );
            deleteWatchers.add( podWatcher );

            podResource.delete();
        }
        for( Service s: resourcesStore.getServices().values() )
        {
            DeleteWatcher<Service> serviceWatcher = new DeleteWatcher<>();
            ServiceResource<Service> serviceResource = client.services().resource( s );
            Watch watch = serviceResource.watch( serviceWatcher );

            watches.add( watch );
            deleteWatchers.add( serviceWatcher );

            serviceResource.delete();
        }
    }

    @Override
    public void waitUntilDestroyed()
    {
        for( DeleteWatcher<?> deleteWatcher: deleteWatchers )
        {
            deleteWatcher.waitUntilDeleted();
        }
        deleteWatchers.clear();
        for( Watch watch: watches )
        {
            watch.close();
        }
        watches.clear();
    }
}

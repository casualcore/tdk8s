/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.dsl.PodResource;
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

    private final ProvisioningProbeController initProbeController;
    private final KubernetesClient client;
    private final ResourcesStore resourcesStore;
    private final String labelValue;

    public ProvisioningControllerImpl( ProvisioningProbeController initProbeController, KubernetesClient client, ResourcesStore resourcesStore, String labelValue )
    {
        this.initProbeController = initProbeController;
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
        for( Map.Entry<String, Pod> entry : resourcesStore.getPods().entrySet() )
        {
            String name = entry.getKey();
            Pod p = entry.getValue();
            Map<String,String> labels = p.getMetadata().getLabels();
            labels.put( RESOURCE_LABEL_NAME, labelValue );
            Pod updated = p.edit().editMetadata().withLabels( labels ).endMetadata().build();
            updated = client.pods().resource( updated ).serverSideApply();
            resourcesStore.putPod( name, updated );
        }

        for( Map.Entry<String, Service> entry : resourcesStore.getServices().entrySet() )
        {
            String name = entry.getKey();
            Service s = entry.getValue();
            Map<String,String> labels = s.getMetadata().getLabels();
            labels.put( RESOURCE_LABEL_NAME, labelValue );
            Service updated = s.edit().editMetadata().withLabels( labels ).endMetadata().build();
            updated = client.services().resource( updated ).serverSideApply();
            resourcesStore.putService( name, updated );
        }
    }

    @Override
    public void waitUntilReady()
    {
        for( Pod p: resourcesStore.getPods().values() )
        {
            client.pods().resource( p ).waitUntilReady( 1, TimeUnit.MINUTES );
        }

        initProbeController.runAll( resourcesStore.getProvisioningProbes(), 1, TimeUnit.MINUTES );
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
        for( Pod p: resourcesStore.getPods().values() )
        {
            DeleteWatcher<Pod> podWatcher = new DeleteWatcher<>();
            PodResource resource = client.pods().resource( p );
            Watch watch = resource.watch( podWatcher );

            watches.add( watch );
            deleteWatchers.add( podWatcher );

            resource.delete();
        }
        for( Service s: resourcesStore.getServices().values() )
        {
            DeleteWatcher<Service> serviceWatcher = new DeleteWatcher<>();
            ServiceResource<Service> resource = client.services().resource( s );
            Watch watch = resource.watch( serviceWatcher );

            watches.add( watch );
            deleteWatchers.add( serviceWatcher );

            resource.delete();
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

/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import se.laz.casual.test.tdk8s.store.ResourcesStore;
import se.laz.casual.test.tdk8s.watchers.DeleteResourceWatcher;
import se.laz.casual.test.tdk8s.watchers.DeleteWatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static se.laz.casual.test.tdk8s.TestKube.RESOURCE_LABEL_NAME;

public class ProvisioningServiceController implements ProvisionableAsync
{
    private final KubernetesClient client;
    private final ResourcesStore resourcesStore;
    private final String labelValue;
    private final List<DeleteResourceWatcher<Service>> deleteWatchers = new ArrayList<>();

    public ProvisioningServiceController( KubernetesClient client, ResourcesStore resourcesStore, String labelValue )
    {
        this.client = client;
        this.resourcesStore = resourcesStore;
        this.labelValue = labelValue;
    }

    @Override
    public void initAsync()
    {
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
        //no-op
    }

    @Override
    public void destroyAsync()
    {
        for( Service s: resourcesStore.getServices().values() )
        {
            ServiceResource<Service> serviceResource = client.services().resource( s );
            deleteWatchers.add( new DeleteResourceWatcher<>( new DeleteWatcher<>(), serviceResource ) );

            serviceResource.delete();
        }
    }

    @Override
    public void waitUntilDestroyed()
    {
        for( DeleteResourceWatcher<Service> watcher : deleteWatchers )
        {
            watcher.waitUntilDeleted();
        }
        deleteWatchers.clear();
    }
}

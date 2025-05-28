/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.PodResource;
import se.laz.casual.test.tdk8s.store.ResourcesStore;
import se.laz.casual.test.tdk8s.watchers.DeleteResourceWatcher;
import se.laz.casual.test.tdk8s.watchers.DeleteWatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static se.laz.casual.test.tdk8s.TestKube.RESOURCE_LABEL_NAME;

public class ProvisioningPodController implements ProvisionableAsync
{
    private final KubernetesClient client;
    private final ResourcesStore resourcesStore;
    private final String labelValue;
    private final List<DeleteResourceWatcher<Pod>> deleteWatchers = new ArrayList<>();

    public ProvisioningPodController( KubernetesClient client, ResourcesStore resourcesStore, String labelValue )
    {
        this.client = client;
        this.resourcesStore = resourcesStore;
        this.labelValue = labelValue;
    }

    @Override
    public void initAsync()
    {
        for( Map.Entry<String, Pod> entry : resourcesStore.getPods().entrySet() )
        {
            String name = entry.getKey();
            Pod p = entry.getValue();
            Pod updated = p.edit().editMetadata().addToLabels( RESOURCE_LABEL_NAME, labelValue ).endMetadata().build();
            updated = client.pods().resource( updated ).serverSideApply();
            resourcesStore.putPod( name, updated );
        }
    }

    @Override
    public void waitUntilReady()
    {
        for( Pod p: resourcesStore.getPods().values() )
        {
            client.pods().resource( p ).waitUntilReady( 1, TimeUnit.MINUTES );
        }
    }

    @Override
    public void destroyAsync()
    {
        for( Pod p: resourcesStore.getPods().values() )
        {
            PodResource podResource = client.pods().resource( p );
            deleteWatchers.add( new DeleteResourceWatcher<>( new DeleteWatcher<>(), podResource ) );

            podResource.delete();
        }
    }

    @Override
    public void waitUntilDestroyed()
    {
        for( DeleteResourceWatcher<Pod> watcher : deleteWatchers )
        {
            watcher.waitUntilDeleted();
        }
        deleteWatchers.clear();
    }
}

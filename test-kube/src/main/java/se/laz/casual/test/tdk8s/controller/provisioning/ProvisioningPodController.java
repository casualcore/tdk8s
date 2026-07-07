/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.provisioning;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.PodResource;
import se.laz.casual.test.tdk8s.store.ResourcesStore;
import se.laz.casual.test.tdk8s.watchers.DeleteResourceWatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.System.Logger.Level.DEBUG;
import static se.laz.casual.test.tdk8s.TestKube.RESOURCE_LABEL_NAME;

/**
 * Controls the provisioning of Pod resources.
 */
public class ProvisioningPodController implements ProvisionableAsync
{
    private static final System.Logger logger = System.getLogger( ProvisioningPodController.class.getName() );

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
            logger.log( DEBUG, () -> "Pod applied: " + name );
        }
    }

    @Override
    public void waitUntilReady()
    {
        for( Map.Entry<String, Pod> entry : resourcesStore.getPods().entrySet() )
        {
            client.pods().resource( entry.getValue() ).waitUntilReady( 1, TimeUnit.MINUTES );
            logger.log( DEBUG, () -> "Pod ready: " + entry.getKey() );
        }
    }

    @Override
    public void destroyAsync()
    {
        for( Map.Entry<String, Pod> entry : resourcesStore.getPods().entrySet() )
        {
            PodResource podResource = client.pods().resource( entry.getValue() );
            deleteWatchers.add( new DeleteResourceWatcher<>( podResource ) );

            podResource.delete();
            logger.log( DEBUG, () -> "Pod deleted: " + entry.getKey() );
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

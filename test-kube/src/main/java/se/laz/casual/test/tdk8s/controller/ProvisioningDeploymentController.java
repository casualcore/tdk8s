/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import se.laz.casual.test.tdk8s.store.ResourcesStore;
import se.laz.casual.test.tdk8s.watchers.DeleteResourceWatcher;
import se.laz.casual.test.tdk8s.watchers.DeleteWatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static se.laz.casual.test.tdk8s.TestKube.RESOURCE_LABEL_NAME;

public class ProvisioningDeploymentController implements ScaleOperation<Deployment>, ProvisionableAsync
{
    private final KubernetesClient client;
    private final ResourcesStore resourcesStore;
    private final ResourceLookupController lookupController;
    private final String labelValue;

    private final List<DeleteResourceWatcher<?>> deleteResourceWatchers = new ArrayList<>();

    public ProvisioningDeploymentController( KubernetesClient client, ResourcesStore store, ResourceLookupController lookupController, String labelValue )
    {
        this.client = client;
        this.resourcesStore = store;
        this.lookupController = lookupController;
        this.labelValue = labelValue;
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
    }

    @Override
    public void waitUntilReady()
    {
        for( Map.Entry<String,Deployment> entry: resourcesStore.getDeployments().entrySet() )
        {
            client.apps().deployments().resource( entry.getValue() ).waitUntilReady( 1, TimeUnit.MINUTES );
            updateStoredDeploymentPods( entry.getKey() );
        }
    }

    @Override
    public void destroyAsync()
    {
        for( Map.Entry<String,Deployment> entry: resourcesStore.getDeployments().entrySet() )
        {
            RollableScalableResource<Deployment> deploymentResource = client.apps().deployments().resource( entry.getValue() );

            deleteResourceWatchers.add( new DeleteResourceWatcher<>( new DeleteWatcher<>(), deploymentResource ) );

            //Add watches for all deployment pods to delete.
            List<PodResource> podList = lookupController.getDeploymentPodResources( entry.getKey() );
            deleteResourceWatchers.add( new DeleteResourceWatcher<>( new DeleteWatcher<>(podList.size()), podList ) );

            deploymentResource.delete();
        }
    }

    @Override
    public void waitUntilDestroyed()
    {
        for( DeleteResourceWatcher<?> watcher: deleteResourceWatchers )
        {
            watcher.waitUntilDeleted();
        }
        deleteResourceWatchers.clear();
    }

    @Override
    public Deployment scale( String name, int replicas )
    {
        ScaleDeploymentOperation s = new ScaleDeploymentOperation( lookupController );
        Deployment d = s.scale( name, replicas );
        if( resourcesStore.containsDeployment( name ) )
        {
            updateStoredDeploymentPods( name );
            this.resourcesStore.putDeployment( name, d );
        }
        return d;
    }

    private void updateStoredDeploymentPods( String name )
    {
        List<Pod> pods = lookupController.findDeploymentPods( name );
        resourcesStore.putDeploymentPods( name, pods );
    }
}

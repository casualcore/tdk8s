/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.provisioning;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import se.laz.casual.test.tdk8s.store.ResourcesStore;
import se.laz.casual.test.tdk8s.watchers.DeleteResourceWatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.System.Logger.Level.DEBUG;
import static se.laz.casual.test.tdk8s.TestKube.RESOURCE_LABEL_NAME;

/**
 * Controls the provisioning of Deployment resources.
 */
public class ProvisioningDeploymentController implements ScaleOperation<Deployment>, ProvisionableAsync
{
    private static final System.Logger logger = System.getLogger( ProvisioningDeploymentController.class.getName() );

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
            logger.log( DEBUG, ()-> "Deployment applied: " + name );
        }
    }

    @Override
    public void waitUntilReady()
    {
        for( Map.Entry<String,Deployment> entry: resourcesStore.getDeployments().entrySet() )
        {
            client.apps().deployments().resource( entry.getValue() ).waitUntilReady( 1, TimeUnit.MINUTES );
            updateStoredDeploymentPods( entry.getKey() );
            logger.log( DEBUG, ()-> "Deployment ready: " + entry.getKey() );
        }
    }

    @Override
    public void destroyAsync()
    {
        for( Map.Entry<String,Deployment> entry: resourcesStore.getDeployments().entrySet() )
        {
            RollableScalableResource<Deployment> deploymentResource = client.apps().deployments().resource( entry.getValue() );

            deleteResourceWatchers.add( new DeleteResourceWatcher<>( deploymentResource ) );

            //Add watches for all deployment pods to delete.
            List<PodResource> podList = lookupController.getPodsForDeploymentAsResources( entry.getKey() );
            deleteResourceWatchers.add( new DeleteResourceWatcher<>( podList ) );

            deploymentResource.delete();
            logger.log( DEBUG, ()-> "Deployment deleted: " + entry.getKey() );
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
        List<Pod> pods = lookupController.retrievePodsForDeployment( name );
        logger.log( DEBUG, ()-> "Found " + pods.size() + " Pod(s) for Deployment: " + name );
        resourcesStore.putPodsForDeployment( name, pods );
    }
}

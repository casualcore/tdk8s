/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.provisioning;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import se.laz.casual.test.tdk8s.TestKubeException;
import se.laz.casual.test.tdk8s.store.ResourceNotFoundException;
import se.laz.casual.test.tdk8s.watchers.DeleteResourceWatcher;
import se.laz.casual.test.tdk8s.watchers.DeleteWatcher;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ScaleDeploymentOperation implements ScaleOperation<Deployment>
{
    private final ResourceLookupController lookupController;

    private DeleteResourceWatcher<Pod> watcher;

    public ScaleDeploymentOperation( ResourceLookupController lookupController )
    {
        this.lookupController = lookupController;
    }

    @Override
    public Deployment scale( String name, int replicas )
    {
        RollableScalableResource<Deployment> resource = lookupController.getDeploymentResource( name )
                .orElseThrow( () -> new ResourceNotFoundException( "Resource not found: " + name ) );
        Deployment deployment = resource.get();
        int currentReplicas = deployment.getSpec().getReplicas();
        if( currentReplicas == replicas )
        {
            return deployment;
        }

        preScale( name, replicas, currentReplicas );

        resource.scale( replicas );

        postScale();

        return resource.waitUntilReady( 1, TimeUnit.MINUTES );
    }

    private void preScale( String name, int replicas, int currentReplicas )
    {
        // If scaling down, add delete watches to monitor when the correct number of deployment pods
        // have been deleted.
        // Otherwise, the deployment scale operation returns immediately and the deployment is "ready",
        // even though "additional" pods are still running awaiting completion of their termination.
        if( currentReplicas > replicas )
        {

            List<PodResource> pods = this.lookupController.getDeploymentPodResources( name );
            if( pods.size() != currentReplicas )
            {
                throw new TestKubeException( "Unexpected number of current replicas found: " + pods.size() + ", expected: " + currentReplicas );
            }
            watcher = new DeleteResourceWatcher<>( new DeleteWatcher<>( currentReplicas - replicas ), pods );
        }
    }

    private void postScale( )
    {
        // Wait and close watchers created in preScale.
        if( watcher != null )
        {
            watcher.waitUntilDeleted();
        }
    }
}

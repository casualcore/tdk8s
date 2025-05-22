/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import se.laz.casual.test.tdk8s.TestKubeException;
import se.laz.casual.test.tdk8s.watchers.DeleteWatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DeploymentScaleController implements ScaleOperation<Deployment>
{
    private final KubernetesClient client;

    List<Watch> watches = new ArrayList<>();
    DeleteWatcher<Pod> watcher = null;

    public DeploymentScaleController( KubernetesClient client )
    {
        this.client = client;
    }

    @Override
    public Deployment scale( Deployment deployment, int replicas )
    {
        int currentReplicas = deployment.getSpec().getReplicas();
        if( currentReplicas == replicas )
        {
            return deployment;
        }

        preScale( deployment, replicas, currentReplicas );

        Deployment scaled = this.client.apps().deployments().resource( deployment ).scale( replicas );

        postScale();

        return this.client.apps().deployments().resource( scaled ).waitUntilReady( 1, TimeUnit.MINUTES );
    }

    private void preScale( Deployment deployment, int replicas, int currentReplicas )
    {
        // If scaling down, add delete watches to monitor when the correct number of deployment pods
        // have been deleted.
        // Otherwise, the deployment scale operation returns immediately and the deployment is "ready",
        // even though "additional" pods are still running awaiting completion of their termination.
        if( currentReplicas > replicas )
        {
            watcher = new DeleteWatcher<>( currentReplicas - replicas );
            List<Pod> pods = this.client.pods().withLabelSelector( deployment.getSpec().getSelector() ).list().getItems();
            if( pods.size() != currentReplicas )
            {
                throw new TestKubeException( "Unexpected number of current replicas found: " + pods.size() + ", expected: " + currentReplicas );
            }
            for( Pod p : pods )
            {
                watches.add( this.client.pods().resource( p ).watch( watcher ) );
            }
        }
    }

    private void postScale( )
    {
        // Wait and close watchers created in preScale.
        if( watcher != null )
        {
            watcher.waitUntilDeleted();
        }
        if( !watches.isEmpty() )
        {
            for( Watch w : watches )
            {
                w.close();
            }
        }
    }
}

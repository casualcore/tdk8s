/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.provisioning;

import io.fabric8.kubernetes.client.KubernetesClient;
import se.laz.casual.test.tdk8s.store.ResourcesStore;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Controller responsible for provisioning and destruction of
 * resources within the TestKube.
 */
public class ProvisioningControllerImpl implements ProvisioningController
{
    private final ProvisioningProbeController provisioningProbeController;
    private final ResourcesStore resourcesStore;


    private final ProvisioningDeploymentController provisioningDeploymentController;
    private final ProvisioningPodController provisioningPodController;
    private final ProvisioningServiceController provisioningServiceController;

    public ProvisioningControllerImpl( ProvisioningProbeController provisioningProbeController, KubernetesClient client, ResourcesStore resourcesStore, ResourceLookupController lookupController, String labelValue )
    {
        this.provisioningProbeController = provisioningProbeController;
        this.resourcesStore = resourcesStore;

        this.provisioningDeploymentController = new ProvisioningDeploymentController( client, resourcesStore, lookupController, labelValue );
        this.provisioningPodController = new ProvisioningPodController( client, resourcesStore, labelValue );
        this.provisioningServiceController = new ProvisioningServiceController( client, resourcesStore, labelValue );
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
        this.provisioningDeploymentController.initAsync();
        this.provisioningPodController.initAsync();
        this.provisioningServiceController.initAsync();
    }

    @Override
    public void waitUntilReady()
    {
        this.provisioningDeploymentController.waitUntilReady();
        this.provisioningPodController.waitUntilReady();
        this.provisioningServiceController.waitUntilReady();

        this.provisioningProbeController.runAll( resourcesStore.getProvisioningProbes(), 1, TimeUnit.MINUTES );
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
        this.provisioningDeploymentController.destroyAsync();
        this.provisioningPodController.destroyAsync();
        this.provisioningServiceController.destroyAsync();
    }

    @Override
    public void waitUntilDestroyed()
    {
        this.provisioningDeploymentController.waitUntilDestroyed();
        this.provisioningPodController.waitUntilDestroyed();
        this.provisioningServiceController.waitUntilDestroyed();
    }

    @Override
    public void scale( String name, int replicas )
    {
        this.provisioningDeploymentController.scale( name, replicas );
    }

    @Override
    public CompletableFuture<Void> scaleAsync( String name, int replicas )
    {
        return CompletableFuture.runAsync( ()-> scale( name, replicas ) );
    }
}

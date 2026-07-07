/*
 * Copyright (c) 2025 - 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.provisioning;

import io.fabric8.kubernetes.client.KubernetesClient;
import se.laz.casual.test.tdk8s.store.ResourcesStore;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Controller responsible for provisioning and destruction of managed resources within the TestKube.
 * Delegates operations to specialised controllers for Deployment, Pod, Service and Provisioning Probes.
 */
public class ProvisioningControllerImpl implements ProvisioningController
{
    private final ProvisioningProbeController provisioningProbeController;
    private final ResourcesStore resourcesStore;


    private final ProvisioningDeploymentController provisioningDeploymentController;
    private final ProvisioningPodController provisioningPodController;
    private final ProvisioningServiceController provisioningServiceController;
    private final ProvisioningConfigMapController provisioningConfigMapController;

    public ProvisioningControllerImpl( ProvisioningProbeController provisioningProbeController, KubernetesClient client, ResourcesStore resourcesStore, ResourceLookupController lookupController, String labelValue )
    {
        this.provisioningProbeController = provisioningProbeController;
        this.resourcesStore = resourcesStore;

        this.provisioningDeploymentController = new ProvisioningDeploymentController( client, resourcesStore, lookupController,
                                                                                      labelValue );
        this.provisioningPodController = new ProvisioningPodController( client, resourcesStore, labelValue );
        this.provisioningServiceController = new ProvisioningServiceController( client, resourcesStore, labelValue );
        this.provisioningConfigMapController = new ProvisioningConfigMapController( client, resourcesStore, labelValue );
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
        this.provisioningConfigMapController.initAsync();
        this.provisioningDeploymentController.initAsync();
        this.provisioningPodController.initAsync();
        this.provisioningServiceController.initAsync();
    }

    @Override
    public void waitUntilReady()
    {
        this.provisioningConfigMapController.waitUntilReady();
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
        this.provisioningConfigMapController.destroyAsync();
    }

    @Override
    public void waitUntilDestroyed()
    {
        this.provisioningDeploymentController.waitUntilDestroyed();
        this.provisioningPodController.waitUntilDestroyed();
        this.provisioningServiceController.waitUntilDestroyed();
        this.provisioningConfigMapController.waitUntilDestroyed();
    }

    @Override
    public void scale( String resource, int replicas )
    {
        this.provisioningDeploymentController.scale( resource, replicas );
    }

    @Override
    public CompletableFuture<Void> scaleAsync( String resource, int replicas )
    {
        return CompletableFuture.runAsync( () -> scale( resource, replicas ) );
    }
}

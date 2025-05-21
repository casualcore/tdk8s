/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.api.model.PodList
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.ServiceBuilder
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.Watch
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.dsl.AppsAPIGroupDSL
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable
import io.fabric8.kubernetes.client.dsl.MixedOperation
import io.fabric8.kubernetes.client.dsl.PodResource
import io.fabric8.kubernetes.client.dsl.RollableScalableResource
import io.fabric8.kubernetes.client.dsl.ServiceResource
import se.laz.casual.test.tdk8s.TestKube
import se.laz.casual.test.tdk8s.probe.ProvisioningProbe
import se.laz.casual.test.tdk8s.store.ResourcesStore
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class ProvisioningControllerTest extends Specification
{
    TestKube testKube = Mock()
    ProvisioningProbeController provisioningProbeController = new ProvisioningProbeControllerImpl( testKube )
    KubernetesClient client = Mock()
    String label = UUID.randomUUID(  ).toString(  )
    ResourcesStore store = new ResourcesStore()

    String podName = "my-pod"
    String deploymentName = "my-deployment"
    String serviceName = "my-service"

    ProvisioningControllerImpl instance

    Pod initialPod = new PodBuilder(  ).withNewMetadata(  ).withName( podName ).addToLabels( "a","b" ).endMetadata(  ).build(  )
    Pod expectedPod = initialPod.edit(  ).editMetadata(  )
            .addToLabels( TestKube.RESOURCE_LABEL_NAME, label )
            .endMetadata(  )
            .build(  )

    Deployment initialDeployment = new DeploymentBuilder().withNewMetadata(  ).withName( deploymentName ).addToLabels( "a","b" ).endMetadata(  )
            .withNewSpec(  )
            .withNewSelector(  )
                .addToMatchLabels( ["app":"fun"] )
            .endSelector(  )
            .withNewTemplate(  )
            .withNewMetadata(  ).addToLabels( "c","d" ).endMetadata(  )
            .endTemplate(  )
            .endSpec(  )
            .build(  )
    Deployment expectedDeployment = initialDeployment.edit(  ).editMetadata(  )
            .addToLabels( TestKube.RESOURCE_LABEL_NAME, label )
            .endMetadata(  )
            .editSpec(  )
            .editTemplate(  )
            .editMetadata(  ).addToLabels( TestKube.RESOURCE_LABEL_NAME, label ).endMetadata(  )
            .endTemplate(  )
            .endSpec(  )
            .build(  )

    Service initialSvc = new ServiceBuilder(  ).withNewMetadata(  ).withName( serviceName ).addToLabels( "a","b" ).endMetadata(  ).build(  )
    Service expectedSvc = initialSvc.edit(  ).editMetadata(  )
            .addToLabels( TestKube.RESOURCE_LABEL_NAME, label )
            .endMetadata(  )
            .build(  )

    def setup()
    {
        instance = new ProvisioningControllerImpl( provisioningProbeController, client, store, label )
    }

    def "init applies managed resources in store with label applied and updates store, waits until resources are ready."()
    {
        given:
        mockCreatePod( expectedPod )
        mockWaitForPod( expectedPod )
        mockCreateDeployment( expectedDeployment )
        mockWaitForDeployment( expectedDeployment )
        mockCreateService( expectedSvc )

        store.putPod( podName, initialPod )
        store.putDeployment( deploymentName, initialDeployment )
        store.putService( serviceName, initialSvc )

        when:
        instance.init(  )

        then:
        store.getPod( podName  ) == expectedPod
        store.getDeployment( deploymentName ) == expectedDeployment
        store.getService( serviceName ) == expectedSvc
    }

    def "init async managed resources in store with label applied and updates store."()
    {
        given:
        mockCreatePod( expectedPod )
        mockCreateDeployment( expectedDeployment )
        mockCreateService( expectedSvc )

        store.putPod( podName, initialPod )
        store.putDeployment( deploymentName, initialDeployment )
        store.putService( serviceName, initialSvc )

        when:
        instance.initAsync(  )

        then:
        store.getPod( podName  ) == expectedPod
        store.getDeployment( deploymentName ) == expectedDeployment
        store.getService( serviceName ) == expectedSvc
    }

    def "wait until ready, waits for pods and deployments in store."()
    {
        given:
        mockWaitForPod( expectedPod )
        mockWaitForDeployment( expectedDeployment )

        store.putPod( podName, expectedPod )
        store.putDeployment( deploymentName, expectedDeployment )
        store.putService( serviceName, expectedSvc )

        when:
        instance.waitUntilReady(  )

        then:
        noExceptionThrown(  )
    }

    def "wait until ready, with init probes."()
    {
        given:
        ProvisioningProbe probe = Mock()
        1* probe.ready( testKube ) >> false
        1* probe.ready( testKube ) >> true

        store.putProvisioningProbes( ["p": probe ] )

        when:
        instance.waitUntilReady(  )

        then:
        noExceptionThrown(  )
    }

    def "destroy deletes all managed resources in store, waits until deletion complete."()
    {
        given:
        store.putPod( podName, expectedPod )
        store.putDeployment( deploymentName, expectedDeployment )
        store.putService( serviceName, expectedSvc )

        mockDeleteDeployment( expectedDeployment )
        mockDeletePod( expectedPod )
        mockDeleteService( expectedSvc )

        when:
        instance.destroy(  )

        then:
        noExceptionThrown(  )
    }

    def "destroyAsync deletes all managed resources in store."()
    {
        given:
        store.putPod( podName, expectedPod )
        store.putDeployment( deploymentName, expectedDeployment )
        store.putService( serviceName, expectedSvc )

        mockDeleteDeployment( expectedDeployment )
        mockDeletePod( expectedPod )
        mockDeleteService( expectedSvc )

        when:
        instance.destroyAsync(  )

        then:
        noExceptionThrown(  )

        when:
        instance.waitUntilDestroyed(  )

        then:
        noExceptionThrown(  )
    }

    void mockCreatePod( Pod pod )
    {
        PodResource resource = mockPodResource( pod )
        1* resource.serverSideApply(  ) >> pod
    }

    MixedOperation mockClientPods( )
    {
        MixedOperation mo = Mock()
        1* client.pods(  ) >> mo
        return mo
    }

    PodResource mockPodResource( Pod pod )
    {
        MixedOperation mo = mockClientPods(  )
        PodResource resource = Mock()
        1* mo.resource( pod ) >> resource
        return resource
    }

    void mockWaitForPod( Pod pod )
    {
        PodResource resource = mockPodResource( pod  )
        1* resource.waitUntilReady( 1, TimeUnit.MINUTES ) >> pod
    }

    void mockCreateDeployment( Deployment deployment )
    {
        RollableScalableResource<Deployment> resource = mockDeploymentResource( deployment )
        1* resource.serverSideApply(  ) >> deployment
    }

    MixedOperation mockClientDeployments( )
    {
        AppsAPIGroupDSL apps = Mock()
        1* client.apps(  ) >> apps
        MixedOperation mo = Mock()
        1* apps.deployments(  ) >> mo
        return mo
    }

    RollableScalableResource<Deployment> mockDeploymentResource( Deployment deployment )
    {
        MixedOperation mo = mockClientDeployments(  )
        RollableScalableResource<Deployment> resource = Mock()
        1* mo.resource( deployment ) >> resource
        return resource
    }

    void mockWaitForDeployment( Deployment deployment )
    {
        RollableScalableResource<Deployment> resource = mockDeploymentResource( deployment  )
        1* resource.waitUntilReady( 1, TimeUnit.MINUTES ) >> deployment
    }

    void mockCreateService( Service service )
    {
        ServiceResource resource = mockServiceResource( service )
        1* resource.serverSideApply(  ) >> service
    }

    MixedOperation mockClientServices( )
    {
        MixedOperation mo = Mock()
        1* client.services(  ) >> mo
        return mo
    }

    ServiceResource mockServiceResource( Service service )
    {
        MixedOperation mo = mockClientServices(  )
        ServiceResource resource = Mock()
        1* mo.resource( service ) >> resource
        return resource
    }

    PodResource mockWatchDeletePod( Pod pod )
    {
        PodResource resource = mockPodResource( pod )
        Watch watch = Mock()
        1* resource.watch( _ ) >> { Watcher watcher ->
            watcher.eventReceived( Watcher.Action.DELETED, pod )
            return watch
        }
        1* watch.close()
        return resource
    }

    void mockDeletePod( Pod pod )
    {
        PodResource pr = mockWatchDeletePod( pod )
        1* pr.delete()
    }

    void mockDeleteDeployment( Deployment deployment )
    {
        RollableScalableResource<Deployment> dr = mockDeploymentResource( deployment )

        Watch watch = Mock()
        1* dr.watch( _ ) >> { Watcher watcher ->
            watcher.eventReceived( Watcher.Action.DELETED, deployment )
            return watch
        }
        1* watch.close()
        1* dr.delete()

        MixedOperation mixed = mockClientPods(  )
        FilterWatchListDeletable fwld = Mock()
        1* mixed.withLabelSelector( deployment.getSpec(  ).getSelector(  ) ) >> fwld
        PodList podList = Mock()
        1* fwld.list() >> podList
        Pod pod = Mock()
        1* podList.getItems(  ) >> [pod]

        mockWatchDeletePod( pod )
    }

    void mockDeleteService( Service service )
    {
        ServiceResource sr = mockServiceResource( service )
        Watch watch = Mock( )
        1* sr.watch( _ ) >> { Watcher watcher ->
            watcher.eventReceived( Watcher.Action.DELETED, service )
            return watch
        }
        1* watch.close()
        1* sr.delete()
    }

}

/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.ServiceBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.Watch
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.dsl.MixedOperation
import io.fabric8.kubernetes.client.dsl.PodResource
import io.fabric8.kubernetes.client.dsl.ServiceResource
import se.laz.casual.test.tdk8s.TestKube
import se.laz.casual.test.tdk8s.probe.ProvisioningProbe
import se.laz.casual.test.tdk8s.store.ResourcesStore
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class ProvisioningControllerTest extends Specification
{
    TestKube testKube = Mock()
    ProvisioningProbeController provisioningProbeController = new ProvisioningProbeController( testKube )
    KubernetesClient client = Mock()
    String label = UUID.randomUUID(  ).toString(  )
    ResourcesStore store = new ResourcesStore()

    String podName = "my-pod"
    String serviceName = "my-service"

    ProvisioningControllerImpl instance

    Pod initialPod = new PodBuilder(  ).withNewMetadata(  ).withName( podName ).endMetadata(  ).build(  )
    Pod expectedPod = initialPod.edit(  ).editMetadata(  )
            .addToLabels( TestKube.RESOURCE_LABEL_NAME, label )
            .endMetadata(  )
            .build(  )

    Service initialSvc = new ServiceBuilder(  ).withNewMetadata(  ).withName( serviceName ).endMetadata(  ).build(  )
    Service expectedSvc = initialSvc.edit(  ).editMetadata(  )
            .addToLabels( TestKube.RESOURCE_LABEL_NAME, label )
            .endMetadata(  )
            .build(  )

    def setup()
    {
        instance = new ProvisioningControllerImpl( provisioningProbeController, client, store, label )
    }

    def "init applies pods and services in store with label applied and updates store, waits until pods ready."()
    {
        given:
        mockCreatePod( expectedPod )
        mockWaitForPod( expectedPod )
        mockCreateService( expectedSvc )

        store.putPod( podName, initialPod )
        store.putService( serviceName, initialSvc )

        when:
        instance.init(  )

        then:
        store.getPod( podName  ) == expectedPod
        store.getService( serviceName ) == expectedSvc
    }

    def "init async pods and services in store with label applied and updates store."()
    {
        given:
        mockCreatePod( expectedPod )
        mockCreateService( expectedSvc )

        store.putPod( podName, initialPod )
        store.putService( serviceName, initialSvc )

        when:
        instance.initAsync(  )

        then:
        store.getPod( podName  ) == expectedPod
        store.getService( serviceName ) == expectedSvc
    }

    def "wait until ready, waits for pods in store."()
    {
        given:
        mockWaitForPod( expectedPod )

        store.putPod( podName, expectedPod )
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

        store.putProvisioningProbes( ["": probe ] )

        when:
        instance.waitUntilReady(  )

        then:
        noExceptionThrown(  )
    }

    def "destroy deletes all pods and services in store, waits until deletion complete."()
    {
        given:
        store.putPod( podName, expectedPod )
        store.putService( serviceName, expectedSvc )

        mockDeletePod( expectedPod )
        mockDeleteService( expectedSvc )

        when:
        instance.destroy(  )

        then:
        noExceptionThrown(  )
    }

    def "destroyAsync deletes all pods and services in store."()
    {
        given:
        store.putPod( podName, expectedPod )
        store.putService( serviceName, expectedSvc )

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

    void mockDeletePod( Pod pod )
    {
        PodResource resource = mockPodResource( pod )
        Watch watch = Mock()
        1* resource.watch( _ ) >> { Watcher watcher ->
            watcher.eventReceived( Watcher.Action.DELETED, pod )
            return watch
        }
        1* watch.close()
        1* resource.delete()
    }

    void mockDeleteService( Service service )
    {
        ServiceResource resource = mockServiceResource( service )
        Watch watch = Mock( )
        1* resource.watch( _ ) >> { Watcher watcher ->
            watcher.eventReceived( Watcher.Action.DELETED, service )
            return watch
        }
        1* watch.close()
        1* resource.delete()
    }

}

/*
 * Copyright (c) 2025 - 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.provisioning

import io.fabric8.kubernetes.api.model.ConfigMap
import io.fabric8.kubernetes.api.model.ConfigMapBuilder
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.ServiceBuilder
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.Watch
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.dsl.AppsAPIGroupDSL
import io.fabric8.kubernetes.client.dsl.MixedOperation
import io.fabric8.kubernetes.client.dsl.PodResource
import io.fabric8.kubernetes.client.dsl.Resource
import io.fabric8.kubernetes.client.dsl.RollableScalableResource
import io.fabric8.kubernetes.client.dsl.ServiceResource
import se.laz.casual.test.tdk8s.TestKube
import se.laz.casual.test.tdk8s.TestKubeException
import se.laz.casual.test.tdk8s.probe.ProvisioningProbe
import se.laz.casual.test.tdk8s.store.ResourceNotFoundException
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
    ResourceLookupController lookupController = Mock()

    String podName = "my-pod"
    String deploymentName = "my-deployment"
    String serviceName = "my-service"
    String configMapName = "my-config-map"

    ProvisioningControllerImpl instance

    Pod initialPod = new PodBuilder(  ).withNewMetadata(  ).withName( podName ).addToLabels( "a","b" ).endMetadata(  ).build(  )
    Pod expectedPod = initialPod.edit(  ).editMetadata(  )
            .addToLabels( TestKube.RESOURCE_LABEL_NAME, label )
            .endMetadata(  )
            .build(  )

    Deployment initialDeployment = new DeploymentBuilder().withNewMetadata(  ).withName( deploymentName ).addToLabels( "a","b" ).endMetadata(  )
            .withNewSpec(  )
            .withReplicas( 1 )
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

    ConfigMap initialConfigMap = new ConfigMapBuilder(  ).withNewMetadata(  ).withName( configMapName ).addToLabels("a", "b"  ).endMetadata(  ).build(  )
    ConfigMap expectedConfigMap = initialConfigMap.edit(  ).editMetadata(  )
            .addToLabels( TestKube.RESOURCE_LABEL_NAME, label )
            .endMetadata(  )
            .build(  )

    def setup()
    {
        instance = new ProvisioningControllerImpl( provisioningProbeController, client, store, lookupController, label )
    }

    def "init applies managed resources in store with label applied and updates store, waits until resources are ready."()
    {
        given:
        mockCreateConfigMap( expectedConfigMap )
        mockCreatePod( expectedPod )
        mockWaitForPod( expectedPod )
        mockCreateDeployment( expectedDeployment )
        mockWaitForDeployment( expectedDeployment )
        mockFindDeploymentPods( deploymentName, [expectedPod] )
        mockCreateService( expectedSvc )

        store.putPod( podName, initialPod )
        store.putDeployment( deploymentName, initialDeployment )
        store.putService( serviceName, initialSvc )
        store.putConfigMap( configMapName, initialConfigMap )

        when:
        instance.init(  )

        then:
        store.getPod( podName  ) == expectedPod
        store.getDeployment( deploymentName ) == expectedDeployment
        store.getPodsForDeployment( deploymentName ) == [expectedPod]
        store.getService( serviceName ) == expectedSvc
        store.getConfigMap( configMapName ) == expectedConfigMap
    }

    def "init async managed resources in store with label applied and updates store."()
    {
        given:
        mockCreateConfigMap( expectedConfigMap )
        mockCreatePod( expectedPod )
        mockCreateDeployment( expectedDeployment )
        mockCreateService( expectedSvc )

        store.putPod( podName, initialPod )
        store.putDeployment( deploymentName, initialDeployment )
        store.putService( serviceName, initialSvc )
        store.putConfigMap( configMapName, initialConfigMap )

        when:
        instance.initAsync(  )

        then:
        store.getPod( podName  ) == expectedPod
        store.getDeployment( deploymentName ) == expectedDeployment
        store.getService( serviceName ) == expectedSvc
        store.getConfigMap( configMapName ) == expectedConfigMap
    }

    def "wait until ready, waits for pods and deployments in store."()
    {
        given:
        mockWaitForPod( expectedPod )
        mockWaitForDeployment( expectedDeployment )
        mockFindDeploymentPods( deploymentName, [expectedPod] )

        store.putPod( podName, expectedPod )
        store.putDeployment( deploymentName, expectedDeployment )
        store.putService( serviceName, expectedSvc )
        store.putConfigMap( configMapName, expectedConfigMap )

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
        store.putPodsForDeployment( deploymentName, [expectedPod] )
        store.putService( serviceName, expectedSvc )
        store.putConfigMap( configMapName, expectedConfigMap )

        mockDeleteDeployment( expectedDeployment )
        mockWatchDeleteDeploymentPods( deploymentName, 1 )
        mockDeletePod( expectedPod )
        mockDeleteService( expectedSvc )
        mockDeleteConfigMap( expectedConfigMap )

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
        store.putPodsForDeployment( deploymentName, [expectedPod] )
        store.putService( serviceName, expectedSvc )
        store.putConfigMap( configMapName, expectedConfigMap )

        mockDeleteDeployment( expectedDeployment )
        mockWatchDeleteDeploymentPods( deploymentName, 2 )
        mockDeletePod( expectedPod )
        mockDeleteService( expectedSvc )
        mockDeleteConfigMap( expectedConfigMap )

        when:
        instance.destroyAsync(  )

        then:
        noExceptionThrown(  )

        when:
        instance.waitUntilDestroyed(  )

        then:
        noExceptionThrown(  )
    }

    def "scale from 1 to 0."()
    {
        given:
        store.putPodsForDeployment( deploymentName, [expectedPod] )
        store.putDeployment( deploymentName, expectedDeployment )
        RollableScalableResource<Deployment> resource = Mock()
        1* lookupController.getDeploymentAsResource( deploymentName ) >> Optional.ofNullable( resource )
        1* resource.get() >> expectedDeployment
        PodResource podResource = Mock()
        1* lookupController.getPodsForDeploymentAsResources( deploymentName ) >> [podResource]
        mockWatchDeletePodResource( podResource, expectedPod )

        1* resource.scale( 0 )
        1* resource.waitUntilReady( 1, TimeUnit.MINUTES ) >> expectedDeployment
        1* lookupController.retrievePodsForDeployment( deploymentName ) >> []

        when:
        instance.scale( deploymentName, 0 )

        then:
        store.getPodsForDeployment( deploymentName ) == []
    }

    def "scale async from 1 to 0."()
    {
        given:
        store.putPodsForDeployment( deploymentName, [expectedPod] )
        store.putDeployment( deploymentName, expectedDeployment )
        RollableScalableResource<Deployment> resource = Mock()
        1* lookupController.getDeploymentAsResource( deploymentName ) >> Optional.ofNullable( resource )
        1* resource.get() >> expectedDeployment
        PodResource podResource = Mock()
        1* lookupController.getPodsForDeploymentAsResources( deploymentName ) >> [podResource]
        mockWatchDeletePodResource( podResource, expectedPod )

        1* resource.scale( 0 )
        1* resource.waitUntilReady( 1, TimeUnit.MINUTES ) >> expectedDeployment
        1* lookupController.retrievePodsForDeployment( deploymentName ) >> []

        when:
        instance.scaleAsync( deploymentName, 0 ).join(  )

        then:
        store.getPodsForDeployment( deploymentName ) == []
    }

    def "scale from 1 to 2."()
    {
        given:
        store.putPodsForDeployment( deploymentName, [expectedPod] )
        store.putDeployment( deploymentName, expectedDeployment )
        RollableScalableResource<Deployment> resource = Mock()
        1* lookupController.getDeploymentAsResource( deploymentName ) >> Optional.ofNullable( resource )
        1* resource.get() >> expectedDeployment

        1* resource.scale( 2 )
        1* resource.waitUntilReady( 1, TimeUnit.MINUTES ) >> expectedDeployment
        1* lookupController.retrievePodsForDeployment( deploymentName ) >> [expectedPod, expectedPod]

        when:
        instance.scale( deploymentName, 2 )

        then:
        store.getPodsForDeployment( deploymentName ) == [expectedPod, expectedPod]
    }

    def "scale no change."()
    {
        store.putPodsForDeployment( deploymentName, [expectedPod] )
        store.putDeployment( deploymentName, expectedDeployment )
        RollableScalableResource<Deployment> resource = Mock()
        1* lookupController.getDeploymentAsResource( deploymentName ) >> Optional.ofNullable( resource )
        1* resource.get() >> expectedDeployment
        1* lookupController.retrievePodsForDeployment( deploymentName ) >> [expectedPod]

        when:
        instance.scale( deploymentName, 1 )

        then:
        0* resource.scale_
        store.getDeployment( deploymentName ) == expectedDeployment
        store.getPodsForDeployment( deploymentName ) == [expectedPod]
    }

    def "scale deployment doesn't exist throws exception."()
    {
        given:
        1* lookupController.getDeploymentAsResource(deploymentName  ) >> Optional.empty(  )

        when:
        instance.scale( deploymentName, 2 )

        then:
        thrown ResourceNotFoundException
    }

    def "scale, unexpected state for number of replicas, throws exception."()
    {
        store.putPodsForDeployment( deploymentName, [expectedPod] )
        store.putDeployment( deploymentName, expectedDeployment )
        RollableScalableResource<Deployment> resource = Mock()
        1* lookupController.getDeploymentAsResource( deploymentName ) >> Optional.ofNullable( resource )
        1* resource.get() >> expectedDeployment
        1* lookupController.getPodsForDeploymentAsResources( deploymentName ) >> [expectedPod, expectedPod]

        when:
        instance.scale( deploymentName, 0 )

        then:
        thrown TestKubeException
        store.getPodsForDeployment( deploymentName ) == [expectedPod]
    }

    def "scale unmanaged deployment."()
    {
        given:
        RollableScalableResource<Deployment> resource = Mock()
        1* lookupController.getDeploymentAsResource( deploymentName ) >> Optional.ofNullable( resource )
        1* resource.get() >> expectedDeployment

        when:
        instance.scale( deploymentName, 1 )

        then:
        !store.containsDeployment( deploymentName )
        !store.containsPodsForDeployment( deploymentName )
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

    void mockFindDeploymentPods( String name, List<Pod> pods )
    {
        1* lookupController.retrievePodsForDeployment( name ) >> pods
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

    void mockCreateConfigMap( ConfigMap configMap )
    {
        Resource<ConfigMap> resource = mockConfigMapResource( configMap )
        1* resource.serverSideApply(  ) >> configMap
    }

    MixedOperation mockClientConfigMaps( )
    {
        MixedOperation mo = Mock()
        1* client.configMaps(  ) >> mo
        return mo
    }

    Resource<ConfigMap> mockConfigMapResource( ConfigMap configMap )
    {
        MixedOperation mo = mockClientConfigMaps(  )
        Resource<ConfigMap> resource = Mock()
        1* mo.resource( configMap ) >> resource
        return resource
    }

    PodResource mockWatchDeletePod( Pod pod )
    {
        PodResource resource = mockPodResource( pod )
        resource = mockWatchDeletePodResource( resource, pod )
        return resource
    }

    PodResource mockWatchDeletePodResource( PodResource resource, Pod pod )
    {
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
    }

    void mockWatchDeleteDeploymentPods( String name, int count )
    {
        List<PodResource> podResources = new ArrayList()
        for( int i=0;i< count; i++ )
        {
            Pod pod = Mock()
            PodResource podResource = Mock()
            podResources.add( podResource )
            mockWatchDeletePodResource( podResource, pod )
        }

        1* lookupController.getPodsForDeploymentAsResources( name ) >> podResources
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

    void mockDeleteConfigMap( ConfigMap configMap )
    {
        Resource<ConfigMap> cr = mockConfigMapResource( configMap )
        Watch watch = Mock( )
        1* cr.watch( _ ) >> { Watcher watcher ->
            watcher.eventReceived( Watcher.Action.DELETED, configMap )
            return watch
        }
        1* watch.close()
        1* cr.delete()
    }

}

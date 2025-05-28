/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.provisioning


import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodList
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.AppsAPIGroupDSL
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable
import io.fabric8.kubernetes.client.dsl.MixedOperation
import io.fabric8.kubernetes.client.dsl.PodResource
import io.fabric8.kubernetes.client.dsl.RollableScalableResource
import io.fabric8.kubernetes.client.dsl.ServiceResource
import se.laz.casual.test.tdk8s.store.ResourcesStore
import spock.lang.Specification

class ResourceLookupControllerTest extends Specification
{

    KubernetesClient client = Mock( KubernetesClient )

    ResourcesStore store = new ResourcesStore()

    ResourceLookupController instance

    String name = "myobject"
    Deployment deployment = new DeploymentBuilder().withNewMetadata(  ).withName( "test" ).endMetadata(  )
            .withNewSpec(  ).withNewSelector().addToMatchLabels( ["app":"test"]).endSelector( ).endSpec(  )
            .build(  )

    def setup()
    {
        instance = new ResourceLookupControllerImpl( client, store )
    }

    def "Get pod, present in store."()
    {
        given:
        Pod pod = Mock( Pod )
        PodResource pr = mockPodResource( pod )

        store.putPod( name, pod )

        when:
        Optional<PodResource> actual = instance.getPodResource( name )

        then:
        actual.isPresent(  )
        actual.get() == pr
    }

    def "Get pod, not present in store, is present on cluster."()
    {
        given:
        Pod pod = Mock( Pod )
        mockPodWithNameGet( name, pod )
        PodResource pr = mockPodResource( pod )

        when:
        Optional<PodResource> actual = instance.getPodResource( name )

        then:
        actual.isPresent(  )
        actual.get(  ) == pr
    }

    def "Get pod, not present in store, not present on cluster, returns empty."()
    {
        given:
        mockPodWithNameGet( name, null )

        when:
        Optional<PodResource> actual = instance.getPodResource( name )

        then:
        actual.isEmpty(  )
    }

    def "Get deployment, present in store."()
    {
        given:
        Deployment deployment = Mock( Deployment )
        RollableScalableResource<Deployment> dr = mockDeploymentResource( deployment )

        store.putDeployment( name, deployment )

        when:
        Optional<RollableScalableResource<Deployment>> actual = instance.getDeploymentResource( name )

        then:
        actual.isPresent(  )
        actual.get() == dr
    }

    def "Get deployment, not present in store, is present on cluster."()
    {
        given:
        Deployment deployment = Mock( Deployment )

        mockDeploymentWithNameGet( name, deployment )
        RollableScalableResource<Deployment> dr = mockDeploymentResource( deployment )

        when:
        Optional<RollableScalableResource<Deployment>> actual = instance.getDeploymentResource( name )

        then:
        actual.isPresent(  )
        actual.get(  ) == dr
    }

    def "Get deployment, not present in store, not present on cluster, returns empty."()
    {
        given:
        mockDeploymentWithNameGet( name, null )

        when:
        Optional<RollableScalableResource<Deployment>> actual = instance.getDeploymentResource( name )

        then:
        actual.isEmpty(  )
    }

    def "Get service, present in store."()
    {
        given:
        Service service = Mock( Service )
        ServiceResource sr = mockServiceResource( service )

        store.putService( name, service )

        when:
        Optional<ServiceResource> actual = instance.getServiceResource( name )

        then:
        actual.isPresent(  )
        actual.get() == sr
    }

    def "Get service, not present in store, is present on cluster."()
    {
        given:
        Service service = Mock( Service )
        mockServiceWithNameGet( name, service )
        ServiceResource sr = mockServiceResource( service )

        when:
        Optional<ServiceResource> actual = instance.getServiceResource( name )

        then:
        actual.isPresent(  )
        actual.get() == sr
    }

    def "Get service, not present in store, not present on cluster, returns empty."()
    {
        given:
        mockServiceWithNameGet( name, null )

        when:
        Optional<ServiceResource> actual = instance.getServiceResource( name )

        then:
        actual.isEmpty()
    }

    def "Find managed deployment pods"()
    {
        given:
        Pod p = Mock()
        mockPodsWithSelector( deployment, [p] )
        store.putDeployment( name, deployment )

        when:
        List<Pod> actual = instance.findDeploymentPods( name )

        then:
        actual == [p]
    }

    def "Find managed deployment pods, no pods"()
    {
        given:
        mockPodsWithSelector( deployment, [] )
        store.putDeployment( name, deployment )

        when:
        List<Pod> actual = instance.findDeploymentPods( name )

        then:
        actual == []
    }

    def "Find unmanaged deployment pods"()
    {
        given:
        Pod p = Mock()

        mockDeploymentWithNameGet( name, deployment )
        mockPodsWithSelector( deployment, [p] )

        when:
        List<Pod> actual = instance.findDeploymentPods( name )

        then:
        actual == [p]
    }

    def "Find unmanaged deployment pods, no pods"()
    {
        given:
        mockDeploymentWithNameGet( name, deployment )
        mockPodsWithSelector( deployment, [] )

        when:
        List<Pod> actual = instance.findDeploymentPods( name )

        then:
        actual == []
    }

    def "Find deployment pods, no deployments, no pods."()
    {
        given:
        mockDeploymentWithNameGet( name, null )

        when:
        List<Pod> actual = instance.findDeploymentPods( name )

        then:
        actual == []
    }

    def "Get managed deployment pod resources"()
    {
        given:
        Pod pod = Mock()
        store.putDeploymentPods( name, [pod] )
        PodResource resource = mockPodResource(pod )
        List<PodResource> expected = [resource]

        when:
        List<PodResource> actual = instance.getDeploymentPodResources( name )

        then:
        actual == expected
    }

    def "Get unmanaged deployment pods resources"()
    {
        given:
        Pod pod = Mock()
        mockDeploymentWithNameGet( name, deployment )
        mockPodsWithSelector( deployment, [pod] )
        PodResource resource = mockPodResource( pod )
        List<PodResource> expected = [resource]

        when:
        List<PodResource> actual = instance.getDeploymentPodResources( name )

        then:
        actual == expected
    }

    def "Get deployment pods, deployment doesn't exist."()
    {
        given:
        mockDeploymentWithNameGet( name, null )

        when:
        List<PodResource> actual = instance.getDeploymentPodResources( name )

        then:
        actual == []
    }

    def "Get pod or first deployment pod resource, is a managed pod"()
    {
        given:
        Pod p = Mock()
        PodResource pr = mockPodResource( p )
        store.putPod( name,  p )


        when:
        Optional<PodResource> actual = instance.findPodResource( name )

        then:
        actual.isPresent(  )
        actual.get() == pr
    }

    def "Get pod or first deployment pod resource, un managed pod"()
    {
        given:
        Pod p = Mock()
        mockPodWithNameGet( name, p )
        PodResource pr = mockPodResource( p )


        when:
        Optional<PodResource> actual = instance.findPodResource( name )

        then:
        actual.isPresent(  )
        actual.get(  ) == pr
    }

    def "Get pod or first deployment pod resource, managed deployment"()
    {
        given:
        Pod p = Mock()
        mockPodWithNameGet( name, null )
        PodResource pr = mockPodResource( p )


        store.putDeploymentPods( name, [p] )

        when:
        Optional<PodResource> actual = instance.findPodResource( name )

        then:
        actual.isPresent(  )
        actual.get() == pr
    }

    def "Get pod or first deployment pod resource, un managed deployment"()
    {
        given:
        Pod pod = Mock()
        mockPodWithNameGet( name, null )
        mockDeploymentWithNameGet( name, deployment )
        mockPodsWithSelector( deployment, [pod] )
        PodResource pr = mockPodResource( pod )

        when:
        Optional<PodResource> actual = instance.findPodResource( name )

        then:
        actual.isPresent(  )
        actual.get() == pr
    }

    def "Get pod or first deployment pod resource, none"()
    {
        given:
        mockPodWithNameGet( name, null )
        mockDeploymentWithNameGet( name, null )

        when:
        Optional<PodResource> actual = instance.findPodResource( name )

        then:
        actual.isEmpty(  )
    }

    def "Get pod or first deployment pod resource, multiple deployment pods"()
    {
        given:
        Pod p1 = Mock()
        Pod p2 = Mock()
        mockPodWithNameGet( name, null )
        store.putDeploymentPods( name, [p1, p2] )
        PodResource pr = mockPodResource( p1 )
        mockPodResource( p2 )

        when:
        Optional<PodResource> actual = instance.findPodResource( name )

        then:
        actual.isPresent(  )
        actual.get(  ) == pr
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

    void mockPodWithNameGet( String name, Pod pod )
    {
        MixedOperation mo = mockClientPods()
        PodResource resource = Mock()
        1* resource.get(  ) >> pod
        1* mo.withName( name ) >> resource
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

    void mockDeploymentWithNameGet( String name, Deployment deployment )
    {
        MixedOperation mo = mockClientDeployments(  )
        RollableScalableResource<Deployment> resource = Mock()
        1* resource.get() >> deployment
        1* mo.withName( name ) >> resource
    }

    void mockPodsWithSelector( Deployment d, List<Pod> pods )
    {
        MixedOperation mo = mockClientPods(  )
        FilterWatchListDeletable fwld = Mock()
        1* mo.withLabelSelector( d.getSpec(  ).getSelector(  ) ) >> fwld
        PodList podList = Mock()
        1* fwld.list() >> podList
        1* podList.getItems(  ) >> pods
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

    void mockServiceWithNameGet( String name, Service service )
    {
        MixedOperation mo = mockClientServices(  )
        ServiceResource<Service> resource = Mock()
        1* resource.get() >> service
        1* mo.withName( name ) >> resource
    }


}

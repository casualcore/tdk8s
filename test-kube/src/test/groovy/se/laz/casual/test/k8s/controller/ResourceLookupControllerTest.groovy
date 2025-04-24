/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.k8s.controller

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.MixedOperation
import io.fabric8.kubernetes.client.dsl.PodResource
import io.fabric8.kubernetes.client.dsl.ServiceResource
import se.laz.casual.test.k8s.store.ResourcesStore
import spock.lang.Specification

class ResourceLookupControllerTest extends Specification
{

    KubernetesClient client = Mock( KubernetesClient )

    ResourcesStore store = new ResourcesStore()

    ResourceLookupController instance

    def setup()
    {
        instance = new ResourceLookupControllerImpl( client, store )
    }

    def "Get pod, present in store."()
    {
        given:
        String podName = "mypod"
        Pod pod = Mock( Pod )
        PodResource pr = Mock( PodResource )
        MixedOperation mixed = Mock( MixedOperation )
        1* client.pods(  ) >> mixed
        1* mixed.resource( pod ) >> pr

        store.putPod( podName, pod )

        when:
        Optional<PodResource> actual = instance.getPodResource( podName )

        then:
        actual.isPresent(  )
        actual.get() == pr
    }

    def "Get pod, not present in store, is present on cluster."()
    {
        given:
        String podName = "mypod"
        Pod pod = Mock( Pod )
        PodResource pr = Mock( PodResource )
        MixedOperation mixed = Mock( MixedOperation )
        2* client.pods(  ) >> mixed
        1* mixed.withName( podName ) >> pr
        1* pr.get(  ) >> pod
        1* mixed.resource( pod ) >> pr

        when:
        Optional<PodResource> actual = instance.getPodResource( podName )

        then:
        actual.isPresent(  )
        actual.get(  ) == pr
    }

    def "Get pod, not present in store, not present on cluster, throws ResourceNotFoundException."()
    {
        given:
        String podName = "mypod"
        Pod pod = Mock( Pod )
        PodResource pr = Mock( PodResource )
        MixedOperation mixed = Mock( MixedOperation )
        1* client.pods(  ) >> mixed
        1* mixed.withName( podName ) >> pr
        1* pr.get(  ) >> null

        when:
        Optional<PodResource> actual = instance.getPodResource( podName )

        then:
        0* mixed.resource( pod )
        actual.isEmpty(  )
    }

    def "Get service, present in store."()
    {
        given:
        String name = "service"
        Service service = Mock( Service )
        ServiceResource sr = Mock( ServiceResource )
        MixedOperation mixed = Mock( MixedOperation )
        1* client.services(  ) >> mixed
        1* mixed.resource( service ) >> sr

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
        String name = "service"
        Service service = Mock( Service )
        ServiceResource sr = Mock( ServiceResource )
        MixedOperation mixed = Mock( MixedOperation )
        2* client.services(  ) >> mixed
        1* mixed.withName( name ) >> sr
        1* sr.get(  ) >> service
        1* mixed.resource( service ) >> sr

        when:
        Optional<ServiceResource> actual = instance.getServiceResource( name )

        then:
        actual.isPresent(  )
        actual.get() == sr
    }

    def "Get service, not present in store, not present on cluster, throws ResourceNotFoundException."()
    {
        given:
        String name = "mypod"
        Service service = Mock( Service )
        ServiceResource sr = Mock( ServiceResource )
        MixedOperation mixed = Mock( MixedOperation )
        1* client.services(  ) >> mixed
        1* mixed.withName( name ) >> sr
        1* sr.get(  ) >> null

        when:
        Optional<ServiceResource> actual = instance.getServiceResource( name )

        then:
        0* mixed.resource( service )
        actual.isEmpty()
    }

}

/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.ServiceBuilder
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import se.laz.casual.test.tdk8s.controller.KubeController
import se.laz.casual.test.tdk8s.probe.ProvisioningProbe
import spock.lang.Shared
import spock.lang.Specification

class TestKubeTest extends Specification
{
    @Shared
    String podName = "single-pod-resource"

    @Shared
    Pod pod = new PodBuilder(  )
            .withNewMetadata(  )
            .withName( podName )
            .endMetadata(  )
            .build(  )

    @Shared
    String serviceName = "single-service-resource"

    @Shared
    Service service = new ServiceBuilder(  )
            .withNewMetadata(  )
            .withName( serviceName )
            .endMetadata(  )
            .build()

    @Shared
    String deploymentName = "single-deployment-resource"

    @Shared
    Deployment deployment = new DeploymentBuilder(  )
        .withNewMetadata(  )
            .withName( deploymentName )
        .endMetadata(  )
            .build(  )

    TestKube instance

    def setup()
    {
        instance = TestKube.newBuilder().build()
    }

    def "Get client instance, default."()
    {
        expect:
        instance.getClient() != null
    }

    def "Get client instance, provided."()
    {
        given:
        KubernetesClient client = new KubernetesClientBuilder().build(  )

        when:
        instance = TestKube.newBuilder().client( client ).build()

        then:
        instance.getClient() == client
    }

    def "Get label, default."()
    {
        expect:
        instance.getLabel(  ) != null
    }

    def "Get label, provided."()
    {
        given:
        String label = "TestLabel"

        when:
        instance = TestKube.newBuilder().label( label ).build()

        then:
        instance.getLabel() == label
        instance.toString(  ).contains( label )
    }

    def "Create TestKube with a single pod resource."()
    {
        when:
        instance = TestKube.newBuilder().addPod( podName, pod ).build()

        then:
        instance.getPods( ) == [(podName): pod]
    }

    def "Create TestKube with a single pod and a single service."()
    {
        when:
        instance = TestKube.newBuilder(  )
                .addPod( podName, pod )
                .addService( serviceName, service ).build()

        then:
        instance.getPods(  ) == [(podName): pod]
        instance.getServices() == [(serviceName):service]
    }

    def "Create TestKube with a single deployment."()
    {
        when:
        instance = TestKube.newBuilder(  )
                .addDeployment( deploymentName, deployment )
                .build(  )

        then:
        instance.getDeployments() == [(deploymentName): deployment ]
    }

    def "ResourcesStore accessible."()
    {
        expect:
        instance.getResourcesStore(  )  != null
    }

    def "KubeController accessible."()
    {
        expect:
        instance.getController(  ) != null
    }

    def "Init and destroy TestKube invokes controller."()
    {
        given:
        KubeController kc = Mock()
        instance = TestKube.newBuilder().kubeController( kc ).build(  )

        when:
        instance.init(  )

        then:
        1* kc.init(  )

        when:
        instance.destroy(  )

        then:
        1* kc.destroy(  )
    }

    def "Get connection delegated to contorller."()
    {
        given:
        String name = "my-service"
        int port = 8080

        KubeController kc = Mock()
        instance = TestKube.newBuilder().kubeController( kc ).build(  )

        when:
        instance.getConnection( name, port )

        then:
        1* kc.getConnection( name, port )
    }

    def "Get init probes."()
    {
        expect:
        instance.getResourcesStore().getProvisioningProbes(  ) == [:]
    }

    def "Add and get init probes."()
    {
        given:
        ProvisioningProbe probe = { -> return false }
        ProvisioningProbe probe2 = { -> return true }

        Map<String,ProvisioningProbe> expected = ["probe": probe, "probe2": probe2 ]

        instance = TestKube.newBuilder(  )
                .addProvisioningProbe( "probe", probe )
                .addProvisioningProbe( "probe2", probe2 )
                .build()

        when:
        Map<String,ProvisioningProbe> actual = instance.getResourcesStore(  ).getProvisioningProbes()

        then:
        actual == expected
    }
}

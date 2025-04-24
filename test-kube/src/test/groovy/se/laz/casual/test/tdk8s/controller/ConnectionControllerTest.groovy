/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller


import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.client.LocalPortForward
import io.fabric8.kubernetes.client.dsl.PodResource
import io.fabric8.kubernetes.client.dsl.ServiceResource
import se.laz.casual.test.tdk8s.connection.ConnectionException
import se.laz.casual.test.tdk8s.connection.KubeConnection
import se.laz.casual.test.tdk8s.connection.KubeConnectionType
import se.laz.casual.test.tdk8s.sample.WildflyResources
import spock.lang.Shared
import spock.lang.Specification

class ConnectionControllerTest extends Specification
{
    ResourceLookupController rc = Mock()
    NetworkController nc = Mock()
    RuntimeController runc = Mock()
    ConnectionController instance

    String serviceName = WildflyResources.SIMPLE_WILDFLY_SERVICE_NAME
    @Shared int port = 8080

    def setup()
    {
        instance = new ConnectionControllerImpl( rc, nc, runc )
    }

    def "Get connection service is available."()
    {
        given:
        Service service = WildflyResources.SIMPLE_WILDFLY_SERVICE
        ServiceResource<Service> resource = Mock()
        1* rc.getServiceResource( serviceName ) >> Optional.of( resource )
        1* resource.get() >> service
        1* nc.canConnect( serviceName, port ) >> true

        when:
        KubeConnection connection = instance.getConnection( serviceName, port )

        then:
        connection.getType(  ) == KubeConnectionType.SERVICE
        connection.getHostName(  ) == service.getMetadata(  ).getName(  )
        connection.getPort(  ) == port
        0* runc.isInsideContainer(  )

        cleanup:
        connection.close(  )
    }

    def "Get connection service is available, but not accessible run outside container attempts external connection."()
    {
        given:
        String ip = "1.0.0.1"
        int externalPort = WildflyResources.EXTERNAL_WILDFLY_SERVICE.getSpec(  ).getPorts(  ).get( 0 ).getPort(  )

        serviceName = WildflyResources.EXTERNAL_WILDFLY_SERVICE_NAME
        Service service = WildflyResources.EXTERNAL_WILDFLY_SERVICE.edit(  )
                .editStatus(  )
                    .editOrNewLoadBalancer(  )
                        .addNewIngress(  )
                            .withIp( ip  )
                        .endIngress(  )
                    .endLoadBalancer(  )
                .endStatus(  )
        .build(  )

        ServiceResource<Service> resource = Mock()
        1* rc.getServiceResource( serviceName ) >> Optional.of( resource )
        1* resource.get() >> service
        1* nc.canConnect( serviceName, port ) >> false
        1* runc.isInsideContainer(  ) >> false
        1* nc.canConnect( ip,  externalPort ) >> true

        when:
        KubeConnection connection = instance.getConnection( serviceName, port )

        then:
        connection.getType(  ) == KubeConnectionType.SERVICE
        connection.getHostName(  ) == ip
        connection.getPort(  ) == externalPort
    }

    def "Get connection service is available, not accessible, run outside container not matching target port or not externally accessible, return port-forward."()
    {
        given:
        String ip = "1.0.0.1"
        int externalPort = WildflyResources.EXTERNAL_WILDFLY_SERVICE.getSpec(  ).getPorts(  ).get( 0 ).getPort(  )

        serviceName = WildflyResources.EXTERNAL_WILDFLY_SERVICE_NAME
        Service service = WildflyResources.EXTERNAL_WILDFLY_SERVICE.edit(  )
                .editStatus(  )
                .editOrNewLoadBalancer(  )
                .addNewIngress(  )
                .withIp( ip  )
                .endIngress(  )
                .endLoadBalancer(  )
                .endStatus(  )
                .build(  )

        ServiceResource<Service> resource = Mock()
        1* rc.getServiceResource( serviceName ) >> Optional.of( resource )
        1* resource.get() >> service
        1* nc.canConnect( serviceName, targetPort ) >> false
        1* runc.isInsideContainer(  ) >> false
        externalCheck* nc.canConnect( ip, externalPort )


        LocalPortForward lpf = Mock()
        1* resource.portForward( targetPort, InetAddress.getLoopbackAddress(  ), 0 ) >> lpf

        when:
        KubeConnection connection = instance.getConnection( serviceName, targetPort )
        connection.close()

        then:
        connection.getType(  ) == KubeConnectionType.PORT_FORWARDED
        1* lpf.close(  )

        where:
        targetPort || externalCheck || canConnectExternal
        port       || 1             || false
        port + 1   || 0             || true
        port + 1   || 0             || false

    }

    def "Get connection service is available not externally, run outside container, return port-forward."()
    {
        given:
        serviceName = WildflyResources.EXTERNAL_WILDFLY_SERVICE_NAME
        Service service = WildflyResources.EXTERNAL_WILDFLY_SERVICE.edit(  ).editOrNewSpec(  )
                .withType( type )
            .endSpec(  )
        .build(  )


        ServiceResource<Service> resource = Mock()
        1* rc.getServiceResource( serviceName ) >> Optional.of( resource )
        1* resource.get() >> service
        1* nc.canConnect( serviceName, port ) >> false
        1* runc.isInsideContainer(  ) >> false
        LocalPortForward lpf = Mock()
        1* resource.portForward( port, InetAddress.getLoopbackAddress(  ), 0 ) >> lpf

        when:
        KubeConnection connection = instance.getConnection( serviceName, port )
        connection.close()

        then:
        connection.getType(  ) == KubeConnectionType.PORT_FORWARDED
        1* lpf.close(  )

        cleanup:
        connection.close()

        where:
        type << [ null, "", "ClusterIP"]
    }

    def "Get connection service is available but not accessible, run inside container, throws ConnectionException."()
    {
        given:
        Service service = WildflyResources.SIMPLE_WILDFLY_SERVICE

        ServiceResource<Service> resource = Mock()
        1* rc.getServiceResource( serviceName ) >> Optional.of( resource )
        1* resource.get() >> service
        1* nc.canConnect( serviceName, port ) >> false
        1* runc.isInsideContainer(  ) >> true

        when:
        instance.getConnection( serviceName, port )

        then:
        thrown ConnectionException
    }

    def "Get connection service not found, throws ConnectionException."()
    {
        given:
        1* rc.getServiceResource( serviceName ) >> Optional.empty(  )

        when:
        instance.getConnection( serviceName, port )

        then:
        thrown ConnectionException
    }

    def "Get port-forward service."()
    {
        given:
        ServiceResource<Service> resource = Mock()
        1* rc.getServiceResource( serviceName ) >> Optional.of( resource )
        LocalPortForward lpf = Mock()
        1* resource.portForward( port, InetAddress.getLoopbackAddress(  ), 0 ) >> lpf

        when:
        KubeConnection connection = instance.getPortForwardConnection( serviceName, port )
        connection.close()

        then:
        connection.getType(  ) == KubeConnectionType.PORT_FORWARDED
        1* lpf.close(  )

        cleanup:
        connection.close(  )

    }

    def "Get port-forward pod."()
    {
        given:
        String name = WildflyResources.SIMPLE_WILDFLY_POD_NAME

        PodResource resource = Mock()
        1* rc.getServiceResource( name ) >> Optional.empty(  )
        1* rc.getPodResource( name ) >> Optional.of( resource )
        LocalPortForward lpf = Mock()
        1* resource.portForward( port, InetAddress.getLoopbackAddress(  ), 0 ) >> lpf

        when:
        KubeConnection connection = instance.getPortForwardConnection( name, port )
        connection.close()

        then:
        connection.getType(  ) == KubeConnectionType.PORT_FORWARDED
        1* lpf.close(  )

        cleanup:
        connection.close(  )

    }

    def "Get port-forward not available, throws ConnectionException"()
    {
        given:
        String name = "blah"

        1* rc.getServiceResource( name ) >> Optional.empty(  )
        1* rc.getPodResource( name ) >> Optional.empty(  )

        when:
        instance.getPortForwardConnection( name, port )

        then:
        thrown ConnectionException
    }



}

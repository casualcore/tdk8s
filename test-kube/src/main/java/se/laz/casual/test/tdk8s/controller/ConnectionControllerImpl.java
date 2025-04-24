/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.dsl.PortForwardable;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import se.laz.casual.test.tdk8s.connection.ConnectionException;
import se.laz.casual.test.tdk8s.connection.KubeConnection;
import se.laz.casual.test.tdk8s.connection.PortForwardedConnection;
import se.laz.casual.test.tdk8s.connection.ServiceConnection;

import java.net.InetAddress;
import java.util.logging.Logger;

/**
 * Controller responsible for handling connection requests to resources in the TestKube.
 */
public class ConnectionControllerImpl implements ConnectionController
{
    Logger log = Logger.getLogger( ConnectionControllerImpl.class.getName());

    private final ResourceLookupController lookupController;
    private final NetworkController networkController;
    private final RuntimeController runtimeController;

    public ConnectionControllerImpl( ResourceLookupController lookupController, NetworkController networkController, RuntimeController runtimeController )
    {
        this.lookupController = lookupController;
        this.networkController = networkController;
        this.runtimeController = runtimeController;
    }

    @Override
    public KubeConnection getConnection( String service, int targetPort )
    {
        ServiceResource<Service> sr = lookupController.getServiceResource( service )
                .orElseThrow( ()-> new ConnectionException( "Resource unavailable: " + service ) );

        Service s = sr.get();

        // Check if client can connect to the service.
        if( networkController.canConnect( s.getMetadata().getName(), targetPort ) )
        {
            return new ServiceConnection( s.getMetadata().getName(), targetPort );
        }

        // Fix for running from outside a container / cluster.
        if( !runtimeController.isInsideContainer() )
        {
            log.info( ()->"Running outside of a container, attempting to connect externally." );
            // Check if the service should be externally accessible.
            if( s.getSpec().getType() != null && s.getSpec().getType().equals( "LoadBalancer" ) &&
                    !s.getStatus().getLoadBalancer().getIngress().isEmpty() )
            {
                // Attempt to access service externally.
                String externalIp = s.getStatus().getLoadBalancer().getIngress().get( 0 ).getIp();
                int externalPort = s.getSpec().getPorts().stream()
                        .filter( p -> p.getTargetPort().getIntVal() == targetPort )
                        .findFirst()
                        .map( ServicePort::getPort )
                        .orElse( -1 );
                log.info( ()-> "External IP: " + externalIp + ". External Port: " + externalPort );
                if( externalPort != -1 && networkController.canConnect( externalIp, externalPort ) )
                {
                    log.info( ()->"Connection available externally." );
                    return new ServiceConnection( externalIp, externalPort );
                }
            }

            log.warning( ()-> "Creating a port forward connection for service: "+ service + ", to allow seamless connectivity during development. " +
                    "Load Balancing will not work. Do NOT use for performance testing." );
            return createPortForwardConnection( sr, targetPort );
        }

        throw new ConnectionException( "Unable to connect to service: " + service );
    }


    @Override
    public KubeConnection getPortForwardConnection( String resource, int targetPort )
    {
        return lookupController.getServiceResource( resource )
                .map( serviceServiceResource -> createPortForwardConnection( serviceServiceResource, targetPort ) )
                .orElseGet( () -> lookupController.getPodResource( resource )
                .map( pr -> createPortForwardConnection( pr, targetPort ) )
                .orElseThrow( () -> new ConnectionException( "Resource unavailable: " + resource ) ) );
    }

    private KubeConnection createPortForwardConnection( PortForwardable resource, int port )
    {
        LocalPortForward portForward = createPortForward( resource, port );

        return new PortForwardedConnection( portForward );
    }

    private LocalPortForward createPortForward( PortForwardable resource, int port )
    {
        InetAddress local = InetAddress.getLoopbackAddress();
        return resource.portForward( port, local, 0 );
    }
}

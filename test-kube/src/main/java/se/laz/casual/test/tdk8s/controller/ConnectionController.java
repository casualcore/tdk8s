/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import se.laz.casual.test.tdk8s.connection.KubeConnection;

public interface ConnectionController extends Connectable
{
    /**
     * Creates a `port-forward` to the resource named on the targetPort provided.
     * <br/>
     * The name can be either the alias for the managed resource or
     * the actual underlying name of the resource inside the cluster.
     * <br/>
     * The resource can be a pod or a service.
     * </br>
     * The `port-forward` will be assigned to an available local port at random.
     * <br/>
     * The returned {@link KubeConnection} provides the hostname and port
     * details that should be used for connections.
     * <br/>
     * The {@link KubeConnection} returned is {@link AutoCloseable} and should be used
     * within `try-with-resources` blocks to ensure they are closed correctly.
     *
     * @param resource to which to connect.
     * @param targetPort with which to connect.
     * @return the connection.
     */
    KubeConnection getPortForwardConnection( String resource, int targetPort );
}

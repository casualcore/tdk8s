/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.k8s.controller;

import se.laz.casual.test.k8s.connection.KubeConnection;

public interface Connectable
{
    /**
     * Gets a connection to the service requested.
     * </br>
     * The name can be either the alias for the managed resource or
     * the actual underlying name of the resource inside the cluster.
     * <br/>
     * Attempts to connect to the service on the port provided to confirm availability.
     * <br/>
     * The returned {@link KubeConnection} provides the hostname and port
     * details that should be used for connections.
     * <br/>
     * Note: when run locally / outside a container, if the service is not accessible
     * a seamless `port-forward` connection will be created if possible. This is done to
     * simplify writing and maintaining test code.
     * <br/>
     * The {@link KubeConnection} returned is {@link AutoCloseable} and should be used
     * within `try-with-resources` blocks to ensure they are closed correctly.
     *
     * @param service to which to connect.
     * @param targetPort with which to connect.
     * @return the connection.
     */
    KubeConnection getConnection( String service, int targetPort );
}

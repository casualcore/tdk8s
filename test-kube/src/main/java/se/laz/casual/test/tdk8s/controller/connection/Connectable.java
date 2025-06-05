/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.connection;

import se.laz.casual.test.tdk8s.connection.ConnectionException;
import se.laz.casual.test.tdk8s.connection.KubeConnection;

public interface Connectable
{
    /**
     * Gets a connection to the Service requested.
     * <p>
     *     The name can be either the alias for the managed Service or
     *     the actual underlying name of the Service inside the cluster.
     * </p>
     * <p>
     *     Attempts to connect to the Service on the port provided to confirm availability.
     * </p>
     * <p>
     *     The returned {@link KubeConnection} provides the hostname and port
     *     details that should be used for connections.
     * </p>
     * <p>
     *     Note: when run locally / outside a container, if the service is not accessible
     *     a seamless `port-forward` connection will be created if possible. This is done to
     *     simplify writing and maintaining test code.
     * </p>
     * <p>
     *     The {@link KubeConnection} returned is {@link AutoCloseable} and should be used
     *     within `try-with-resources` blocks to ensure they are closed correctly.
     * </p>
     *
     * @param service to which to connect.
     * @param targetPort with which to connect.
     * @return the connection.
     * @throws ConnectionException if unable to connect.
     */
    KubeConnection getConnection( String service, int targetPort );
}

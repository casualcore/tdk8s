/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.connection;

import se.laz.casual.test.tdk8s.connection.ConnectionException;
import se.laz.casual.test.tdk8s.connection.KubeConnection;

public interface ConnectionController extends Connectable
{
    /**
     * Creates a `port-forward` to the resource on the targetPort provided.
     * <p>
     * The resource name can be either the alias for the managed resource
     * or the actual underlying name of the resource inside the cluster.
     * </p>
     * <p>
     * Note: If the resource has multiple pods associated the first is used.
     * </p>
     * <p>
     * The `port-forward` will be assigned to an available local port at random.
     * </p>
     * <p>
     * The returned {@link KubeConnection} provides the host name and port
     * details that should be used for connections.
     * </p>
     * <p>
     * The {@link KubeConnection} returned is {@link AutoCloseable} and should be used
     * within `try-with-resources` blocks to ensure they are closed correctly.
     * </p>
     *
     * @param resource   to which to connect.
     * @param targetPort with which to connect.
     * @return the connection.
     * @throws ConnectionException if unable to connect.
     */
    KubeConnection getPortForwardConnection( String resource, int targetPort );
}

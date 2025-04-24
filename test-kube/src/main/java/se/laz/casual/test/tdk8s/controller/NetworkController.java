/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

public interface NetworkController
{
    /**
     * Checks if it is possible to connect to the given
     * host and port.
     *
     * @param host to which to connect.
     * @param port on which to connect.
     * @return if connection was possible.
     */
    boolean canConnect( String host, int port );
}

/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import se.laz.casual.test.tdk8s.connection.NetworkChecker;

public class NetworkControllerImpl implements NetworkController
{
    @Override
    public boolean canConnect( String host, int port )
    {
        return NetworkChecker.canConnect( host, port );
    }
}

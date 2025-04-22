/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.k8s.controller;

import se.laz.casual.test.k8s.connection.NetworkChecker;

public class NetworkController
{
    public boolean canConnect( String host, int port )
    {
        return NetworkChecker.canConnect( host, port );
    }
}

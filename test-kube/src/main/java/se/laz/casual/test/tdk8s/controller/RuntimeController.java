/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

public interface RuntimeController
{
    /**
     * Determine if the process is currently running within
     * a container.
     *
     * @return is inside a container.
     */
    boolean isInsideContainer();
}

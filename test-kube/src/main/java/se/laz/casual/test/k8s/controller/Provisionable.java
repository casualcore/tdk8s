/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.k8s.controller;

public interface Provisionable
{
    /**
     * Initialises the managed resources and wait until they are ready.
     */
    void init();

    /**
     * Deletes the managed resources and wait until they are deleted.
     */
    void destroy();
}

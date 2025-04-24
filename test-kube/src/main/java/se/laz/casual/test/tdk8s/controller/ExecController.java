/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import se.laz.casual.test.tdk8s.exec.ExecResult;

import java.util.concurrent.CompletableFuture;

public interface ExecController
{
    /**
     * Executes the provided command on the named pod and wait until completed.
     * </br>
     * The name can be either the alias for the managed resource or
     * the actual underlying name of the resource inside the cluster.
     *
     * @param pod on which to run the command.
     * @param command the command to run.
     * @return the result of running the command.
     */
    ExecResult executeCommand( String pod, String... command );

    /**
     * Executes the provided command on the named pod without waiting for the result.
     * </br>
     * The name can be either the alias for the managed resource or
     * the actual underlying name of the resource inside the cluster.
     *
     * @param pod on which to run the command.
     * @param command the command to run.
     * @return the result of the running command as a future.
     */
    CompletableFuture<ExecResult> executeCommandAsync( String pod, String... command );
}

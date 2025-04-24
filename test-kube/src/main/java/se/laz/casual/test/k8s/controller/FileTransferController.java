/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.k8s.controller;

import java.nio.file.Path;

public interface FileTransferController
{
    /**
     * Downloads from the pod named the source specified in the pod, to
     * the destination specified on the local filesystem.
     * </br>
     * The name can be either the alias for the managed resource or
     * the actual underlying name of the resource inside the cluster.
     *
     * @param pod from which to download.
     * @param source file on the pod to download.
     * @param destination file on the local filesystem to save the download.
     * @return if the operation was successful.
     */
    boolean download( String pod, String source, Path destination );

    /**
     * Uploads to the pod named the source specific on the local filesystem, to
     * the destination specified in the pod.
     * </br>
     * The name can be either the alias for the managed resource or
     * the actual underlying name of the resource inside the cluster.
     *
     * @param pod to which to upload.
     * @param source file on the local filesystem to upload.
     * @param destination file on the pod to save the upload.
     * @return if the operation was successful.
     */
    boolean upload( String pod, String source, Path destination );
}

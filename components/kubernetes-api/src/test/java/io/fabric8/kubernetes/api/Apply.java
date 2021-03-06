/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.ControllerDesiredState;
import io.fabric8.kubernetes.api.model.DesiredState;
import io.fabric8.kubernetes.api.model.Manifest;
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.PodCurrentContainerInfo;
import io.fabric8.kubernetes.api.model.PodListSchema;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerListSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.api.model.ServiceListSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies the given JSON file to the kubernetes environment
 */
public class Apply {
    public static void main(String... args) {
        if (args.length < 1) {
            System.out.println("Usage jsonFileToApply");
            return;
        }
        try {
            KubernetesClient kube = new KubernetesClient();
            System.out.println("Connecting to kubernetes on: " + kube.getAddress());

            File file = new File(args[0]);
            System.out.println("Applying file: " + file);
            if (!file.exists() || !file.isFile()) {
                System.out.println("File does not exist! " + file.getAbsolutePath());
                return;
            }
            Controller controller = new Controller(kube);
            Object answer = controller.applyJson(file);
            System.out.println("Applied!: " + answer);
        } catch (Exception e) {
            System.out.println("FAILED: " + e);
            e.printStackTrace();
        }
    }
}

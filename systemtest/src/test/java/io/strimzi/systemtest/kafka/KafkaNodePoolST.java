/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.kafka;

import io.strimzi.systemtest.AbstractST;
import io.strimzi.systemtest.annotations.IsolatedTest;
import io.strimzi.systemtest.annotations.ParallelSuite;
import io.strimzi.systemtest.resources.ResourceManager;
import io.strimzi.systemtest.storage.TestStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import static io.strimzi.systemtest.Constants.REGRESSION;
import org.junit.jupiter.api.extension.ExtensionContext;


@Tag(REGRESSION)
//@IsolatedSuite
@ParallelSuite
public class KafkaNodePoolST extends AbstractST {

    private static final Logger LOGGER = LogManager.getLogger(KafkaNodePoolST.class);

    @IsolatedTest
    void testSmoke() {
        System.out.println("");
    }

    @BeforeAll
    void setup(ExtensionContext extensionContext) {
        // assumeTrue(Environment.isKafkaNodePoolsModeEnabled());
        clusterOperator.unInstall();

        TestStorage testStorage = new TestStorage(extensionContext);

        String clusterOperatorNamespace = testSuiteNamespaceManager.getMapOfAdditionalNamespaces().get(KafkaNodePoolST.class.getSimpleName()).stream().findFirst().get();
        String clusterOperatorName =  ResourceManager.getCoDeploymentName();

        // deploy CO watching multiple or single namespace
        LOGGER.info("Creating CO {} in {} namespace", clusterOperatorName, clusterOperatorNamespace);

//        clusterOperator = new SetupClusterOperator.SetupClusterOperatorBuilder()
//            .withExtensionContext(extensionContext)
//            .withNamespace(coNamespace)
//            .withClusterOperatorName(coName)
//            .withWatchingNamespaces(namespace)
//            .withExtraLabels(Collections.singletonMap("app.kubernetes.io/operator", coName))
//            .withExtraEnvVars(extraEnvs)
//            .createInstallation()
//            .runBundleInstallation();

//        cluster.createNamespaces(CollectorElement.createCollectorElement(this.getClass().getName()), clusterOperator.getDeploymentNamespace(), Arrays.asList(PRIMARY_KAFKA_WATCHED_NAMESPACE, MAIN_TEST_NAMESPACE));

//        clusterOperator = new SetupClusterOperator.SetupClusterOperatorBuilder()
//            .withExtensionContext(BeforeAllOnce.getSharedExtensionContext())
//            .withNamespace(INFRA_NAMESPACE)
//            .withWatchingNamespaces(String.join(",", INFRA_NAMESPACE, PRIMARY_KAFKA_WATCHED_NAMESPACE, MAIN_TEST_NAMESPACE))
//            .withBindingsNamespaces(Arrays.asList(INFRA_NAMESPACE, PRIMARY_KAFKA_WATCHED_NAMESPACE, MAIN_TEST_NAMESPACE))
//            .createInstallation()
//            .runInstallation();

//        clusterOperator.unInstall();

//        cluster.createNamespaces(CollectorElement.createCollectorElement(this.getClass().getName()), clusterOperator.getDeploymentNamespace(), Arrays.asList(PRIMARY_KAFKA_WATCHED_NAMESPACE, MAIN_TEST_NAMESPACE));
//        cluster.createNamespaces(CollectorElement.createCollectorElement(this.getClass().getName()), clusterOperator.getDeploymentNamespace(), Arrays.asList(PRIMARY_KAFKA_WATCHED_NAMESPACE, MAIN_TEST_NAMESPACE));

//        clusterOperator = clusterOperator.defaultInstallation()
//            .withWatchingNamespaces(Constants.WATCH_ALL_NAMESPACES)
//            .createInstallation()
//            .runInstallation();

//        LOGGER.info("Creating Cluster Operator which will watch over all namespaces");
//
//        clusterOperator.unInstall();
//
//        // cluster.createNamespaces(CollectorElement.createCollectorElement(this.getClass().getName()), clusterOperator.getDeploymentNamespace(), Arrays.asList(PRIMARY_KAFKA_WATCHED_NAMESPACE, MAIN_TEST_NAMESPACE));
//
//
//        clusterOperator = clusterOperator.defaultInstallation()
//            .withWatchingNamespaces(Constants.WATCH_ALL_NAMESPACES)
//            .withExtraEnvVars(Environment.isKRaftModeEnabled())
//            .createInstallation()
//            .runInstallation();
    }

}

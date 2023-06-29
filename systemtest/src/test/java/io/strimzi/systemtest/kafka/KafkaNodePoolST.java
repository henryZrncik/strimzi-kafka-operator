/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.kafka;


import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.strimzi.api.kafka.model.KafkaResources;
import io.strimzi.operator.common.Annotations;
import io.strimzi.operator.common.model.Labels;
import io.strimzi.systemtest.AbstractST;
import io.strimzi.systemtest.Constants;
import io.strimzi.systemtest.Environment;
import io.strimzi.systemtest.annotations.ParallelNamespaceTest;
import io.strimzi.systemtest.kafkaclients.internalClients.KafkaClients;
import io.strimzi.systemtest.kafkaclients.internalClients.KafkaClientsBuilder;
import io.strimzi.systemtest.resources.ResourceManager;
import io.strimzi.systemtest.resources.crd.KafkaNodePoolResource;
import io.strimzi.systemtest.resources.crd.KafkaResource;
import io.strimzi.systemtest.resources.operator.SetupClusterOperator;
import io.strimzi.systemtest.storage.TestStorage;
import io.strimzi.systemtest.templates.crd.KafkaTemplates;
import io.strimzi.systemtest.utils.ClientUtils;
import io.strimzi.systemtest.utils.kubeUtils.objects.PodUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

import static io.strimzi.systemtest.Constants.INFRA_NAMESPACE;
import static io.strimzi.systemtest.Constants.REGRESSION;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.extension.ExtensionContext;
import java.util.Map;

@Tag(REGRESSION)
public class KafkaNodePoolST extends AbstractST {

    private static final Logger LOGGER = LogManager.getLogger(KafkaNodePoolST.class);


    /**
     * @description This test case verifies transfer of Kafka Cluster from and to management by KafkaNodePool, by creating corresponding Kafka and KafkaNodePool custom resources
     * and manipulating according kafka annotation.
     *
     * @steps
     *  1. - Deploy Kafka with annotated to enable management by KafkaNodePool, and KafkaNodePool targeting given Kafka Cluster.
     *     - Kafka is deployed, KafkaNodePool custom resource is targeting Kafka Cluster as expected.
     *  2. - Modify KafkaNodePool by increasing number of Kafka Replicas.
     *     - Number of Kafka Pods is increased to match specification from KafkaNodePool
     *  3. - Produce and consume messages in given Kafka Cluster.
     *     - Clients can produce and consume messages.
     *  4. - Modify Kafka custom resource annotation strimzi.io/node-pool to disable management by KafkaNodePool.
     *     - new StatefulSet is created, replacing former one, Pods are replaced and specification from KafkaNodePool (i.e., changed replica count) are ignored.
     *  5. - Produce and consume messages in given Kafka Cluster.
     *     - Clients can produce and consume messages.
     *  6. - Modify Kafka custom resource annotation strimzi.io/node-pool to enable management by KafkaNodePool.
     *     - new StatefulSet is created, replacing former one, Pods are replaced and specification from KafkaNodePool (i.e., changed replica count) has priority over Kafka specification.
     *  7. - Produce and consume messages in given Kafka Cluster.
     *     - Clients can produce and consume messages.
     * @usecase
     *  - kafka-node-pool
     */
    @ParallelNamespaceTest
    void testKafkaManagementTransferToAndFromKafkaNodePool(ExtensionContext extensionContext) {

        final TestStorage testStorage = new TestStorage(extensionContext);

        // setup clients
        KafkaClients clients = new KafkaClientsBuilder()
            .withProducerName(testStorage.getProducerName())
            .withConsumerName(testStorage.getConsumerName())
            .withBootstrapAddress(KafkaResources.plainBootstrapAddress(testStorage.getClusterName()))
            .withTopicName(testStorage.getTopicName())
            .withMessageCount(testStorage.getMessageCount())
            .withDelayMs(200)
            .withNamespaceName(clusterOperator.getDeploymentNamespace())
            .build();

        LOGGER.info("Deploying Kafka Cluster: {}/{} controlled by KafkaNodePool: {}", testStorage.getNamespaceName(), testStorage.getClusterName(), testStorage.getKafkaNodePoolName());
        resourceManager.createResource(extensionContext, KafkaTemplates.kafkaPersistent(testStorage.getClusterName(), 3, 1).build());
        LOGGER.info("Kafka Cluster: {}/{} is ready", testStorage.getNamespaceName(), testStorage.getClusterName());

        // increase number of kafka replicas in KafkaNodePool
        LOGGER.info("Modifying KafkaNodePool: {}/{} by increasing number of Kafka replicas from '3' to '4'", testStorage.getNamespaceName(), testStorage.getKafkaNodePoolName());
        KafkaNodePoolResource.replaceKafkaNodePoolResourceInSpecificNamespace(testStorage.getKafkaNodePoolName(), kafkaNodePool -> {
            kafkaNodePool.getSpec().setReplicas(4);
        }, testStorage.getNamespaceName());

        waitForPodSetKafkaPodsReady(testStorage.getNamespaceName(), 4, testStorage.getKafkaStatefulSetName());

        LOGGER.info("Producing and Consuming messages with clients: {}, {} in Namespace {}", testStorage.getProducerName(), testStorage.getConsumerName(), testStorage.getNamespaceName());
        resourceManager.createResource(extensionContext,
            clients.producerStrimzi(),
            clients.consumerStrimzi()
        );
        ClientUtils.waitForClientsSuccess(testStorage);

        LOGGER.info("Transferring Kafka Cluster: {}/{} from KafkaNodePool enabled, to being disabled by removing annotation", testStorage.getNamespaceName(), testStorage.getClusterName());
        KafkaResource.replaceKafkaResourceInSpecificNamespace(testStorage.getClusterName(),
            kafka -> {
                kafka.getMetadata().getAnnotations().clear();
            }, testStorage.getNamespaceName());

        waitForPodSetKafkaPodsReady(testStorage.getNamespaceName(), 3, KafkaResources.kafkaStatefulSetName(testStorage.getClusterName()));

        LOGGER.info("Producing and Consuming messages with clients: {}, {} in Namespace {}", testStorage.getProducerName(), testStorage.getConsumerName(), testStorage.getNamespaceName());
        resourceManager.createResource(extensionContext,
            clients.producerStrimzi(),
            clients.consumerStrimzi()
        );
        ClientUtils.waitForClientsSuccess(testStorage);

        LOGGER.info("Transferring Kafka Cluster: {}/{} from KafkaNodePool enabled, to being disabled by removing annotation", testStorage.getNamespaceName(), testStorage.getClusterName());
        KafkaResource.replaceKafkaResourceInSpecificNamespace(testStorage.getClusterName(),
            kafka -> {
                kafka.getMetadata().getAnnotations().put(Annotations.ANNO_STRIMZI_IO_NODE_POOLS, "enabled");
            }, testStorage.getNamespaceName());

        waitForPodSetKafkaPodsReady(testStorage.getNamespaceName(), 4, testStorage.getKafkaStatefulSetName());

        LOGGER.info("Producing and Consuming messages with clients: {}, {} in Namespace {}", testStorage.getProducerName(), testStorage.getConsumerName(), testStorage.getNamespaceName());
        resourceManager.createResource(extensionContext,
            clients.producerStrimzi(),
            clients.consumerStrimzi()
        );
        ClientUtils.waitForClientsSuccess(testStorage);

    }

    private static void waitForPodSetKafkaPodsReady(String namespace, int expectedPodsCount, String controllingPodSetName) {
        LOGGER.info("Match all Pods with label {}={}, monitored by given StrimziPodSet in Namespace: {}", Labels.STRIMZI_CONTROLLER_NAME_LABEL, controllingPodSetName, namespace);
        Map<String, String> matchLabels = Map.of(Labels.STRIMZI_CONTROLLER_NAME_LABEL, controllingPodSetName);
        LabelSelector ls =  new LabelSelectorBuilder().withMatchLabels(matchLabels).build();

        LOGGER.info("Waiting for {} Pod(s) of StrimziPodSet {}/{} to be ready", expectedPodsCount, namespace, controllingPodSetName);
        PodUtils.waitForPodsReady(namespace, ls, expectedPodsCount, true);
    }

    @BeforeAll
    void setup(ExtensionContext extensionContext) {
        assumeFalse(Environment.isOlmInstall());
        assumeFalse(Environment.isHelmInstall());

        LOGGER.info("test assumes that KafkaNodePool feature gate is enabled, as this test is specific to this scenario");
        assumeTrue(Environment.isKafkaNodePoolEnabled());

        String clusterOperatorName =  ResourceManager.getCoDeploymentName();

        clusterOperator.unInstall();
        clusterOperator = new SetupClusterOperator.SetupClusterOperatorBuilder()
            .withExtensionContext(extensionContext)
            .withNamespace(INFRA_NAMESPACE)
            .withWatchingNamespaces(Constants.WATCH_ALL_NAMESPACES)
            .withClusterOperatorName(clusterOperatorName)
            .createInstallation()
            .runInstallation();
    }

}

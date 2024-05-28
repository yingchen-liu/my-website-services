package com.yingchenliu.services.skilltree.services

import com.yingchenliu.services.skilltree.domains.*
import com.yingchenliu.services.skilltree.repositories.NodeOrderRepository
import com.yingchenliu.services.skilltree.repositories.NodeRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class NodeService(
    private val nodeRepository: NodeRepository,
    private val nodeOrderRepository: NodeOrderRepository,
    @Qualifier("transactionManager") private val transactionManager: PlatformTransactionManager
) {
    fun findFromNode(uuid: UUID): TreeNode {
        val orders = nodeOrderRepository.findTreeNodeNonDeletedChildrenOrder(uuid)
        println("orders")
        println(orders)
        val node = nodeRepository.findTreeNodeAndNonDeletedChildren(uuid)
        return sortChildrenAtCurrentLevel(node, orders)
    }

    private fun sortChildrenAtCurrentLevel(node: TreeNode, orders: List<NodeOrder>): TreeNode {
        return node.children?.let { children ->
            val nodesByUuid = children.associateBy { it.uuid }
            val orderForThisLevel =
                orders.filter { order -> nodesByUuid.contains(order.previousNodeUuid) || nodesByUuid.contains((order.nextNodeUuid)) }
                    .toList()

            val relationships =
                orderForThisLevel.associate { it.previousNodeUuid.toString() to listOf(it.nextNodeUuid.toString()) }
            println(TopologicalSort(relationships).topologicalSort())
            val sortedOrderedNodes = TopologicalSort(relationships).topologicalSort()
                .mapNotNull { uuid -> nodesByUuid[UUID.fromString(uuid)] }.toList()

            println("sortedOrderedNodes")
            println(sortedOrderedNodes)

            val unorderedNodes = children.filter { node -> !sortedOrderedNodes.contains(node) }.toList()

            println("unorderedNodes")
            println(unorderedNodes)

            return node.copy(children = (sortedOrderedNodes + unorderedNodes).map { node -> sortChildrenAtCurrentLevel(node, orders) })
        } ?: node
    }

    fun findFromRoot(): TreeNode {
        return findFromNode(UUID.fromString("b1747c9f-3818-4edd-b7c6-7384b2cb5e41"))
    }

    fun findById(uuid: UUID): Optional<TreeNode> {
        return nodeRepository.findById(uuid)
    }

    @Transactional("transactionManager")
    fun create(node: TreeNode, parentUUID: UUID): TreeNode {
        val newNode = node.copy(createdAt = LocalDateTime.now(), lastUpdatedAt = LocalDateTime.now())
        val createdNode = nodeRepository.save(newNode)
        nodeRepository.createParentRelationship(parentUUID, newNode.uuid)
        nodeRepository.findLastChild(parentUUID)?.let {
            nodeRepository.createAfterRelationship(createdNode.uuid, parentUUID)
        }
        return createdNode;
    }

    fun update(node: TreeNode): TreeNode {
        val newNode = node.copy(lastUpdatedAt = LocalDateTime.now())
        return nodeRepository.save(newNode)
    }

    @Transactional("transactionManager")
    fun changeNodePosition(uuid: UUID, nodePositionDTO: NodePositionDTO) {
        nodeRepository.removeBeforeRelationship(uuid)
        nodeRepository.removeParentRelationship(uuid)
        nodeRepository.createParentRelationship(UUID.fromString(nodePositionDTO.parentUUID), uuid)
        nodePositionDTO.order?.let {
            val orderRelatedToUuid = UUID.fromString(it.relatedToUUID)
            when (it.position) {
                NodeOrderPosition.BEFORE -> nodeRepository.createBeforeRelationship(uuid, orderRelatedToUuid)
                NodeOrderPosition.AFTER -> nodeRepository.createAfterRelationship(uuid, orderRelatedToUuid)
            }
        } ?: {
            nodeRepository.findLastChild(UUID.fromString(nodePositionDTO.parentUUID))?.let {
                nodeRepository.createAfterRelationship(uuid, it.uuid)
            }
        }
    }

    fun delete(node: TreeNode): TreeNode {
        val newNode = node.copy(isDeleted = true)
        return update(newNode)
    }

    fun refresh() {
        val nodes = nodeRepository.findAll()
        nodeRepository.saveAll(nodes)
    }
}
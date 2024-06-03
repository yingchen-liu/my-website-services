package com.yingchenliu.services.services

import com.yingchenliu.services.domains.NodeOrder
import com.yingchenliu.services.domains.NodeOrderPosition
import com.yingchenliu.services.domains.NodePositionDTO
import com.yingchenliu.services.domains.TreeNode
import com.yingchenliu.services.skilltree.domains.*
import com.yingchenliu.services.repositories.NodeOrderRepository
import com.yingchenliu.services.repositories.NodeRepository
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
            val sortedOrderedNodes = TopologicalSort(relationships).topologicalSort()
                .mapNotNull { uuid -> nodesByUuid[UUID.fromString(uuid)] }.toList()

            val unorderedNodes = children.filter { node -> !sortedOrderedNodes.contains(node) }.toList()

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
    fun createChild(node: TreeNode, parentUUID: UUID): TreeNode {
        val lastChild = nodeRepository.findLastChild(parentUUID)
        val newNode = node.copy(createdAt = LocalDateTime.now(), lastUpdatedAt = LocalDateTime.now())
        val createdNode = nodeRepository.save(newNode)
        nodeRepository.createParentRelationship(parentUUID, newNode.uuid)
        lastChild?.let {
            nodeRepository.createAfterRelationship(createdNode.uuid, it.uuid)
        }
        return createdNode;
    }

    @Transactional("transactionManager")
    fun createAfter(node: TreeNode, previousNodeUUID: UUID): TreeNode {
        val newNode = node.copy(createdAt = LocalDateTime.now(), lastUpdatedAt = LocalDateTime.now())
        val createdNode = nodeRepository.save(newNode)
        val parentNode = nodeRepository.findParentNode(previousNodeUUID)
        nodeRepository.createParentRelationship(parentNode.uuid, newNode.uuid)
        nodeRepository.createAfterRelationship(createdNode.uuid, previousNodeUUID)
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
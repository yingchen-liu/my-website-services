package com.yingchenliu.services.skilltree.services

import com.yingchenliu.services.skilltree.domains.TreeNode
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
    @Qualifier("transactionManager") private val transactionManager: PlatformTransactionManager
) {
    fun findFromNode(uuid: UUID): TreeNode {
        return nodeRepository.findTreeNodeAndNonDeletedChildren(uuid)
    }

    fun findFromRoot(): TreeNode {
        return findFromNode(UUID.fromString("b1747c9f-3818-4edd-b7c6-7384b2cb5e41"))
    }

    fun findById(uuid: UUID): Optional<TreeNode> {
        return nodeRepository.findById(uuid)
    }

    @Transactional("transactionManager")
    fun create(node: TreeNode, parentUuid: UUID): TreeNode {
        val newNode = node.copy(createdAt = LocalDateTime.now(), lastUpdatedAt = LocalDateTime.now())
        val createdNode = nodeRepository.save(newNode)
        nodeRepository.createRelationship(parentUuid, newNode.uuid)
        return createdNode;
    }

    fun update(node: TreeNode): TreeNode {
        val newNode = node.copy(lastUpdatedAt = LocalDateTime.now())
        return nodeRepository.save(newNode)
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
package com.yingchenliu.services.skilltree.controllers

import com.yingchenliu.services.skilltree.domains.NodePositionDTO
import com.yingchenliu.services.skilltree.domains.TreeNode
import com.yingchenliu.services.skilltree.services.NodeService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*


@RestController
class SkillTreeController(val nodeService: NodeService) {

    @GetMapping("/nodes/root")
    fun findRoot(): TreeNode? {
        return nodeService.findFromRoot()
    }

    @GetMapping("/nodes/{uuid}")
    fun findNode(@PathVariable("uuid") uuid: String): TreeNode? {
        return nodeService.findFromNode(UUID.fromString(uuid))
    }

    @PostMapping("/nodes/{parentUUID}")
    fun createChildNode(@PathVariable("parentUUID") parentUUID: String, @RequestBody node: TreeNode): TreeNode {
        val parentNode = nodeService.findById(UUID.fromString(parentUUID))

        return if (parentNode.isPresent) {
            nodeService.createChild(node, parentNode.get().uuid)
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Error creating node: Parent node not found")
        }
    }

    @PostMapping("/nodes/{previousNodeUUID}/after")
    fun createNodeAfter(@PathVariable("previousNodeUUID") previousNodeUUID: String, @RequestBody node: TreeNode): TreeNode {
        val previousNode = nodeService.findById(UUID.fromString(previousNodeUUID))

        return if (previousNode.isPresent) {
            nodeService.createAfter(node, previousNode.get().uuid)
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Error creating node: Parent node not found")
        }
    }

    @PutMapping("/nodes/{uuid}")
    fun updateNode(@PathVariable("uuid") uuid: String, @RequestBody node: TreeNode): TreeNode {
        val existingNode = nodeService.findById(UUID.fromString(uuid))

        return if (existingNode.isPresent) {
            val newNode = node.copy(uuid = existingNode.get().uuid)
            nodeService.update(newNode)
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Error updating node: Node not found")
        }
    }

    @PutMapping("/nodes/{uuid}/position")
    fun updateNodeParent(@PathVariable("uuid") uuid: String, @RequestBody positionDTO: NodePositionDTO): TreeNode {
        val node = nodeService.findById(UUID.fromString(uuid))
        val parentNode = nodeService.findById(UUID.fromString(positionDTO.parentUUID))

        if (!node.isPresent || !parentNode.isPresent) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Error updating node's position: Node not found")
        }
        positionDTO.order?.let {
            val relatedToNode = nodeService.findById(UUID.fromString(it.relatedToUUID))
            if (!relatedToNode.isPresent) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Error updating node's position: Node not found")
            }
        }

        nodeService.changeNodePosition(node.get().uuid, positionDTO)
        return nodeService.findFromNode(parentNode.get().uuid)
    }

    @DeleteMapping("/nodes/{uuid}")
    fun deleteById(@PathVariable("uuid") uuid: String) {
        val node = nodeService.findById(UUID.fromString(uuid))
        node.ifPresentOrElse(
            { nodeService.delete(it) },
            { throw ResponseStatusException(HttpStatus.NOT_FOUND, "Error deleting node: Node not found") }
        );
    }

    @GetMapping("/refresh")
    fun refresh() {
        nodeService.refresh()
    }
}
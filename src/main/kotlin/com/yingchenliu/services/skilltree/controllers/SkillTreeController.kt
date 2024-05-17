package com.yingchenliu.services.skilltree.controllers

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

    @PostMapping("/nodes/{parentUuid}")
    fun createNode(@PathVariable("parentUuid") parentUuid: String, @RequestBody node: TreeNode): TreeNode {
        val parentNode = nodeService.findById(UUID.fromString(parentUuid))

        return if (parentNode.isPresent) {
            nodeService.create(node, UUID.fromString(parentUuid))
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Error creating node: Parent node not found")
        }
    }

    @PutMapping("/nodes/{uuid}")
    fun updateNodeById(@PathVariable("uuid") uuid: String, @RequestBody node: TreeNode): TreeNode {
        val existingNode = nodeService.findById(UUID.fromString(uuid))

        return if (existingNode.isPresent) {
            val newNode = node.copy(uuid = existingNode.get().uuid)
            nodeService.update(newNode)
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Error updating node: Node not found")
        }
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
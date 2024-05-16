package com.yingchenliu.services.skilltree

import com.yingchenliu.services.skilltree.domains.TreeNode
import com.yingchenliu.services.skilltree.repositories.TreeNodeRepository
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException


@RestController
class SkillTreeController(val nodeRepository: TreeNodeRepository) {

    @GetMapping("/nodes/root")
    fun findRootAndChildren(): TreeNode? {
        return nodeRepository.findTreeNodeAndNonDeletedChildren("b1747c9f-3818-4edd-b7c6-7384b2cb5e41")
    }

    @GetMapping("/nodes/{uuid}")
    fun findNodeAndChildren(@PathVariable("uuid") uuid: String): TreeNode? {
        return nodeRepository.findTreeNodeAndNonDeletedChildren(uuid)
    }

    @PutMapping("/nodes/{uuid}")
    fun updateById(@PathVariable("uuid") uuid: String, @RequestBody node: TreeNode): TreeNode {
        return nodeRepository.save(node)
    }

    @DeleteMapping("/nodes/{uuid}")
    fun deleteById(@PathVariable("uuid") uuid: String) {
        val node = nodeRepository.findById(uuid)
        node.ifPresentOrElse(
            { nodeRepository.save(it.copy(isDeleted = true)) },
            { throw ResponseStatusException(HttpStatus.NOT_FOUND, "Error deleting node: Node not found") }
        );
    }

    @GetMapping("/test")
    fun test() {
        val nodes = nodeRepository.findAll()
        nodeRepository.saveAll(nodes)
    }
}
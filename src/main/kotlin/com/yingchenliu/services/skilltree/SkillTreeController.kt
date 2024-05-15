package com.yingchenliu.services.skilltree

import com.yingchenliu.services.skilltree.domains.TreeNode
import com.yingchenliu.services.skilltree.repositories.TreeNodeRepository
import org.springframework.web.bind.annotation.*


@RestController
class SkillTreeController(val nodeRepository: TreeNodeRepository) {

    @GetMapping("/nodes/root")
    fun findRoot(): TreeNode {
        return nodeRepository.findByName("Root")
    }

    @PutMapping("/nodes/{id}")
    fun updateById(@PathVariable("id") id: Long, @RequestBody node: TreeNode): TreeNode {
       return nodeRepository.save(node)
    }

    @GetMapping("/test")
    fun test() {
        nodeRepository.deleteAll()
        val java = TreeNode(null, "Java", null, null, emptySet())
        val js = TreeNode(null, "JavaScript", null, null, emptySet())
        val root = TreeNode(null, "Root", null, null, setOf(java, js))
        nodeRepository.saveAll(mutableListOf(java, js, root))
    }
}
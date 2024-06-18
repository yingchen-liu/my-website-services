package com.yingchenliu.services.domains

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import java.time.LocalDateTime
import java.util.*

@Node
data class TreeNode(
    @Id
    val uuid: UUID,
    val name: String,
    val subtitle: String?,
    val content: String?,

    @Relationship(type = "PARENT_OF")
    val children: List<TreeNode>?,

    val isDeleted: Boolean = false,
    val isCollapsed: Boolean = false,
    val isRelationship: Boolean = false,

    val createdAt: LocalDateTime?,
    val lastUpdatedAt: LocalDateTime?
) {
    override fun toString(): String {
        return "TreeNode(name=$name)"
    }
}
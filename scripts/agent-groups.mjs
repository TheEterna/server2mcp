#!/usr/bin/env node

/**
 * Agent 分组管理工具 — api2mcp4j
 *
 * 解析 .claude/agents/agent-groups.json，提供 agent 分组查询、列表、搜索、双向一致性验证。
 * 继承自 ../real-agent · 董事长 2026-06-24 批准方案B（裁剪为纯 Java 库适配版）。
 *
 * 用法：node scripts/agent-groups.mjs list|find|verify|stats
 */

import { readFileSync, readdirSync } from 'fs'
import { join, resolve } from 'path'

const ROOT = resolve(import.meta.dirname, '..')
const GROUPS_FILE = join(ROOT, '.claude', 'agents', 'agent-groups.json')
const AGENTS_DIR = join(ROOT, '.claude', 'agents')

// ==================== 加载分组数据 ====================

function loadGroups() {
  try {
    return JSON.parse(readFileSync(GROUPS_FILE, 'utf-8'))
  } catch (e) {
    console.error('无法读取 agent-groups.json:', e.message)
    process.exit(1)
  }
}

// ==================== 命令：list ====================

function cmdList(groupId) {
  const data = loadGroups()
  const groups = groupId
    ? data.groups.filter(g => g.id === groupId)
    : data.groups

  if (groups.length === 0) {
    console.error(`未找到分组: ${groupId}`)
    console.error(`可用分组: ${data.groups.map(g => g.id).join(', ')}`)
    process.exit(1)
  }

  for (const group of groups) {
    console.log(`\n╔══ ${group.name}（${group.id}）══╗`)
    console.log(`║  ${group.description}`)
    console.log(`║  成员: ${group.agents.length} 人`)
    console.log('╠══════════════════════════════════╣')

    const maxNameLen = Math.max(...group.agents.map(a => a.name.length))
    for (const agent of group.agents) {
      const name = agent.name.padEnd(maxNameLen)
      const caps = agent.capabilities
        ? agent.capabilities.join(' / ')
        : agent.category || ''
      console.log(`║  ${name}  ${agent.role}  [${caps}]`)
    }
    console.log('╚══════════════════════════════════╝')
  }
}

// ==================== 命令：find ====================

function cmdFind(keyword) {
  const data = loadGroups()
  const results = []

  for (const group of data.groups) {
    for (const agent of group.agents) {
      const searchText = [
        agent.name,
        agent.role,
        ...(agent.capabilities || []),
        agent.category || '',
      ].join(' ').toLowerCase()

      if (searchText.includes(keyword.toLowerCase())) {
        results.push({ group: group.name, groupId: group.id, ...agent })
      }
    }
  }

  if (results.length === 0) {
    console.log(`未找到匹配 "${keyword}" 的 agent`)
    return
  }

  console.log(`\n找到 ${results.length} 个匹配 "${keyword}" 的 agent:\n`)
  for (const r of results) {
    const caps = r.capabilities ? r.capabilities.join(' / ') : r.category || ''
    console.log(`  ${r.name}  (${r.role})  [${r.group}]`)
    console.log(`    能力: ${caps}`)
    console.log(`    调用: Agent(subagent_type="${r.name}")`)
    console.log()
  }
}

// ==================== 命令：verify ====================

function cmdVerify() {
  const data = loadGroups()
  const existingFiles = new Set(
    readdirSync(AGENTS_DIR).filter(f => f.endsWith('.md'))
  )
  let errors = 0
  let warnings = 0

  console.log('\n验证 agent-groups.json 与实际文件的一致性...\n')

  // 方向一：JSON 中引用的文件必须存在
  for (const group of data.groups) {
    for (const agent of group.agents) {
      if (!existingFiles.has(agent.file)) {
        console.error(`  ✗ [${group.id}] ${agent.name} 引用的文件 ${agent.file} 不存在`)
        errors++
      }
    }
  }

  // 方向二：目录中的 .md 文件必须都被分组收录
  const registeredFiles = new Set()
  for (const group of data.groups) {
    for (const agent of group.agents) {
      registeredFiles.add(agent.file)
    }
  }

  for (const file of existingFiles) {
    if (!registeredFiles.has(file)) {
      console.error(`  ✗ 文件 ${file} 未被任何分组收录`)
      errors++
    }
  }

  // 统计
  const totalAgents = data.groups.reduce((sum, g) => sum + g.agents.length, 0)

  if (errors === 0) {
    console.log('  ✓ 双向一致：所有引用的文件都存在，所有 .md 文件都被收录')
  }

  console.log(`\n统计: ${data.groups.length} 个分组, ${totalAgents} 个注册项, ${existingFiles.size} 个 .md 文件`)

  if (errors > 0) {
    console.error(`\n✗ 验证失败：${errors} 处不一致${warnings ? `，${warnings} 处警告` : ''}`)
    process.exit(1)
  }
  console.log('\n✓ 验证通过 (EXIT=0)')
}

// ==================== 命令：stats ====================

function cmdStats() {
  const data = loadGroups()

  console.log('\n╔══ Agent 分组统计 ══╗\n')

  for (const group of data.groups) {
    console.log(`  ${group.name}（${group.id}）: ${group.agents.length} 人`)
    for (const agent of group.agents) {
      console.log(`    └─ ${agent.name}（${agent.role}）· ${agent.model || '?'}`)
    }
  }

  const totalAgents = data.groups.reduce((sum, g) => sum + g.agents.length, 0)
  console.log(`\n  总计: ${totalAgents} 个 agent`)
  console.log('╚══════════════════════╝')
}

// ==================== 主入口 ====================

const [,, cmd, ...args] = process.argv

switch (cmd) {
  case 'list':
    cmdList(args[0])
    break
  case 'find':
    if (!args[0]) {
      console.error('用法: node scripts/agent-groups.mjs find <关键词>')
      process.exit(1)
    }
    cmdFind(args[0])
    break
  case 'verify':
    cmdVerify()
    break
  case 'stats':
    cmdStats()
    break
  default:
    console.log(`
Agent 分组管理工具 — api2mcp4j

用法:
  node scripts/agent-groups.mjs list [分组ID]    列出分组成员
  node scripts/agent-groups.mjs find <关键词>    按能力/名称搜索 agent
  node scripts/agent-groups.mjs verify           验证文件双向一致（不一致则 EXIT=1）
  node scripts/agent-groups.mjs stats            显示统计信息

分组 ID:
  command     指挥层（架构设计/团队编排/基建维护）
  execution   执行层（实现/测试/文档/提交）
  quality     质检层（独立审查/根因/代码定位）

示例:
  node scripts/agent-groups.mjs list command
  node scripts/agent-groups.mjs find 解析器
  node scripts/agent-groups.mjs verify
`)
}

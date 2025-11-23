import {defineConfig} from 'vitepress'
import fs from 'fs'
import path from 'path'
import {fileURLToPath} from 'url'
import {InlineLinkPreviewElementTransform} from '@nolebase/vitepress-plugin-inline-link-preview/markdown-it'
import {GitChangelog, GitChangelogMarkdownSection,} from '@nolebase/vitepress-plugin-git-changelog/vite'
import {BiDirectionalLinks} from '@nolebase/markdown-it-bi-directional-links'
import timeline from "vitepress-markdown-timeline";
import {groupIconMdPlugin, groupIconVitePlugin} from 'vitepress-plugin-group-icons'
import {vitepressPluginLegend} from 'vitepress-plugin-legend'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const docsDir = path.resolve(__dirname, '..')

/**
 * 获取模块的显示名称
 */
function getModuleDisplayName(modulePath) {
  const indexPath = path.join(modulePath, 'index.md')
  if (fs.existsSync(indexPath)) {
    const content = fs.readFileSync(indexPath, 'utf-8')
    const match = content.match(/^#\s+(.+)$/m)
    if (match) {
      return match[1].trim()
    }
  }
  return path.basename(modulePath)
}

/**
 * 递归查找所有模块（从 docs 目录）
 */
function findModules(dir, basePath = '') {
  const modules = []

  if (!fs.existsSync(dir)) {
    return modules
  }

  const items = fs.readdirSync(dir, {withFileTypes: true})

  for (const item of items) {
    // 跳过隐藏文件和特殊目录
    if (item.name.startsWith('.') ||
        item.name === 'node_modules' ||
        item.name === '.vitepress') {
      continue
    }

    if (item.isDirectory() && /^\d/.test(item.name)) {
      const modulePath = path.join(dir, item.name)
      const relativePath = basePath ? `${basePath}/${item.name}` : item.name
      const indexPath = path.join(modulePath, 'index.md')

      if (fs.existsSync(indexPath)) {
        const displayName = getModuleDisplayName(modulePath)
        const link = `/${relativePath}/`

        const moduleInfo = {
          text: displayName,
          link: link
        }

        // 递归查找子模块
        const subModules = findModules(modulePath, relativePath)
        if (subModules.length > 0) {
          moduleInfo.items = subModules
        }

        modules.push(moduleInfo)
      } else {
        // 即使没有 index.md，也继续查找子模块
        const subModules = findModules(modulePath, relativePath)
        modules.push(...subModules)
      }
    }
  }

  // 按目录名排序
  return modules.sort((a, b) => {
    const aMatch = a.link.match(/(\d+(?:\.\d+)?)/)
    const bMatch = b.link.match(/(\d+(?:\.\d+)?)/)
    if (!aMatch || !bMatch) {
      return 0
    }

    const aNum = aMatch[1].split('.').map(n => parseInt(n.padStart(3, '0')))
    const bNum = bMatch[1].split('.').map(n => parseInt(n.padStart(3, '0')))

    for (let i = 0; i < Math.max(aNum.length, bNum.length); i++) {
      const aVal = aNum[i] || 0
      const bVal = bNum[i] || 0
      if (aVal !== bVal) {
        return aVal - bVal
      }
    }
    return 0
  })
}

/**
 * 将模块分类
 */
function categorizeModules(modules) {
  const categorized = {}

  for (const module of modules) {
    const moduleNum = module.link.match(/\/(\d+)/)?.[1]
    if (!moduleNum) {
      continue
    }

    const num = parseInt(moduleNum)

    // 跳过 num === 0 的模块（0.spring-ai-introduction），因为它会单独处理
    if (num === 0) {
      continue
    }

    let category = '其他'
    if (num <= 2) {
      category = '入门'
    } else if (num <= 5) {
      category = '核心功能'
    } else if (num <= 13) {
      category = 'Model API'
    } else if (num <= 18) {
      category = '高级功能'
    } else {
      category = '部署与测试'
    }

    if (!categorized[category]) {
      categorized[category] = []
    }
    categorized[category].push(module)
  }

  return categorized
}

/**
 * 获取 0.spring-ai-introduction 模块信息
 */
function getIntroductionModule() {
  const introPath = path.join(docsDir, '0.spring-ai-introduction')
  const indexPath = path.join(introPath, 'index.md')

  if (fs.existsSync(indexPath)) {
    const displayName = getModuleDisplayName(introPath)
    return {
      text: displayName,
      link: '/0.spring-ai-introduction/'
    }
  }
  return null
}

/**
 * 生成侧边栏配置
 */
function generateSidebar() {
  const allModules = findModules(docsDir)
  const categorized = categorizeModules(allModules)

  const sidebar = {
    '/': []
  }

  const categoryOrder = ['简介', '入门', '核心功能', 'Model API', '高级功能', '部署与测试']

  // 单独处理"简介"分类
  const introModule = getIntroductionModule()
  if (introModule) {
    sidebar['/'].push({
                        text: '简介',
                        items: [introModule]
                      })
  }

  // 处理其他分类（排除"简介"，因为已经单独处理了）
  for (const category of categoryOrder) {
    if (category === '简介') {
      continue
    }

    if (categorized[category] && categorized[category].length > 0) {
      sidebar['/'].push({
                          text: category,
                          items: categorized[category]
                        })
    }
  }

  return sidebar
}

export default defineConfig(
    {

      vite: {
        publicDir: path.resolve(__dirname, '../public'),
        plugins: [
          groupIconVitePlugin(
              {
                // 自定义图标: https://github.com/vscode-icons/vscode-icons/wiki/ListOfFiles
                customIcon: {
                  'java': 'vscode-icons:file-type-java',
                  'xml': 'vscode-icons:file-type-xml',
                  'maven': 'vscode-icons:file-type-maven',
                  'unplugin': 'https://unplugin.unjs.io/logo_light.svg',
                },
              }),
          GitChangelog(
              {
                // Fill in your repository URL here
                repoURL: () => 'https://github.com/dong4j/spring-ai-cookbook',
              }),
          GitChangelogMarkdownSection(),
        ],
        optimizeDeps: {
          exclude: [
            '@nolebase/vitepress-plugin-enhanced-readabilities/client',
            'vitepress',
            '@nolebase/ui',
          ],
        },
        ssr: {
          noExternal: [
            // If there are other packages that need to be processed by Vite, you can add them here.
            '@nolebase/vitepress-plugin-enhanced-readabilities',
            '@nolebase/ui',
            '@nolebase/vitepress-plugin-highlight-targeted-heading',
            '@nolebase/vitepress-plugin-inline-link-preview',
            '@nolebase/vitepress-plugin-git-changelog',
          ],
        },
      },

      title: 'Spring AI Cookbook',
      description: 'Spring AI 教程文档',
      base: '/',
      lang: 'zh-CN',

      // 域名配置
      // 如果部署到子路径，修改 base 为 '/spring-ai-cookbook/'
      // 当前配置为根域名 springai.dong4j.site

      // 忽略死链接检查（用于开发环境的 localhost 链接等）
      ignoreDeadLinks: [
        /^http:\/\/localhost/,
        /^https:\/\/localhost/
      ],

      head: [
        ['link', {rel: 'icon', href: '/favicon.ico'}],
        ['script', {
          src: 'https://cdn.dong4j.site/source/static/busuanzi.self.js',
          async: true,
          'data-api': 'https://api.dong4j.site/busuanzi/api',
          'data-prefix': 'busuanzi',
          'data-style': 'default',
          'data-pjax': 'true'
        }],
        ['script', {
          src: 'https://cdn.dong4j.site/source/static/umami.self.js',
          defer: true,
          'data-host-url': 'https://umami.dong4j.site',
          'data-website-id': '1e2e22cc-637c-49e6-aeaf-c2bd0b47f92b'
        }]
      ],

      // markdown配置
      markdown: {
        image: {
          // 开启图片懒加载
          lazyLoading: true
        },
        config(md) {
          // other markdown-it configurations...
          md.use(InlineLinkPreviewElementTransform)
          md.use(BiDirectionalLinks())
          md.use(timeline)
          md.use(groupIconMdPlugin, {
            titleBar: {includeSnippet: true},
          })
          vitepressPluginLegend(md, {
            markmap: {showToolbar: true}, // 显示脑图工具栏
            mermaid: true // 启用 Mermaid
          })
          md.renderer.rules.heading_close = (tokens, idx, options, env, slf) => {
            let htmlResult = slf.renderToken(tokens, idx, options);
            if (tokens[idx].tag === 'h1') {
              htmlResult += `<ArticleMetadata />`;
            }
            return htmlResult;
          }
        }
      },

      themeConfig: {
        siteTitle: 'Spring AI Cookbook',
        logo: '/logo.png',

        nav: [
          {text: '🏠 首页', link: '/'},
          {text: '🚀 开始', link: '/1.spring-ai-started/'},
          {text: '📊 统计', link: 'https://umami.dong4j.site/share/o0wIhLdP1EwFcdCt/spring-ai.dong4j.site', target: '_blank'}
        ],

        sidebar: generateSidebar(),

        socialLinks: [
          {icon: 'github', link: 'https://github.com/dong4j/spring-ai-cookbook'}
        ],

        footer: {
          message: '基于 Spring AI 构建',
          copyright: 'Copyright © 2025 Spring AI Cookbook'
        },

        search: {
          provider: 'local'
        },

        editLink: {
          pattern: 'https://github.com/dong4j/spring-ai-cookbook/edit/main/docs/:path',
          text: '在 GitHub 上编辑此页'
        },

        lastUpdated: {
          text: '最后更新于',
          formatOptions: {
            dateStyle: 'short',
            timeStyle: 'medium'
          }
        },

        outline: {
          level: [2, 4],
          label: '页面大纲'
        }
      }
    })

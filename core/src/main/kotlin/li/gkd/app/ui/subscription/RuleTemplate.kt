package li.gkd.app.ui.subscription

import li.gkd.app.R

/**
 * Starting text for a new rule group. The JSON5 parser accepts comments and fills a
 * missing `key`, so the template only carries the fields a user has to fill in.
 */
object RuleTemplate {
    fun forAppGroup(): String = """
        {
          name: '规则组名称',
          // 生效页面，留空表示应用内所有页面
          // activityIds: ['com.example.app.MainActivity'],
          // 版本条件
          // versionCode: { minimum: 1 },
          // versionName: { include: ['1.0.0'] },
          rules: [
            {
              // 同一节点需同时满足多个条件时连续书写
              matches: '[text="示例文本"]',
              // clickNode / clickCenter / click / longClickNode / longClickCenter / longClick / back / none / swipe
              action: 'clickNode',
              // 节点内相对位置，例如 position: { left: 'width/2', top: 'height/2' }
            },
          ],
        }
    """.trimIndent()

    fun forGlobalGroup(): String = """
        {
          name: '规则组名称',
          // 默认作用于所有应用，可改为 false 后通过 apps 指定
          matchAnyApp: true,
          rules: [
            {
              // 限定应用时填写，例如 apps: [{ id: 'com.example.app' }]
              matches: '[text="示例文本"]',
              action: 'clickNode',
            },
          ],
        }
    """.trimIndent()

    fun forNewApp(): String = """
        {
          id: 'com.example.app',
          name: '应用名称',
          groups: [
            {
              name: '规则组名称',
              rules: [
                {
                  matches: '[text="示例文本"]',
                  action: 'clickNode',
                },
              ],
            },
          ],
        }
    """.trimIndent()
}

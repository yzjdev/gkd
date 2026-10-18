package li.gkd.app.ui.subscription

import li.gkd.app.subscription.RawSubscription
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 可视化草稿与 data class 的往返映射：每个字段都要能读出、写回，
 * 且未设置的字段不能被凭空生成。
 */
class RuleVisualDraftTest {
    private val subscription = RawSubscription.parse(
        """{id:1,name:'S',version:1,
          globalGroups:[{key:1,name:'全局',enable:false,matchAnyApp:true,rules:[{matches:'*'}]}],
          apps:[{id:'app',groups:[
            {key:1,name:'开屏',enable:true,fastQuery:true,rules:[{matches:'*'}]},
            {key:2,name:'广告',enable:false,matchRoot:false,rules:[{matches:'*'}]},
            {key:3,name:'未设置',rules:[{matches:'*'}]}
          ]}]}""",
    )

    @Test
    fun groupScalarFieldsRoundTrip() {
        val draft = RuleVisualDraft.fromGroup(subscription.getAppGroups("app")[0])
        assertEquals("开屏", draft.values["name"])
        assertEquals("1", draft.values["key"])
        assertEquals("true", draft.values["enable"])
        assertEquals("true", draft.values["fastQuery"])

        val json = RuleVisualDraft.toJson5(draft, VisualTarget.AppGroup)!!
        assertTrue(json.contains("enable: true"), json)
        assertTrue(json.contains("name: '开屏'"), json)
    }

    @Test
    fun disabledGroupKeepsFalseInsteadOfUnset() {
        val draft = RuleVisualDraft.fromGroup(subscription.getAppGroups("app")[1])
        assertEquals("false", draft.values["enable"])
        assertEquals("false", draft.values["matchRoot"])

        val json = RuleVisualDraft.toJson5(draft, VisualTarget.AppGroup)!!
        assertTrue(json.contains("enable: false"), json)
        assertTrue(json.contains("matchRoot: false"), json)
    }

    @Test
    fun unsetGroupFieldsStayUnset() {
        val draft = RuleVisualDraft.fromGroup(subscription.getAppGroups("app")[2])
        assertEquals("", draft.values["enable"])
        val json = RuleVisualDraft.toJson5(draft, VisualTarget.AppGroup)!!
        assertFalse(json.contains("enable"), json)
    }

    @Test
    fun globalGroupScalarFieldsRoundTrip() {
        val draft = RuleVisualDraft.fromGroup(subscription.globalGroups.single())
        assertEquals("全局", draft.values["name"])
        assertEquals("false", draft.values["enable"])
        assertEquals("true", draft.values["matchAnyApp"])

        val json = RuleVisualDraft.toJson5(draft, VisualTarget.GlobalGroup)!!
        assertTrue(json.contains("enable: false"), json)
        assertTrue(json.contains("matchAnyApp: true"), json)
    }

    /** 全字段往返：每个属性都要经过草稿再回到解析结果，值不变。 */
    @Test
    fun everyAppGroupFieldSurvivesRoundTrip() {
        val source = RawSubscription.parse(
            """{id:1,name:'S',version:1,
              apps:[{id:'app',groups:[{key:7,name:'组名',desc:'描述',enable:true,
                scopeKeys:[1,2],actionCdKey:3,actionMaximumKey:4,actionCd:1500,actionDelay:200,
                fastQuery:true,matchRoot:true,actionMaximum:5,priorityTime:6000,
                priorityActionMaximum:2,order:3,forcedTime:7000,matchDelay:300,matchTime:8000,
                resetMatch:'match',snapshotUrls:['https://a','https://b'],
                excludeSnapshotUrls:['https://c'],exampleUrls:['https://d'],
                activityIds:['.A','.B'],excludeActivityIds:['.C'],
                versionCode:{minimum:1,maximum:99,include:[2],exclude:[3]},
                versionName:{pattern:'x.*',include:['1.0'],exclude:['2.0']},
                ignoreGlobalGroupMatch:true,
                rules:[{key:1,name:'规则',preKeys:[9],action:'clickCenter',
                  position:{left:'1',top:'2',right:'3',bottom:'4',x:'5',y:'6'},
                  swipeArg:{start:{left:'1',top:'2'},end:{left:'3',top:'4'},duration:250},
                  matches:['[text="a"]'],excludeMatches:['[text="b"]'],
                  excludeAllMatches:['[text="c"]'],anyMatches:['[text="d"]'],
                  actionCdKey:11,actionMaximumKey:12,actionCd:1300,actionDelay:140,
                  fastQuery:true,matchRoot:true,actionMaximum:15,priorityTime:1600,
                  priorityActionMaximum:17,order:18,forcedTime:1900,matchDelay:210,
                  matchTime:2200,resetMatch:'app',
                  snapshotUrls:['https://e'],excludeSnapshotUrls:['https://f'],
                  exampleUrls:['https://g'],activityIds:['.R'],excludeActivityIds:['.X'],
                  versionCode:{minimum:5},versionName:{include:['3.0']}}]}]}]}""",
        )
        val original = source.getAppGroups("app").single()
        val draft = RuleVisualDraft.fromGroup(original)
        val text = RuleVisualDraft.toJson5(draft, VisualTarget.AppGroup)!!
        // toJson5 只生成规则组对象，解析回去需要补上订阅根级字段。
        val restored = RawSubscription.parse(
            "{id:1,name:'S',version:1,apps:[{id:'app',groups:[$text]}]}",
        ).getAppGroups("app").single()

        assertEquals(original.key, restored.key)
        assertEquals(original.name, restored.name)
        assertEquals(original.desc, restored.desc)
        assertEquals(original.enable, restored.enable)
        assertEquals(original.scopeKeys, restored.scopeKeys)
        assertEquals(original.actionCdKey, restored.actionCdKey)
        assertEquals(original.actionMaximumKey, restored.actionMaximumKey)
        assertEquals(original.actionCd, restored.actionCd)
        assertEquals(original.actionDelay, restored.actionDelay)
        assertEquals(original.fastQuery, restored.fastQuery)
        assertEquals(original.matchRoot, restored.matchRoot)
        assertEquals(original.actionMaximum, restored.actionMaximum)
        assertEquals(original.priorityTime, restored.priorityTime)
        assertEquals(original.priorityActionMaximum, restored.priorityActionMaximum)
        assertEquals(original.order, restored.order)
        assertEquals(original.forcedTime, restored.forcedTime)
        assertEquals(original.matchDelay, restored.matchDelay)
        assertEquals(original.matchTime, restored.matchTime)
        assertEquals(original.resetMatch, restored.resetMatch)
        assertEquals(original.snapshotUrls, restored.snapshotUrls)
        assertEquals(original.excludeSnapshotUrls, restored.excludeSnapshotUrls)
        assertEquals(original.exampleUrls, restored.exampleUrls)
        assertEquals(original.activityIds, restored.activityIds)
        assertEquals(original.excludeActivityIds, restored.excludeActivityIds)
        assertEquals(original.versionCode, restored.versionCode)
        assertEquals(original.versionName, restored.versionName)
        assertEquals(original.ignoreGlobalGroupMatch, restored.ignoreGlobalGroupMatch)

        val rule = original.rules.single()
        val back = restored.rules.single()
        assertEquals(rule.key, back.key)
        assertEquals(rule.name, back.name)
        assertEquals(rule.preKeys, back.preKeys)
        assertEquals(rule.action, back.action)
        assertEquals(rule.position, back.position)
        assertEquals(rule.swipeArg, back.swipeArg)
        assertEquals(rule.matches, back.matches)
        assertEquals(rule.excludeMatches, back.excludeMatches)
        assertEquals(rule.excludeAllMatches, back.excludeAllMatches)
        assertEquals(rule.anyMatches, back.anyMatches)
        assertEquals(rule.actionCdKey, back.actionCdKey)
        assertEquals(rule.actionMaximumKey, back.actionMaximumKey)
        assertEquals(rule.actionCd, back.actionCd)
        assertEquals(rule.actionDelay, back.actionDelay)
        assertEquals(rule.fastQuery, back.fastQuery)
        assertEquals(rule.matchRoot, back.matchRoot)
        assertEquals(rule.actionMaximum, back.actionMaximum)
        assertEquals(rule.priorityTime, back.priorityTime)
        assertEquals(rule.priorityActionMaximum, back.priorityActionMaximum)
        assertEquals(rule.order, back.order)
        assertEquals(rule.forcedTime, back.forcedTime)
        assertEquals(rule.matchDelay, back.matchDelay)
        assertEquals(rule.matchTime, back.matchTime)
        assertEquals(rule.resetMatch, back.resetMatch)
        assertEquals(rule.snapshotUrls, back.snapshotUrls)
        assertEquals(rule.excludeSnapshotUrls, back.excludeSnapshotUrls)
        assertEquals(rule.exampleUrls, back.exampleUrls)
        assertEquals(rule.activityIds, back.activityIds)
        assertEquals(rule.excludeActivityIds, back.excludeActivityIds)
        assertEquals(rule.versionCode, back.versionCode)
        assertEquals(rule.versionName, back.versionName)
    }
}

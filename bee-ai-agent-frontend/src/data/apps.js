export const chatApps = {
  love: {
    key: 'love',
    route: '/love',
    title: 'AI 恋爱大师',
    navTitle: '恋爱大师',
    description: '把暧昧、沟通、复盘和关系判断交给 AI 一起梳理。',
    usesChatId: true,
    accent: 'love',
    placeholder: '说说你的恋爱困惑...',
    emptyTitle: '有什么恋爱问题想聊聊？',
    emptySubtitle: '把事情经过、对方反应和你的想法说清楚，我帮你具体分析~',
    suggestions: [
      '我一直单身，是圈子太小还是自己没吸引力？',
      '暧昧对象忽冷忽热，我该继续还是止损？',
      '恋爱久了没激情，是不爱了还是不会经营？',
      '已婚后总为钱和家务吵架，问题到底出在哪？',
      '我对她很好，她为什么还是不喜欢我？',
    ],
  },
  manus: {
    key: 'manus',
    route: '/manus',
    title: 'AI 超级智能体',
    navTitle: '超级智能体',
    description: '面向任务规划、工具调用和复杂问题拆解的智能体对话。',
    usesChatId: false,
    accent: 'manus',
    placeholder: '描述你希望智能体完成的任务...',
    emptyTitle: '今天要完成什么任务？',
    emptySubtitle: '智能体会实时拆解任务过程，并返回执行结果~',
    suggestions: [
      '我和对象周末想去深圳湾附近玩，请帮我规划吃饭、散步和拍照路线',
      '我周末想和对象在深圳湾公园附近约会，请帮我规划一条半日路线',
      '我在深圳市南山区海岸城附近，请推荐几家好吃又适合约会的餐厅',
      '帮我规划一个三天学习 Spring AI 的计划'
    ],
  },
}

export const appList = Object.values(chatApps)

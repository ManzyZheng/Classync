// 停用词库（基于 baidu_stopwords.txt）
// 用于过滤词云中的无意义词汇

// 停用词列表（从 baidu_stopwords.txt 提取）
// 使用 Set 数据结构提高查找效率
let STOPWORDS_SET = null

/**
 * 初始化停用词集合
 * 从文件加载或使用内置列表
 */
function initStopwords() {
    if (STOPWORDS_SET) {
        return STOPWORDS_SET
    }

    // 尝试从文件加载
    const stopwordsList = [
        // 英文停用词
        'a', 'an', 'the', 'and', 'or', 'but', 'in', 'on', 'at', 'to', 'for', 'of', 'with', 'by',
        'is', 'are', 'was', 'were', 'be', 'been', 'being', 'have', 'has', 'had', 'do', 'does', 'did',
        'will', 'would', 'should', 'could', 'may', 'might', 'must', 'can', 'this', 'that', 'these', 'those',
        'i', 'you', 'he', 'she', 'it', 'we', 'they', 'me', 'him', 'her', 'us', 'them',
        'my', 'your', 'his', 'her', 'its', 'our', 'their', 'what', 'which', 'who', 'whom', 'whose',
        'where', 'when', 'why', 'how', 'all', 'each', 'every', 'both', 'few', 'more', 'most', 'other', 'some',
        'such', 'no', 'nor', 'not', 'only', 'own', 'same', 'so', 'than', 'too', 'very', 'just', 'now',

        // 中文停用词（常用）
        '的', '了', '在', '是', '我', '有', '和', '就', '不', '人', '都', '一', '一个', '上', '也', '很', '到', '说', '要', '去', '你', '会', '着', '没有', '看', '好', '自己', '这',
        '那', '他', '她', '它', '们', '这个', '那个', '这些', '那些', '这样', '那样', '这里', '那里',
        '什么', '怎么', '为什么', '如何', '哪里', '哪个', '哪些', '多少', '何时',
        '因为', '所以', '但是', '然而', '而且', '或者', '如果', '虽然', '尽管', '即使', '无论',
        '可以', '能够', '应该', '必须', '需要', '想要', '希望', '觉得', '认为', '知道', '了解',
        '进行', '开始', '结束', '完成', '实现', '得到', '获得', '使用', '利用', '通过', '根据',
        '关于', '对于', '由于', '按照', '依照', '根据', '通过', '经过', '随着',
        '一些', '一切', '一般', '一起', '一直', '一定', '一样', '一次', '一面',
        '不是', '不会', '不能', '不要', '不过', '不但', '不仅', '不管', '不论',
        '还是', '还有', '还要',
        '已经', '正在', '将要', '曾经', '现在', '过去', '将来', '今天', '明天', '昨天',
        '可能', '也许', '大概', '似乎', '好像', '仿佛',
        '非常', '很', '更', '最', '比较', '相当', '十分', '特别', '尤其',
        '首先', '其次', '然后', '最后', '接着', '同时', '另外', '此外', '而且',
        '为了', '以便', '以免', '由于', '因为', '所以', '因此', '于是',
        '但是', '可是', '然而', '不过', '只是',
        '如果', '假如', '倘若', '要是', '只要', '只有', '除非',
        '虽然', '尽管', '即使', '纵然', '哪怕',
        '无论', '不管', '不论',
        '或者', '或是', '要么', '还是',
        '以及', '和', '与', '同', '跟', '及',
        '从', '向', '往', '朝', '到', '在', '于', '由', '自', '从',
        '对', '对于', '关于', '至于',
        '被', '让', '使', '把', '将',
        '的', '地', '得', '着', '了', '过', '呢', '吗', '吧', '啊', '呀', '啦',

        // 标点符号和特殊字符
        '?', '!', '.', ',', ';', ':', '"', "'", '(', ')', '[', ']', '{', '}',
        '--', '——', '…', '、', '。', '，', '；', '：', '？', '！',

        // 数字
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '一', '二', '三', '四', '五', '六', '七', '八', '九', '十', '百', '千', '万',

        // 其他常见无意义词
        '等', '等等', '之类', '的话', '来说', '而言',
        '时候', '时间', '地方', '方面', '问题', '情况', '事情',
        '方法', '方式', '办法', '手段', '途径',
        '结果', '效果', '影响', '作用', '意义', '价值',
        '原因', '理由', '根据', '依据', '基础',
        '过程', '阶段', '步骤', '环节', '部分',
        '内容', '形式', '类型', '种类', '类别',
        '程度', '范围', '领域', '方面', '角度',
        '关系', '联系', '区别', '差异', '相同', '不同',
        '特点', '特征', '性质', '属性', '本质',
        '发展', '变化', '改变', '改善', '提高', '增加', '减少',
        '重要', '主要', '关键', '核心', '中心', '重点',
        '基本', '根本', '基础', '前提', '条件',
        '必须', '必要', '需要', '要求', '应该', '应当',
        '可以', '能够', '可能', '也许', '大概',
        '一定', '肯定', '确定', '明确', '清楚',
        '很多', '许多', '大量', '多数', '大多数', '全部', '所有',
        '一些', '一点', '几个', '少数', '个别'
    ]

    STOPWORDS_SET = new Set(stopwordsList)
    return STOPWORDS_SET
}

/**
 * 检查一个词是否是停用词
 * @param {string} word - 要检查的词
 * @returns {boolean} - 如果是停用词返回 true
 */
export function isStopword(word) {
    if (!word || word.trim().length === 0) {
        return true
    }

    const stopwords = initStopwords()
    const normalized = word.toLowerCase().trim()

    // 检查原词和规范化后的词
    return stopwords.has(normalized) || stopwords.has(word.trim())
}

/**
 * 过滤停用词
 * @param {string[]} words - 词数组
 * @returns {string[]} - 过滤后的词数组
 */
export function filterStopwords(words) {
    return words.filter(word => !isStopword(word))
}

/**
 * 从文件加载停用词（异步）
 * 如果文件加载失败，使用内置列表
 * @returns {Promise<Set<string>>}
 */
export async function loadStopwordsFromFile() {
    try {
        const response = await fetch('/stopwords/baidu_stopwords.txt')
        if (!response.ok) {
            throw new Error('Failed to fetch stopwords file')
        }
        const text = await response.text()
        const words = text.split('\n')
            .map(line => line.trim())
            .filter(line => line.length > 0 && !line.startsWith('#'))

        STOPWORDS_SET = new Set(words)
        return STOPWORDS_SET
    } catch (error) {
        console.warn('Failed to load stopwords file, using default stopwords:', error)
        return initStopwords()
    }
}

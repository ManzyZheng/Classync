import { isStopword } from './stopwords'

// 动态导入 jieba-wasm
let jiebaModule = null
let jiebaInitialized = false
let jiebaInitPromise = null

/**
 * 初始化 jieba（单例模式）
 */
export const initJiebaOnce = async () => {
    if (jiebaInitialized && jiebaModule) {
        return
    }
    if (jiebaInitPromise) {
        return jiebaInitPromise
    }
    jiebaInitPromise = (async () => {
        try {
            console.log('[WordCloud] Initializing jieba-wasm...')
            jiebaModule = await import('jieba-wasm')
            console.log('[WordCloud] Jieba module loaded, initializing...')
            await jiebaModule.default()
            jiebaInitialized = true
            console.log('[WordCloud] Jieba initialized successfully')
        } catch (error) {
            console.error('[WordCloud] Failed to initialize jieba:', error)
            jiebaInitPromise = null
            throw error
        }
    })()
    return jiebaInitPromise
}

/**
 * 使用 jieba 分词
 */
export const cutText = (text, useHMM = true) => {
    if (!jiebaModule || !jiebaInitialized) {
        throw new Error('Jieba not initialized')
    }
    if (!jiebaModule.cut) {
        throw new Error('Jieba cut function not available')
    }
    return jiebaModule.cut(text, !useHMM)
}

/**
 * 生成词云数据（词频统计）
 */
export const generateWordCloud = async (answers) => {
    if (!answers || answers.length === 0) {
        return []
    }

    try {
        console.log('[WordCloud] Starting word cloud generation with', answers.length, 'answers')

        // 确保 jieba 已初始化
        await initJiebaOnce()

        // 合并所有答案文本
        const allText = answers.map(a => a.content).join(' ')
        console.log('[WordCloud] Combined text length:', allText.length)

        // 配置参数 - 限制词数以提高性能
        const config = {
            maxWords: 50,            // 减少到50个词，提高性能
            minWordLength: 1,
            minFrequency: 1,
            includeNumbers: false,
            normalizePlurals: false
        }

        // 使用jieba进行中文分词
        const words = []
        const chineseText = allText.replace(/[^\u4e00-\u9fa5a-zA-Z0-9\s]/g, ' ')

        try {
            const segments = cutText(chineseText, true)
            words.push(...segments.filter(s => s && s.trim().length > 0))
        } catch (error) {
            console.warn('Jieba segmentation failed, using fallback:', error)
            const fallbackWords = chineseText.split(/\s+/).filter(s => s && s.trim().length > 0)
            words.push(...fallbackWords)
        }

        // 提取英文单词
        const englishWords = allText.match(/[a-zA-Z]{2,}/g) || []
        englishWords.forEach(word => {
            words.push(word.toLowerCase())
        })

        // 过滤和统计词频
        const frequency = {}

        words.forEach(word => {
            if (!word || word.trim().length === 0) {
                return
            }

            const trimmedWord = word.trim()

            // 最小词长度过滤
            if (trimmedWord.length < config.minWordLength) {
                return
            }

            // 英文单词至少2个字符
            if (/^[a-zA-Z]+$/.test(trimmedWord) && trimmedWord.length < 2) {
                return
            }

            // 过滤停用词
            if (isStopword(trimmedWord)) {
                return
            }

            // 过滤纯数字
            if (!config.includeNumbers && /^\d+$/.test(trimmedWord)) {
                return
            }

            // 过滤单个标点符号
            if (/^[^\u4e00-\u9fa5a-zA-Z0-9]+$/.test(trimmedWord)) {
                return
            }

            // 统计词频
            const normalizedWord = trimmedWord.toLowerCase()
            frequency[normalizedWord] = (frequency[normalizedWord] || 0) + 1
        })

        // 转换为数组并排序
        const sortedFrequencies = Object.entries(frequency)
            .filter(([word, count]) => count >= config.minFrequency)
            .sort((a, b) => b[1] - a[1])
            .slice(0, config.maxWords)

        if (sortedFrequencies.length === 0) {
            console.warn('[WordCloud] No valid words found after filtering')
            return []
        }

        // 归一化频率
        const maxFrequency = sortedFrequencies[0][1]
        const wordFrequency = sortedFrequencies.map(([word, count]) => ({
            word,
            count,
            normalizedFrequency: count / maxFrequency
        }))

        console.log('[WordCloud] Generated', wordFrequency.length, 'words')
        console.log('[WordCloud] Top words:', wordFrequency.slice(0, 10))

        return wordFrequency
    } catch (error) {
        console.error('[WordCloud] Failed to generate word cloud:', error)
        return []
    }
}

/**
 * 根据词频计算字体大小
 */
export const getWordSize = (count, normalizedFrequency, minSize = 20, maxSize = 56) => {
    const ratio = normalizedFrequency !== undefined ? normalizedFrequency : (count / 1)
    const relativeScaling = 0.5
    return minSize + ratio * relativeScaling * (maxSize - minSize) + (1 - relativeScaling) * (maxSize - minSize) * (1 - ratio)
}

/**
 * 根据词频获取样式类
 */
export const getWordClass = (normalizedFrequency) => {
    const ratio = normalizedFrequency
    if (ratio >= 0.8) {
        return 'word-tag-large'
    } else if (ratio >= 0.5) {
        return 'word-tag-medium'
    } else {
        return 'word-tag-small'
    }
}

/**
 * 根据词频获取颜色
 */
export const getWordColor = (normalizedFrequency, index) => {
    const ratio = normalizedFrequency

    const colorPalette = [
        // 高频词 - 深灰
        '#121212', '#2A2A2A', '#3D3D3D', '#4F4F4F',
        // 中频词 - 中灰
        '#696969', '#808080', '#A9A9A9', '#C0C0C0',
        // 低频词 - 浅灰
        '#D3D3D3', '#DCDCDC', '#E8E8E8', '#F0F0F0'
    ]

    if (ratio >= 0.8) {
        return colorPalette[index % 4]
    } else if (ratio >= 0.5) {
        return colorPalette[4 + (index % 4)]
    } else {
        return colorPalette[8 + (index % 4)]
    }
}

/**
 * 测量文本尺寸
 */
let measureCanvas = null
let measureCtx = null

export const measureTextSize = (text, fontSize) => {
    if (!measureCanvas) {
        measureCanvas = document.createElement('canvas')
        measureCtx = measureCanvas.getContext('2d')
    }
    measureCtx.font = `${fontSize}px sans-serif`
    const metrics = measureCtx.measureText(text)
    return {
        width: Math.ceil(metrics.width),
        height: Math.ceil(metrics.actualBoundingBoxAscent + metrics.actualBoundingBoxDescent)
    }
}

/**
 * 计算旋转后的包围框
 */
export const getRotatedBounds = (width, height, rotation) => {
    if (rotation === 0) {
        return { width, height }
    }
    const rad = (rotation * Math.PI) / 180
    const w = width * Math.abs(Math.cos(rad)) + height * Math.abs(Math.sin(rad))
    const h = width * Math.abs(Math.sin(rad)) + height * Math.abs(Math.cos(rad))
    return {
        width: Math.ceil(w),
        height: Math.ceil(h)
    }
}

/**
 * 检查词是否可以放置
 */
const canFitWord = (wordItem, position, placedWords, margin, containerWidth, containerHeight) => {
    const [x, y] = position
    const halfWidth = wordItem.width / 2
    const halfHeight = wordItem.height / 2

    // 检查是否超出容器边界
    if (x - halfWidth < margin || x + halfWidth > containerWidth - margin ||
        y - halfHeight < margin || y + halfHeight > containerHeight - margin) {
        return false
    }

    // 检查是否与已放置的词重叠
    for (const placed of placedWords) {
        const dx = Math.abs(x - placed.x)
        const dy = Math.abs(y - placed.y)
        const minDistX = (wordItem.width + placed.width) / 2 + margin
        const minDistY = (wordItem.height + placed.height) / 2 + margin

        if (dx < minDistX && dy < minDistY) {
            return false
        }
    }

    return true
}

/**
 * 基于螺旋扩散算法的位置计算
 */
const getSpiralPosition = (curWordItem, elWidth, elHeight, centerX, centerY, margin, placedWords) => {
    let startX, endX, startY, endY
    startX = endX = centerX
    startY = endY = centerY

    let stepLeft = 1
    let stepTop = 1
    if (elWidth > elHeight) {
        stepLeft = 1
        stepTop = elHeight / elWidth
    } else if (elHeight > elWidth) {
        stepTop = 1
        stepLeft = elWidth / elHeight
    }

    // 检查中心点
    if (canFitWord(curWordItem, [startX, startY], placedWords, margin, elWidth, elHeight)) {
        return [startX, startY]
    }

    // 螺旋扩散
    let iterations = 0
    const maxIterations = 10000 // 防止无限循环

    while (iterations < maxIterations) {
        iterations++

        // 向右
        endX += stepLeft
        for (let x = startX; x <= endX; x += stepLeft) {
            if (canFitWord(curWordItem, [x, startY], placedWords, margin, elWidth, elHeight)) {
                return [x, startY]
            }
        }
        startX = endX

        // 向下
        endY += stepTop
        for (let y = startY; y <= endY; y += stepTop) {
            if (canFitWord(curWordItem, [endX, y], placedWords, margin, elWidth, elHeight)) {
                return [endX, y]
            }
        }
        startY = endY

        // 向左
        endX -= stepLeft
        for (let x = startX; x >= endX; x -= stepLeft) {
            if (canFitWord(curWordItem, [x, endY], placedWords, margin, elWidth, elHeight)) {
                return [x, endY]
            }
        }
        startX = endX

        // 向上
        endY -= stepTop
        for (let y = startY; y >= endY; y -= stepTop) {
            if (canFitWord(curWordItem, [startX, y], placedWords, margin, elWidth, elHeight)) {
                return [startX, y]
            }
        }
        startY = endY

        // 检查是否超出容器范围太远
        if (Math.abs(startX - centerX) > elWidth * 1.5 || Math.abs(startY - centerY) > elHeight * 1.5) {
            console.warn('[WordCloud] Could not find position for word (out of bounds), placing at center')
            return [centerX, centerY]
        }
    }

    console.warn('[WordCloud] Max iterations reached, placing word at center')
    return [centerX, centerY]
}

/**
 * 计算词云位置
 */
export const calculateWordPositions = async (wordFrequency, containerWidth = 800, containerHeight = 500) => {
    if (wordFrequency.length === 0) {
        return []
    }

    const placedWords = []
    const wordPositions = []
    const centerX = containerWidth / 2
    const centerY = containerHeight / 2
    const margin = 40

    // 第一个词放在中心
    const firstWord = wordFrequency[0]
    const firstFontSize = getWordSize(firstWord.count, firstWord.normalizedFrequency)
    const firstSize = measureTextSize(firstWord.word, firstFontSize)
    const firstBounds = getRotatedBounds(firstSize.width, firstSize.height, 0)

    const firstWordItem = {
        x: centerX,
        y: centerY,
        width: firstBounds.width,
        height: firstBounds.height,
        rotation: 0
    }

    placedWords.push(firstWordItem)
    wordPositions.push({
        x: centerX,
        y: centerY,
        rotation: 0
    })

    // 为其他词找位置
    for (let i = 1; i < wordFrequency.length; i++) {
        const word = wordFrequency[i]
        const fontSize = getWordSize(word.count, word.normalizedFrequency)
        const rotation = (Math.sin(i * 2.3) * 10)
        const textSize = measureTextSize(word.word, fontSize)
        const bounds = getRotatedBounds(textSize.width, textSize.height, rotation)

        const curWordItem = {
            width: bounds.width,
            height: bounds.height,
            rotation
        }

        const [x, y] = getSpiralPosition(
            curWordItem,
            containerWidth,
            containerHeight,
            centerX,
            centerY,
            margin,
            placedWords
        )

        placedWords.push({
            x,
            y,
            width: bounds.width,
            height: bounds.height,
            rotation
        })

        wordPositions.push({ x, y, rotation })
    }

    return wordPositions
}

/**
 * 获取词的样式（用于 DOM 渲染）
 */
export const getWordStyle = (word, index, wordFrequency, wordPositions, containerWidth = 800, containerHeight = 500) => {
    const pos = wordPositions[index]
    if (!pos) return {}

    const fontSize = getWordSize(word.count, word.normalizedFrequency)
    const percentX = (pos.x / containerWidth) * 100
    const percentY = (pos.y / containerHeight) * 100
    const animationDelay = index * 0.05

    return {
        fontSize: fontSize + 'px',
        color: getWordColor(word.normalizedFrequency, index),
        left: percentX + '%',
        top: percentY + '%',
        transform: `translate(-50%, -50%) rotate(${pos.rotation || 0}deg)`,
        '--word-index': index,
        '--animation-delay': animationDelay + 's'
    }
}


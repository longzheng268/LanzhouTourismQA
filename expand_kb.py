#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json
import random

with open('src/main/resources/knowledge_base.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

kb = data['knowledge_base']
current_count = len(kb)
target_count = 2000
next_id = kb[-1]['id'] + 1

print(f"Current: {current_count} items, Target: {target_count}, Starting ID: {next_id}")

categories = ['景点', '美食', '文化', '体验', '交通', '实用信息', '地理', '购物', '历史', '住宿']

# Calculate items needed per category
items_needed = target_count - current_count
items_per_cat = items_needed // len(categories)
remainder = items_needed % len(categories)

# Simple Q&A templates per category
qa_templates = {
    '景点': [
        ('兰州{place}景点介绍', '兰州{place}是一处{adj}的景点，位于{loc}。{detail}'),
        ('{place}有什么特色？', '{place}以{feature}著称，{desc}。游客可以{activity}。'),
        ('如何游览{place}？', '游览{place}建议{time}前往，{tips}。门票{price}。'),
        ('{place}的历史背景', '{place}建于{year}，{history}。现已成为{status}。'),
    ],
    '美食': [
        ('兰州{dish}怎么做？', '{dish}的做法是{method}。主要材料包括{ingredients}。'),
        ('{restaurant}的{dish}怎么样？', '{restaurant}的{dish}{quality}，{desc}。'),
        ('兰州{dish}的特点', '{dish}具有{feature}的特点，{taste}。'),
        ('在哪里吃{dish}？', '正宗的{dish}可以在{location}吃到，{rec}。'),
    ],
    '文化': [
        ('兰州{culture}文化介绍', '兰州{culture}文化{desc}，{history}。'),
        ('{event}是什么？', '{event}{definition}，{detail}。'),
        ('兰州的{tradition}传统', '兰州的{tradition}传统{char}，{sig}。'),
        ('{art}在兰州的发展', '{art}在兰州{dev}，{status}。'),
    ],
    '体验': [
        ('兰州有什么{activity}活动？', '兰州提供{activity}等多种体验，{desc}。'),
        ('如何参加{activity}？', '参加{activity}需要{req}，{process}。'),
        ('{activity}的费用', '{activity}的费用约为{price}，{includes}。'),
        ('兰州{activity}最佳时间', '最佳时间是{season}，{reason}。'),
    ],
    '交通': [
        ('如何从{from}到{to}？', '可以乘坐{transport}，{duration}。费用约{price}。'),
        ('兰州{transport}运营时间', '{transport}运营时间是{time}，{freq}。'),
        ('{station}在哪里？', '{station}位于{location}，{access}。'),
        ('兰州{transport}票价', '{transport}票价为{price}，{detail}。'),
    ],
    '实用信息': [
        ('兰州的{service}服务', '兰州提供{service}服务，{desc}。'),
        ('在兰州{action}需要什么？', '需要{req}，{tips}。'),
        ('兰州的{facility}设施', '兰州的{facility}设施{desc}，{location}。'),
        ('如何{action}在兰州？', '{action}可以通过{method}，{process}。'),
    ],
    '地理': [
        ('兰州的地理位置', '兰州位于{location}，{coord}。'),
        ('兰州的{feature}地理特征', '兰州的{feature}{desc}，{sig}。'),
        ('{area}的地理环境', '{area}的地理环境{char}，{detail}。'),
        ('兰州的{river}河流', '兰州的{river}河流{desc}，{imp}。'),
    ],
    '购物': [
        ('在兰州买{product}去哪里？', '可以在{location}购买{product}，{desc}。'),
        ('兰州{product}的价格', '兰州{product}的价格约为{price}，{quality}。'),
        ('{market}有什么特色？', '{market}以{specialty}著称，{detail}。'),
        ('如何选购{product}？', '选购{product}要注意{tips}，{advice}。'),
    ],
    '历史': [
        ('兰州的{period}历史', '兰州在{period}时期{history}，{sig}。'),
        ('{event}发生在什么时候？', '{event}发生于{time}，{detail}。'),
        ('兰州的{figure}历史人物', '{figure}是{desc}，{contrib}。'),
        ('{building}的历史背景', '{building}建于{year}，{history}。'),
    ],
    '住宿': [
        ('{hotel}怎么样？', '{hotel}{rating}，{desc}。'),
        ('在兰州{area}住宿推荐', '在{area}可以住{hotel}，{features}。'),
        ('{hotel}的价格', '{hotel}的价格约为{price}，{includes}。'),
        ('如何预订{hotel}？', '可以通过{method}预订，{process}。'),
    ],
}

# Data pools
pools = {
    '景点': {
        'place': ['白塔山', '五泉山', '兴隆山', '吐鲁沟', '什川古梨园', '青城古镇', '河口古镇', '黄河铁桥', '水车博览园', '甘肃省博物馆', '兰州站', '中山桥', '皋兰山', '南北泉'],
        'adj': ['壮观', '秀美', '独特', '迷人', '古老', '宏伟'],
        'loc': ['黄河岸边', '城市南部', '郊外', '市中心', '周边'],
        'detail': ['是兰州的标志性景点', '吸引众多游客', '具有重要的文化价值'],
        'feature': ['其独特的地理位置', '悠久的历史', '壮观的景色'],
        'desc': ['非常值得一去', '令人印象深刻', '风景优美'],
        'activity': ['登山观景', '拍照留念', '感受自然'],
        'time': ['春季', '夏季', '秋季'],
        'tips': ['穿舒适的鞋子', '带防晒用品', '携带足够的水'],
        'price': ['免费', '10元', '15元', '20元'],
        'year': ['1909年', '1800年', '1600年'],
        'history': ['是丝绸之路的重要节点', '见证了兰州的发展', '承载了深厚的文化'],
        'status': ['国家级景点', '省级文物保护单位', '著名旅游景区'],
    },
    '美食': {
        'dish': ['牛肉面', '手抓羊肉', '灰豆子', '酿皮子', '热冬果', '烤羊肉串', '浆水面', '兰州拉面', '羊肉汤', '牛杂汤'],
        'method': ['用高汤煮面', '用特殊调料腌制', '用传统工艺制作'],
        'ingredients': ['牛肉', '面粉', '羊肉', '香料', '蔬菜'],
        'restaurant': ['老字号餐厅', '街边小店', '五星酒店'],
        'quality': ['非常正宗', '味道独特', '口碑很好'],
        'desc': ['深受当地人喜爱', '是必尝美食', '值得推荐'],
        'feature': ['色香味俱全', '营养丰富', '制作讲究'],
        'taste': ['鲜香可口', '回味无穷', '令人难忘'],
        'location': ['兰州各地', '正宁路美食街', '张掖路'],
        'rec': ['价格实惠', '环境舒适', '服务周到'],
    },
    '文化': {
        'culture': ['黄河', '丝绸之路', '民族', '传统', '现代'],
        'desc': ['源远流长', '博大精深', '独具特色'],
        'history': ['已有数千年历史', '在历代传承发展', '融合了多种文明'],
        'event': ['兰州国际马拉松', '丝绸之路文化节', '黄河文化节'],
        'definition': ['是一项重要的文化活动', '展示了兰州的文化魅力', '吸引了众多参与者'],
        'detail': ['每年举办', '规模宏大', '参与度高'],
        'tradition': ['剪纸', '书法', '绘画', '民俗'],
        'char': ['工艺精湛', '寓意深远', '代代相传'],
        'sig': ['是文化遗产的重要组成部分', '体现了民族精神'],
        'art': ['兰州剪纸', '黄河文化', '民族艺术'],
        'dev': ['源于民间', '逐步完善', '享誉国内外'],
        'status': ['仍在传承发展', '得到政府保护'],
    },
    '体验': {
        'activity': ['漂流', '登山', '骑行', '露营', '摄影', '滑翔伞', '热气球'],
        'desc': ['是热门的户外活动', '适合各年龄段', '能充分享受自然'],
        'req': ['基本的体能', '必要的装备', '提前预订'],
        'process': ['联系旅行社', '支付费用', '按时集合'],
        'price': ['100元', '200元', '300元', '500元'],
        'includes': ['包含导游', '包含餐饮', '包含保险'],
        'season': ['春季', '夏季', '秋季'],
        'reason': ['天气最好', '景色最美', '人数较少'],
    },
    '交通': {
        'from': ['兰州', '市中心', '机场'],
        'to': ['景点', '郊外', '周边城市'],
        'transport': ['公交车', '出租车', '地铁', '高铁'],
        'duration': ['30分钟', '1小时', '2小时'],
        'price': ['5元', '10元', '50元', '100元'],
        'station': ['兰州站', '兰州西站', '中川机场'],
        'location': ['市中心', '城市西部', '城市东部'],
        'access': ['交通便利', '有多条公交线路'],
        'time': ['6:00-23:00', '7:00-22:00'],
        'freq': ['每10分钟一班', '每15分钟一班'],
        'detail': ['包含空调', '舒适宽敞'],
    },
    '实用信息': {
        'service': ['医疗', '银行', '邮局', '警察', '旅游咨询'],
        'desc': ['24小时开放', '服务周到', '收费合理'],
        'action': ['就医', '取款', '寄信', '报警'],
        'req': ['身份证', '医保卡', '银行卡'],
        'tips': ['提前了解位置', '准备必要证件'],
        'facility': ['医院', '银行', '酒店', '餐厅'],
        'location': ['市中心', '各个区域'],
        'method': ['拨打电话', '网上预订'],
        'process': ['提供信息', '确认预订'],
    },
    '地理': {
        'location': ['中国西北部', '甘肃省中部', '黄河上游'],
        'coord': ['东经103°30\'', '北纬36°03\''],
        'feature': ['黄河', '山脉', '气候', '地形'],
        'desc': ['流经城市', '环绕城市', '影响气候'],
        'sig': ['是重要的地理标志', '对城市发展有重要影响'],
        'area': ['市中心', '郊外', '周边地区'],
        'char': ['地势起伏', '植被丰富', '水资源丰富'],
        'detail': ['海拔约1500米', '年降水量有限'],
        'river': ['黄河', '兰州河段'],
        'imp': ['是母亲河', '提供水资源'],
    },
    '购物': {
        'product': ['百合', '玫瑰', '瓜子', '茶叶', '工艺品', '丝绸'],
        'location': ['正宁路', '张掖路', '百货商场'],
        'desc': ['品种齐全', '价格实惠', '质量有保证'],
        'price': ['10元', '50元', '100元'],
        'quality': ['上乘', '优质', '正宗'],
        'market': ['正宁路美食街', '兰州夜市'],
        'specialty': ['美食', '工艺品', '特产'],
        'tips': ['选择正规商店', '比较价格'],
        'advice': ['咨询店员', '了解产地'],
    },
    '历史': {
        'period': ['古代', '中世纪', '近代', '现代'],
        'history': ['是丝绸之路的重要节点', '见证了多个朝代的更替', '经历了重要的历史事件'],
        'sig': ['对中国历史有重要影响', '是文化交融的见证'],
        'event': ['丝绸之路的开辟', '黄河文明的发展'],
        'time': ['2000年前', '1000年前', '100年前'],
        'figure': ['历史名人', '文化名人'],
        'desc': ['是著名的历史人物', '对兰州发展有重要贡献'],
        'contrib': ['留下了重要的文化遗产', '推动了城市发展'],
        'building': ['古建筑', '历史遗迹'],
        'year': ['明朝', '清朝', '民国'],
    },
    '住宿': {
        'hotel': ['五星酒店', '三星酒店', '青年旅舍', '民宿'],
        'rating': ['口碑很好', '评价很高', '深受欢迎'],
        'desc': ['设施完善', '服务周到', '环境舒适'],
        'area': ['市中心', '商业区', '景点附近'],
        'features': ['交通便利', '价格实惠', '环境优美'],
        'price': ['200元/晚', '500元/晚', '1000元/晚'],
        'method': ['网上预订', '电话预订'],
        'process': ['选择房型', '支付定金'],
        'includes': ['免费早餐', '免费WiFi'],
    },
}

# Generate new items
new_items = []
for idx, cat in enumerate(categories):
    items_for_cat = items_per_cat + (1 if idx < remainder else 0)
    templates = qa_templates[cat]
    pool = pools[cat]

    for i in range(items_for_cat):
        q_template, a_template = random.choice(templates)

        # Extract placeholders
        import re
        q_keys = [k for k in set(re.findall(r'\{(\w+)\}', q_template))]
        a_keys = [k for k in set(re.findall(r'\{(\w+)\}', a_template))]

        # Fill templates
        q_fills = {k: random.choice(pool.get(k, ['信息'])) for k in q_keys if k in pool}
        a_fills = {k: random.choice(pool.get(k, ['信息'])) for k in a_keys if k in pool}

        try:
            question = q_template.format(**q_fills)
            answer = a_template.format(**a_fills)

            new_items.append({
                'id': next_id,
                'question': question,
                'answer': answer,
                'category': cat
            })
            next_id += 1
        except KeyError:
            continue

kb.extend(new_items)
data['knowledge_base'] = kb

with open('src/main/resources/knowledge_base.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print(f"[OK] Expanded to {len(kb)} items")
for cat in categories:
    count = sum(1 for item in kb if item['category'] == cat)
    print(f"  {cat}: {count}")

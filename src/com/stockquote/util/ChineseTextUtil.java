package com.stockquote.util;

import com.github.houbb.opencc4j.util.ZhConverterUtil;

/** Traditional / Simplified Chinese helpers used by the symbol search. */
public final class ChineseTextUtil {

    private ChineseTextUtil() {
    }

    public static String toSimplified(String input) {
        if (input == null || input.length() == 0) {
            return input;
        }
        if (!containsCjk(input)) {
            return input;
        }
        try {
            String simple = ZhConverterUtil.toSimple(input);
            if (simple != null && simple.length() > 0) {
                return simple;
            }
        } catch (Throwable t) {
            // fall through to local map
        }
        return toSimplifiedLocal(input);
    }

    private static boolean containsCjk(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3400 && c <= 0x4DBF)) {
                return true;
            }
        }
        return false;
    }

    private static String toSimplifiedLocal(String input) {
        final String TRA =
                "匯豐鴻產工業銀國電氣機車東區門開關發龍萬與學會務廣報處場態據歷權檢濟營環總聯職華語"
                + "讀變這過還邊達遠適選鄉醫銅鐵陽際雜雲靜頁項順預頓領頭題額風飛館馬駕駛驗體鬥魚鳥麗"
                + "偉傳優價億儀僅儲兒內兩冊寫軍農決況凍準凱則創劇勞勢勝單賣廠厲壓嗎噸員問啟喪喬團"
                + "園圖堅執導層幣幫幹幾庫廳廢彈強從徹徑復恆愛慮慶憂憶應懷戀戰戲戶拋捨掃掙掛採揚換"
                + "損搖攤擇擊擴攝敗敵數斷時晉暫書條極構檔櫃樓歐歲歸殘殺殼毀氣滬濟燈燒牆獨獎獲當監盤"
                + "眾確碼禮積穩競筆簡簽籌紀約紅級組結給統絲經綠維網練縣績織續羅義習聖聞聲職聽舉舊藝"
                + "節藥蘇蘭處號蟲衛裝見規視覺訂計訊記設許訴試詩話該詳認語說調論請諸謝證護讀讓財責質"
                + "貴買貸費貿賓賞賠賣賤賬購賽贊贏趙跡轉較載輕輛輸辯遷運遞遠遲還郵鄭釋針錢錦錫錯錶鍋"
                + "鏈鏡鐘鐵鑄長閃閉閏閒間閘鬧閱闆闊騰莊廈張悅惡慘慣憤憲懇懶懸懼陳楊黃劉吳葉馮蕭蔣呂"
                + "鍾譚陸鄒顧湯臺後於隻佔餘並衝乾髮";
        final String SIM =
                "汇丰鸿产工业银国电气机车东区门开关发龙万与学会务广报处场态据历权检济营环总联职华语"
                + "读变这过还边达远适选乡医铜铁阳际杂云静页项顺预顿领头题额风飞馆马驾驶验体斗鱼鸟丽"
                + "伟传优价亿仪仅储儿内两册写军农决况冻准凯则创剧劳势胜单卖厂厉压吗吨员问启丧乔团"
                + "园图坚执导层币帮干几库厅废弹强从彻径复恒爱虑庆忧忆应怀恋战戏户抛舍扫挣挂采扬换"
                + "损摇摊择击扩摄败敌数断时晋暂书条极构档柜楼欧岁归残杀壳毁气沪济灯烧墙独奖获当监盘"
                + "众确码礼积稳竞笔简签筹纪约红级组结给统丝经绿维网练县绩织续罗义习圣闻声职听举旧艺"
                + "节药苏兰处号虫卫装见规视觉订计讯记设许诉试诗话该详认语说调论请诸谢证护读让财责质"
                + "贵买贷费贸宾赏赔卖贱账购赛赞赢赵迹转较载轻辆输辩迁运递远迟还邮郑释针钱锦锡错表锅"
                + "链镜钟铁铸长闪闭闰闲间闸闹阅板阔腾庄厦张悦恶惨惯愤宪恳懒悬惧陈杨黄刘吴叶冯萧蒋吕"
                + "钟谭陆邹顾汤台后于只占余并冲干发";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int idx = TRA.indexOf(c);
            if (idx >= 0 && idx < SIM.length()) {
                sb.append(SIM.charAt(idx));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean isMostlyCjk(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        int cjk = 0;
        int letters = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            letters++;
            if ((c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3400 && c <= 0x4DBF)) {
                cjk++;
            }
        }
        if (letters == 0) {
            return false;
        }
        return (cjk * 100 / letters) >= 50;
    }
}

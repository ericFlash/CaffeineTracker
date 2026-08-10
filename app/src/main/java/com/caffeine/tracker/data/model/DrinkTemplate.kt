package com.caffeine.tracker.data.model

data class DrinkTemplate(
    val name: String,
    val emoji: String,
    val defaultCaffeineMg: Double,
    val standardVolumeMl: Int,
    val sizes: List<DrinkSize>
)

data class DrinkSize(
    val label: String,
    val volumeMl: Int
)

object DrinkCatalog {
    val drinks: List<DrinkTemplate> = listOf(
        DrinkTemplate("美式咖啡", "☕", 225.0, 473, listOf(
            DrinkSize("小杯 (355ml)", 355),
            DrinkSize("中杯 (414ml)", 414),
            DrinkSize("大杯 (473ml)", 473),
            DrinkSize("超大杯 (591ml)", 591),
        )),
        DrinkTemplate("拿铁", "🥛", 150.0, 473, listOf(
            DrinkSize("小杯 (355ml)", 355),
            DrinkSize("中杯 (414ml)", 414),
            DrinkSize("大杯 (473ml)", 473),
            DrinkSize("超大杯 (591ml)", 591),
        )),
        DrinkTemplate("冷萃咖啡", "🧊", 250.0, 473, listOf(
            DrinkSize("中杯 (414ml)", 414),
            DrinkSize("大杯 (473ml)", 473),
            DrinkSize("超大杯 (591ml)", 591),
        )),
        DrinkTemplate("浓缩咖啡", "⚡", 63.0, 30, listOf(
            DrinkSize("单份 (30ml)", 30),
            DrinkSize("双份 (60ml)", 60),
            DrinkSize("三份 (90ml)", 90),
        )),
        DrinkTemplate("抹茶拿铁", "🍵", 80.0, 473, listOf(
            DrinkSize("中杯 (414ml)", 414),
            DrinkSize("大杯 (473ml)", 473),
        )),
        DrinkTemplate("港式奶茶", "🧋", 50.0, 250, listOf(
            DrinkSize("小杯 (250ml)", 250),
            DrinkSize("大杯 (400ml)", 400),
        )),
        DrinkTemplate("珍珠奶茶", "🧋", 100.0, 700, listOf(
            DrinkSize("中杯 (500ml)", 500),
            DrinkSize("大杯 (700ml)", 700),
            DrinkSize("超大杯 (1000ml)", 1000),
        )),
        DrinkTemplate("红茶", "🫖", 47.0, 240, listOf(
            DrinkSize("杯 (240ml)", 240),
            DrinkSize("大杯 (360ml)", 360),
        )),
        DrinkTemplate("绿茶", "🍃", 28.0, 240, listOf(
            DrinkSize("杯 (240ml)", 240),
            DrinkSize("大杯 (360ml)", 360),
        )),
        DrinkTemplate("乌龙茶", "🫖", 37.0, 240, listOf(
            DrinkSize("杯 (240ml)", 240),
            DrinkSize("大杯 (360ml)", 360),
        )),
        DrinkTemplate("普洱茶 (熟)", "🍂", 40.0, 240, listOf(
            DrinkSize("杯 (240ml)", 240),
            DrinkSize("大杯 (360ml)", 360),
        )),
        DrinkTemplate("普洱茶 (生)", "🍃", 55.0, 240, listOf(
            DrinkSize("杯 (240ml)", 240),
            DrinkSize("大杯 (360ml)", 360),
        )),
        DrinkTemplate("茉莉花茶", "🌼", 30.0, 240, listOf(
            DrinkSize("杯 (240ml)", 240),
            DrinkSize("大杯 (360ml)", 360),
        )),
        DrinkTemplate("白茶", "🍃", 28.0, 240, listOf(
            DrinkSize("杯 (240ml)", 240),
            DrinkSize("大杯 (360ml)", 360),
        )),
        DrinkTemplate("铁观音", "🫖", 35.0, 240, listOf(
            DrinkSize("杯 (240ml)", 240),
            DrinkSize("大杯 (360ml)", 360),
        )),
        DrinkTemplate("龙井茶", "🌱", 32.0, 240, listOf(
            DrinkSize("杯 (240ml)", 240),
            DrinkSize("大杯 (360ml)", 360),
        )),
        DrinkTemplate("碧螺春", "🍃", 30.0, 240, listOf(
            DrinkSize("杯 (240ml)", 240),
            DrinkSize("大杯 (360ml)", 360),
        )),
        DrinkTemplate("大红袍", "🫖", 40.0, 240, listOf(
            DrinkSize("杯 (240ml)", 240),
            DrinkSize("大杯 (360ml)", 360),
        )),
        DrinkTemplate("冷泡茶", "🧊", 40.0, 500, listOf(
            DrinkSize("瓶 (500ml)", 500),
            DrinkSize("大瓶 (750ml)", 750),
        )),
        DrinkTemplate("水果茶", "🍹", 45.0, 500, listOf(
            DrinkSize("中杯 (350ml)", 350),
            DrinkSize("大杯 (500ml)", 500),
        )),
        DrinkTemplate("可乐", "🥤", 34.0, 330, listOf(
            DrinkSize("罐 (330ml)", 330),
            DrinkSize("瓶 (500ml)", 500),
            DrinkSize("大瓶 (1000ml)", 1000),
        )),
        DrinkTemplate("零度可乐", "🥤", 34.0, 330, listOf(
            DrinkSize("罐 (330ml)", 330),
            DrinkSize("瓶 (500ml)", 500),
        )),
        DrinkTemplate("百事可乐", "🥤", 38.0, 330, listOf(
            DrinkSize("罐 (330ml)", 330),
            DrinkSize("瓶 (500ml)", 500),
        )),
        DrinkTemplate("魔爪", "👹", 160.0, 473, listOf(
            DrinkSize("罐 (473ml)", 473),
            DrinkSize("大罐 (553ml)", 553),
        )),
        DrinkTemplate("红牛", "🐂", 80.0, 250, listOf(
            DrinkSize("罐 (250ml)", 250),
            DrinkSize("大罐 (355ml)", 355),
        )),
        DrinkTemplate("巧克力饮品", "🍫", 7.0, 240, listOf(
            DrinkSize("杯 (240ml)", 240),
            DrinkSize("大杯 (360ml)", 360),
        )),
        DrinkTemplate("低因咖啡", "☕", 5.0, 240, listOf(
            DrinkSize("小杯 (240ml)", 240),
            DrinkSize("中杯 (355ml)", 355),
            DrinkSize("大杯 (473ml)", 473),
        )),
        DrinkTemplate("无咖啡因咖啡", "☕", 3.0, 240, listOf(
            DrinkSize("杯 (240ml)", 240),
            DrinkSize("大杯 (473ml)", 473),
        )),
    )
}

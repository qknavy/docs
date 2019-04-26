POI常见操作

### 1、根据word模板生成Word内容

[poi-tl官网](http://deepoove.com/poi-tl)

##### 导入依赖

```xml
<dependency>
    <groupId>com.deepoove</groupId>
    <artifactId>poi-tl</artifactId>
    <version>1.4.2</version>
</dependency>
```

##### 生成内容

```java
public class WordTemplateHelper
{
    public static void main(String[] args) throws IOException
    {
        XWPFTemplate template = XWPFTemplate.compile("d:/temp/test.docx").render(new HashMap<String, Object>(){{
            put("name", "刘元坤");
            put("datetime","2019-04-26");
        }});
        FileOutputStream out = new FileOutputStream("d:/temp/out_template.docx");
        template.write(out);
        out.flush();
        out.close();
        template.close();
    }
}
```





### 2、导出excel

##### 引入依赖

```xml
<dependency>
    <groupId>cn.afterturn</groupId>
    <artifactId>easypoi-base</artifactId>
    <version>3.1.0</version>
</dependency>
<dependency>
    <groupId>cn.afterturn</groupId>
    <artifactId>easypoi-annotation</artifactId>
    <version>3.1.0</version>
</dependency>
<dependency>
    <groupId>cn.afterturn</groupId>
    <artifactId>easypoi-web</artifactId>
    <version>3.1.0</version>
</dependency>
```

##### 代码

封装要导出的bean

```java
@Data
public class Goods implements Serializable
{
    private static final long serialVersionUID = 4008702211278193050L;

    @Excel(name = "序号", orderNum = "0")
    private int id;//ID
    @Excel(name = "名称", orderNum = "1")
    private String name;//名称
    @Excel(name = "介绍", orderNum = "2",width = 50)
    private String description;//描述
    @Excel(name = "价格", orderNum = "3")
    private float price;//价格
    @Excel(name = "库存", orderNum = "4")
    private int remain;//库存
}
```

导出：

```java
public class ExcelUtils
{
    public static void main(String[] args)
    {
        List<Goods> goodsList = new ArrayList<>();
        Goods goods = null;
        Random floatRandom = new Random();
        Random intRandom = new Random();
        for (int i = 0 ; i < 201; i++){
            goods = new Goods();
            goods.setId(i+1);
            goods.setDescription("这是商品" + (i+1)+"，质量老好了");
            goods.setName("商品"+ (i+1));
            goods.setPrice(floatRandom.nextFloat() * 1000.00F);
            goods.setRemain(intRandom.nextInt(300));
            goodsList.add(goods);
        }
        //*******************准备数据结束*********************//
        try {
            // 创建参数对象（用来设定excel得sheet得内容等信息）
            ExportParams params1 = new ExportParams() ;
            // 设置sheet得名称
            params1.setSheetName("营业收支明细"); ;
            // 创建sheet1使用得map
            Map<String,Object> dataMap1 = new HashMap<>();
            // title的参数为ExportParams类型，目前仅仅在ExportParams中设置了sheetName
            dataMap1.put("title",params1) ;
            // 模版导出对应得实体类型
            dataMap1.put("entity",Goods.class) ;
            // sheet中要填充得数据
            dataMap1.put("data",goodsList) ;

            // 将sheet1和sheet2使用得map进行包装
            List<Map<String, Object>> sheetsList = new ArrayList<>() ;
            sheetsList.add(dataMap1);

            Workbook workbook = ExcelExportUtil.exportExcel(sheetsList, ExcelType.HSSF);
            OutputStream outStream = null;
            try {
                outStream = new FileOutputStream(new File("d:/temp/商品列表.xls"));
                workbook.write(outStream);
            } finally {
                outStream.close();
                workbook.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```





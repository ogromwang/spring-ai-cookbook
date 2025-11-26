package dev.dong4j.ai.spring.prompt;

import org.junit.jupiter.api.Test;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.stringtemplate.v4.STGroupString;

import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * StringTemplate 使用示例测试，确保示例代码可运行并便于回归。
 *
 * <p>对应文档中的三种自定义分隔符方法：
 *
 * <ul>
 *   <li>方法一：直接使用 ST 构造函数（最简单）
 *   <li>方法二：使用 STGroup + ST 构造函数（推荐，Spring AI 的实现方式）
 *   <li>方法三：使用 STGroup.defineTemplate（不推荐，有坑）
 * </ul>
 */
class StringTemplateDemoTest {

    @Test
    void renderSimpleTemplate() {
        ST template = new ST("请用 <language> 一句话解释 <topic>");
        template.add("language", "中文");
        template.add("topic", "Spring AI");

        assertEquals("请用 中文 一句话解释 Spring AI", template.render());
    }

    // ========== 方法一：直接使用 ST 构造函数（最简单） ==========

    @Test
    void method1_directSTConstructor() {
        // 方法一：直接使用 ST 构造函数并指定分隔符
        ST template = new ST("请审查 {code}，并用 {language} 给出结果。", '{', '}');
        template.add("code", "public class Test {}");
        template.add("language", "中文");

        String result = template.render();
        assertEquals("请审查 public class Test {}，并用 中文 给出结果。", result);
    }

    // ========== 方法二：使用 STGroup + ST 构造函数（推荐，Spring AI 的实现方式） ==========

    @Test
    void method2_stGroupWithSTConstructor() {
        // 方法二：使用 STGroup 构造函数传入分隔符，然后创建 ST 实例
        // 这是 Spring AI 内部 StTemplateRenderer 使用的方式，最可靠
        STGroup group = new STGroup('{', '}');
        ST template = new ST(group, "请审查 {code}，并用 {language} 给出结果。");

        template.add("code", "public class Test {}");
        template.add("language", "中文");

        String result = template.render();
        assertEquals("请审查 public class Test {}，并用 中文 给出结果。", result);
    }

    // ========== 方法三：使用 STGroup.defineTemplate（不推荐，有坑） ==========

    @Test
    void method3_defineTemplateWithCustomDelimiter_fails() {
        // 方法三的错误示例：defineTemplate 在解析模板字符串时使用的是默认分隔符 < >
        // 所以即使设置了自定义分隔符，模板字符串中的 {code} 也无法被识别
        STGroup group = new STGroup();
        group.delimiterStartChar = '{';
        group.delimiterStopChar = '}';
        group.defineTemplate("audit", "请审查 {code}，并用 {language} 给出结果。");

        ST audit = group.getInstanceOf("audit");
        // 这里会抛出异常，因为 {code} 和 {language} 没有被识别为变量
        assertThrows(
            IllegalArgumentException.class,
            () -> {
                audit.add("code", "public class Test {}");
                audit.add("language", "中文");
                audit.render();
            });
    }

    // ========== 额外示例：使用 STGroupString ==========

    @Test
    void renderTemplateFromGroupString() {
        // 使用 STGroupString 定义模板
        STGroupString group =
            new STGroupString("audit(code, language) ::= <<请审查 <code>，并用 <language> 给出结果。>>");

        ST audit = group.getInstanceOf("audit");
        audit.add("code", "public class Test {}");
        audit.add("language", "中文");

        // 渲染结果使用模板定义中的分隔符 < >，所以结果是 "请审查 public class Test {}，并用 中文 给出结果。"
        assertEquals("请审查 public class Test {}，并用 中文 给出结果。", audit.render());
    }

    // ========== 从文件加载模板 ==========

    @Test
    void loadTemplateFromFile() {
        // 从 .stg 文件加载模板
        STGroupFile group = new STGroupFile("templates/prompts.stg");
        ST template = group.getInstanceOf("simplePrompt");

        template.add("language", "中文");
        template.add("topic", "StringTemplate");

        String result = template.render();
        assertEquals("请用 中文 一句话解释 StringTemplate。", result);
    }

    @Test
    void loadTemplateFromDirectory() {
        // STGroupDir 用于加载目录中的多个 .st 文件（每个模板一个文件）
        // 这里我们使用的是 .stg 文件（单文件多模板），所以应该用 STGroupFile
        // 这个测试展示如何从同一个 .stg 文件中加载多个不同的模板
        STGroupFile group = new STGroupFile("templates/prompts.stg");

        // 加载第一个模板
        ST template1 = group.getInstanceOf("simplePrompt");
        template1.add("language", "英文");
        template1.add("topic", "Spring AI");
        assertEquals("请用 英文 一句话解释 Spring AI。", template1.render());

        // 加载第二个模板，证明可以从同一个 .stg 文件加载多个模板
        ST template2 = group.getInstanceOf("multiValuePrompt");
        template2.add("tags", "Java");
        template2.add("tags", "Spring");
        assertEquals("标签：Java, Spring", template2.render());
    }

    // ========== 列表渲染 ==========

    @Test
    void renderListWithSeparator() {
        // 列表渲染，使用分隔符
        ST template = new ST("项目列表：<items; separator=\", \">");
        template.add("items", Arrays.asList("Java", "Spring", "AI"));

        String result = template.render();
        assertEquals("项目列表：Java, Spring, AI", result);
    }

    @Test
    void renderListFromTemplateFile() {
        // 从模板文件加载列表渲染模板
        STGroupFile group = new STGroupFile("templates/prompts.stg");
        ST template = group.getInstanceOf("listPrompt");

        List<String> items = Arrays.asList("项目A", "项目B", "项目C");
        template.add("items", items);

        String result = template.render();
        assertTrue(result.contains("请分析以下项目："));
        assertTrue(result.contains("项目A"));
        assertTrue(result.contains("项目B"));
        assertTrue(result.contains("项目C"));
    }

    @Test
    void renderListWithTemplate() {
        // 列表渲染，每个元素使用子模板
        ST template = new ST("标签：<tags:{tag | [<tag>]}; separator=\", \">");
        template.add("tags", Arrays.asList("Java", "Spring", "AI"));

        String result = template.render();
        assertEquals("标签：[Java], [Spring], [AI]", result);
    }

    // ========== 条件渲染 ==========

    @Test
    void renderConditionalTemplate() {
        // 条件渲染：使用 if/else 语法
        STGroupString group =
            new STGroupString(
                """
                    greeting(isVip, name) ::= <<
                    <if(isVip)>尊敬的 VIP 用户 <name>，<else>用户 <name>，<endif>欢迎！
                    >>""");

        ST template = group.getInstanceOf("greeting");
        template.add("isVip", true);
        template.add("name", "张三");

        String result = template.render().trim();
        assertEquals("尊敬的 VIP 用户 张三，欢迎！", result);
    }

    @Test
    void renderConditionalTemplate_false() {
        // 条件渲染：false 分支
        STGroupString group =
            new STGroupString(
                """
                    greeting(isVip, name) ::= <<
                    <if(isVip)>尊敬的 VIP 用户 <name>，<else>用户 <name>，<endif>欢迎！
                    >>""");

        ST template = group.getInstanceOf("greeting");
        template.add("isVip", false);
        template.add("name", "李四");

        String result = template.render().trim();
        assertEquals("用户 李四，欢迎！", result);
    }

    @Test
    void renderConditionalFromTemplateFile() {
        // 从模板文件加载条件渲染模板
        STGroupFile group = new STGroupFile("templates/prompts.stg");
        ST template = group.getInstanceOf("conditionalPrompt");

        template.add("name", "王五");
        template.add("isVip", true);

        String result = template.render();
        assertTrue(result.contains("尊敬的 VIP 用户 王五"));
        assertTrue(result.contains("欢迎使用我们的服务"));
    }

    // ========== 嵌套模板 ==========

    @Test
    void renderNestedTemplate() {
        // 嵌套模板：主模板调用子模板
        STGroupString group =
            new STGroupString(
                """
                    main(title, items) ::= <<标题：<title>
                    <itemList(items)>
                    >>
                    itemList(items) ::= <<项目列表：
                    <items:{item | - <item>}; separator=", ">
                    >>
                    """);

        ST template = group.getInstanceOf("main");
        template.add("title", "待办事项");
        template.add("items", Arrays.asList("学习 Spring AI", "编写测试", "更新文档"));

        String result = template.render();
        String normalized = result.replaceAll("\\s+", " ").trim();
        assertTrue(normalized.contains("标题：待办事项"));
        assertTrue(normalized.contains("- 学习 Spring AI"));
        assertTrue(normalized.contains("- 编写测试"));
        assertTrue(normalized.contains("- 更新文档"));
    }

    @Test
    void renderNestedFromTemplateFile() {
        // 从模板文件加载嵌套模板
        STGroupFile group = new STGroupFile("templates/prompts.stg");
        ST template = group.getInstanceOf("nestedPrompt");

        template.add("title", "功能列表");
        template.add("items", Arrays.asList("功能A", "功能B"));

        String result = template.render();
        assertTrue(result.contains("标题：功能列表"));
        assertTrue(result.contains("项目列表："));
        assertTrue(result.contains("- 功能A"));
        assertTrue(result.contains("- 功能B"));
    }

    // ========== 属性访问 ==========

    @Test
    void renderPropertyAccess() {
        // 属性访问：访问对象的属性
        // StringTemplate 通过 getter 方法访问属性，使用 Lombok 的 @Data 注解自动生成 getter
        User user = new User("张三", 25, "zhangsan@example.com");

        ST template = new ST("用户：<user.name>，年龄：<user.age>，邮箱：<user.email>");
        template.add("user", user);

        String result = template.render();
        assertEquals("用户：张三，年龄：25，邮箱：zhangsan@example.com", result);
    }

    @Test
    void renderPropertyAccessFromTemplateFile() {
        // 从模板文件加载属性访问模板
        User user = new User("李四", 30, "lisi@example.com");

        STGroupFile group = new STGroupFile("templates/prompts.stg");
        ST template = group.getInstanceOf("propertyAccessPrompt");

        template.add("user", user);

        String result = template.render();
        // 模板中包含换行符，使用 trim 或者检查包含关系时忽略空白
        String normalized = result.replaceAll("\\s+", " ").trim();
        assertTrue(normalized.contains("用户信息："));
        assertTrue(normalized.contains("姓名：李四"));
        assertTrue(normalized.contains("年龄：30"));
        assertTrue(normalized.contains("邮箱：lisi@example.com"));
    }

    // ========== 多值属性 ==========

    @Test
    void renderMultiValueAttribute() {
        // 多值属性：同一个属性添加多次会形成列表
        ST template = new ST("标签：<tags; separator=\", \">");
        template.add("tags", "Java");
        template.add("tags", "Spring");
        template.add("tags", "AI");

        String result = template.render();
        assertEquals("标签：Java, Spring, AI", result);
    }

    @Test
    void renderMultiValueFromTemplateFile() {
        // 从模板文件加载多值属性模板
        STGroupFile group = new STGroupFile("templates/prompts.stg");
        ST template = group.getInstanceOf("multiValuePrompt");

        template.add("tags", "Java");
        template.add("tags", "Spring");
        template.add("tags", "AI");

        String result = template.render();
        assertEquals("标签：Java, Spring, AI", result);
    }

    // ========== 默认值 ==========

    @Test
    void renderWithDefaultValue() {
        // 默认值：使用 if/else 实现默认值逻辑
        STGroupString group =
            new STGroupString(
                """
                    userRole(name, role) ::= <<用户 <name> 的角色是 <if(role)><role><else>普通用户<endif>。>>""");

        ST template = group.getInstanceOf("userRole");
        template.add("name", "张三");
        // 不提供 role，使用默认值

        String result = template.render();
        assertEquals("用户 张三 的角色是 普通用户。", result);
    }

    @Test
    void renderWithDefaultValue_provided() {
        // 默认值：如果属性存在，使用提供的值
        STGroupString group =
            new STGroupString(
                """
                    userRole(name, role) ::= <<用户 <name> 的角色是 <if(role)><role><else>普通用户<endif>。>>""");

        ST template = group.getInstanceOf("userRole");
        template.add("name", "张三");
        template.add("role", "管理员");

        String result = template.render();
        assertEquals("用户 张三 的角色是 管理员。", result);
    }

    @Test
    void renderDefaultValueFromTemplateFile() {
        // 从模板文件加载默认值模板
        STGroupFile group = new STGroupFile("templates/prompts.stg");
        ST template = group.getInstanceOf("defaultValuePrompt");

        template.add("name", "王五");
        // 不提供 role，使用 defaultRole 作为默认值
        template.add("defaultRole", "普通用户");
        template.add("hasRole", false);

        String result = template.render();
        assertEquals("王五 的角色是 普通用户。", result);
    }

    @Test
    void renderDefaultValueFromTemplateFile_withRole() {
        // 从模板文件加载默认值模板，提供 role
        STGroupFile group = new STGroupFile("templates/prompts.stg");
        ST template = group.getInstanceOf("defaultValuePrompt");

        template.add("name", "王五");
        template.add("role", "管理员");
        template.add("defaultRole", "普通用户");
        template.add("hasRole", true);

        String result = template.render();
        assertEquals("王五 的角色是 管理员。", result);
    }

    // ========== 转义字符 ==========

    @Test
    void renderWithEscapeCharacters() {
        // 转义字符：在模板中使用特殊字符
        ST template = new ST("价格：$<price>，折扣：<discount>%");
        template.add("price", "100");
        template.add("discount", "20");

        String result = template.render();
        assertEquals("价格：$100，折扣：20%", result);
    }

    @Test
    void renderWithSpecialCharacters() {
        // 特殊字符：包含 JSON 等特殊内容
        ST template = new ST("JSON 数据：<json>");
        template.add("json", "{\"name\": \"test\", \"value\": 123}");

        String result = template.render();
        assertEquals("JSON 数据：{\"name\": \"test\", \"value\": 123}", result);
    }

    // ========== 复杂场景：组合使用 ==========

    @Test
    void renderComplexScenario() {
        // 复杂场景：列表 + 条件 + 嵌套
        // 使用 if/else 语法处理条件渲染
        STGroupString group =
            new STGroupString(
                """
                    report(title, items, isDetailed, itemCount) ::= <<报告：<title>
                    <if(isDetailed)>
                    <itemList(items)>
                    <else>
                    共 <itemCount> 项
                    <endif>
                    >>
                    itemList(items) ::= <<详细列表：
                    <items:{item | - <item>}; separator=", ">
                    >>
                    """);

        ST template = group.getInstanceOf("report");
        template.add("title", "月度总结");
        List<String> items = Arrays.asList("任务1", "任务2", "任务3");
        template.add("items", items);
        template.add("itemCount", items.size());
        template.add("isDetailed", true);

        String result = template.render();
        String normalized = result.replaceAll("\\s+", " ").trim();
        assertTrue(normalized.contains("报告：月度总结"));
        assertTrue(normalized.contains("详细列表："));
        assertTrue(normalized.contains("- 任务1"));
    }

    @Test
    void renderComplexScenario_notDetailed() {
        // 复杂场景：非详细模式
        STGroupString group =
            new STGroupString(
                """
                    report(title, items, isDetailed, itemCount) ::= <<报告：<title>
                    <if(isDetailed)>
                    <itemList(items)>
                    <else>
                    共 <itemCount> 项
                    <endif>
                    >>
                    itemList(items) ::= <<详细列表：
                    <items:{item | - <item>}; separator=", ">
                    >>
                    """);

        ST template = group.getInstanceOf("report");
        template.add("title", "月度总结");
        List<String> items = Arrays.asList("任务1", "任务2", "任务3");
        template.add("items", items);
        template.add("itemCount", items.size());
        template.add("isDetailed", false);

        String result = template.render();
        String normalized = result.replaceAll("\\s+", " ").trim();
        assertTrue(normalized.contains("报告：月度总结"));
        assertTrue(normalized.contains("共 3 项"));
    }

    /**
     * 用户实体类，用于 StringTemplate 属性访问测试
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class User {
        private String name;
        private Integer age;
        private String email;
    }
}


